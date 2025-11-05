package com.melongmovie

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addScore
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import java.net.URI

class Melongmovie : MainAPI() {

    override var mainUrl = "https://tv11.melongmovies.com"
    private var directUrl: String? = null
    override var name = "Melongmovie"
    override val hasMainPage = true
    override var lang = "id"
    override val supportedTypes = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "$mainUrl/latest-movies/page/%d/" to "Movie Terbaru",
        "$mainUrl/series/page/%d/" to "Series Terbaru",
        "$mainUrl/country/usa/page/%d/" to "Film Barat",
        "$mainUrl/country/south-korea/page/%d/" to "Film Korea",
        "$mainUrl/country/thailand/page/%d/" to "Film Thailand",
        "$mainUrl/country/japan/page/%d/" to "Film Japan",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = request.data.format(page)
        val document = app.get(url).document
        val items = document.select("div.los article.box")
            .mapNotNull { it.toSearchResult() }
            .filter { !it.url.contains("/series/") }

        return newHomePageResponse(HomePageList(request.name, items), hasNext = items.isNotEmpty())
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val linkElement = this.selectFirst("a") ?: return null
        val href = fixUrl(linkElement.attr("href"))
        val title = linkElement.attr("title")
            .ifBlank { this.selectFirst("h2.entry-title")?.text() } ?: return null
        val poster = this.selectFirst("img")?.getImageAttr()?.let { fixUrlNull(it) }
        val quality = this.selectFirst("span.quality")?.text()?.trim()
        val isSeries = href.contains("/series/", true) || href.contains("season", true)

        return if (isSeries) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = poster
                this.quality = getQualityFromString(quality)
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = poster
                this.quality = getQualityFromString(quality)
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query", timeout = 50L).document
        return document.select("div.los article.box")
            .mapNotNull { it.toSearchResult() }
            .filter { !it.url.contains("/series/") }
    }

    private fun Element.toRecommendResult(): SearchResponse? {
        val href = this.selectFirst("a.tip")?.attr("href") ?: return null
        val img = this.selectFirst("a.tip img")
        val title = img?.attr("alt")?.trim() ?: return null
        val posterUrl = fixUrlNull(img?.getImageAttr()?.fixImageQuality())

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1.entry-title")?.text()?.trim().orEmpty()
        val poster = fixUrlNull(doc.selectFirst("div.limage img")?.getImageAttr())
        val description = doc.selectFirst("div.bixbox > p")?.text()?.trim()
        val year = doc.selectFirst("ul.data li:has(b:contains(Release))")?.text()
            ?.filter { it.isDigit() }?.take(4)?.toIntOrNull()
        val tags = doc.select("ul.data li:has(b:contains(Genre)) a").map { it.text() }
        val actors = doc.select("ul.data li:has(b:contains(Stars)) a").map { it.text() }
        val rating = doc.selectFirst("span[itemprop=ratingValue], span.ratingValue")?.text()
        val duration = doc.selectFirst("span[property=duration]")?.text()?.replace(Regex("\\D"), "")?.toIntOrNull()
        val recommendations = doc.select("div.latest.relat article.box").mapNotNull { it.toRecommendResult() }

        return if (doc.select("div.bixbox iframe").isNotEmpty()) {
            val episodes = mutableListOf<Episode>()
            doc.select("div.bixbox").forEachIndexed { idx, box ->
                val name = box.selectFirst("div")?.text()?.trim() ?: "Episode ${idx + 1}"
                val epNumber = idx + 1
                val dataUrl = "$url#ep$epNumber"
                episodes.add(newEpisode(dataUrl) {
                    this.name = name
                    this.episode = epNumber
                })
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                addActors(actors)
                addScore(rating)
                this.recommendations = recommendations
                this.duration = duration ?: 0
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                addActors(actors)
                addScore(rating)
                this.recommendations = recommendations
                this.duration = duration ?: 0
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val parts = data.split("#")
        val url = parts[0]
        val epTag = parts.getOrNull(1)
        var found = false
        val doc = app.get(url).document

        if (epTag != null) {
            val epNum = epTag.removePrefix("ep").toIntOrNull()
            val box = doc.select("div.bixbox").getOrNull(epNum?.minus(1) ?: 0)
            val iframe = box?.selectFirst("iframe")?.attr("src")
            if (iframe != null) {
                loadExtractor(iframe, url, subtitleCallback, callback)
                found = true
            }
        } else {
            doc.select("div#embed_holder iframe").forEach { iframe ->
                loadExtractor(iframe.attr("src"), url, subtitleCallback, callback)
                found = true
            }

            doc.select("ul.mirror li a[data-href]").forEach { a ->
                val mirrorUrl = a.attr("data-href")
                val mirrorDoc = app.get(mirrorUrl).document
                val iframe = mirrorDoc.selectFirst("div#embed_holder iframe")?.attr("src")
                if (iframe != null) {
                    loadExtractor(iframe, mirrorUrl, subtitleCallback, callback)
                    found = true
                }
            }
        }

        return found
    }

    private fun Element.getImageAttr(): String {
        return when {
            this.hasAttr("data-src") -> this.attr("abs:data-src")
            this.hasAttr("data-lazy-src") -> this.attr("abs:data-lazy-src")
            this.hasAttr("srcset") -> this.attr("abs:srcset").substringBefore(" ")
            else -> this.attr("abs:src")
        }
    }

    private fun Element?.getIframeAttr(): String? {
        return this?.attr("data-litespeed-src").takeIf { it?.isNotEmpty() == true }
            ?: this?.attr("src")
    }

    private fun String?.fixImageQuality(): String? {
        if (this == null) return null
        val regex = Regex("(-\\d*x\\d*)").find(this)?.groupValues?.get(0) ?: return this
        return this.replace(regex, "")
    }

    private fun getBaseUrl(url: String): String {
        return URI(url).let { "${it.scheme}://${it.host}" }
    }
}
