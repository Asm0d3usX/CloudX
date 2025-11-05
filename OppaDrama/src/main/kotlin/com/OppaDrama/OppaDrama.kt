package com.oppadrama

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addScore
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class OppaDrama : MainAPI() {
    override var mainUrl = "http://45.11.57.243"
    override var name = "OppaDrama"
    override val hasMainPage = true
    override var lang = "id"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "series/?status=&type=&order=update" to "Update Terbaru",
        "series/?country%5B%5D=japan&type=Movie&order=update" to "Film Jepang",
        "series/?country%5B%5D=thailand&status=&type=Movie&order=update" to "Film Thailand",
        "series/?country%5B%5D=united-states&status=&type=Movie&order=update" to "Film Barat",
        "series/?country%5B%5D=south-korea&status=&type=Movie&order=update" to "Film Korea",
        "series/?country%5B%5D=south-korea&status=&type=Drama&order=update" to "Series Korea",
        "series/?country%5B%5D=japan&type=Drama&order=update" to "Series Jepang",
        "series/?country%5B%5D=usa&type=Drama&order=update" to "Series Barat"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = "$mainUrl/${request.data}&page=$page"
        val document = app.get(url).document
        val items = document.select("div.listupd article.bs").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(HomePageList(request.name, items), hasNext = items.isNotEmpty())
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val linkElement = this.selectFirst("a") ?: return null
        val href = fixUrl(linkElement.attr("href"))
        val title = linkElement.attr("title").ifBlank { this.selectFirst("div.tt")?.text() } ?: return null
        val poster = this.selectFirst("img")?.getImageAttr()?.let { fixUrlNull(it) }

        val isSeries = href.contains("/series/", true) || href.contains("drama", true)

        return if (isSeries) {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = poster }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = poster }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query", timeout = 50L).document
        return document.select("div.listupd article.bs").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toRecommendResult(): SearchResponse? {
        val title = this.selectFirst("div.tt")?.text()?.trim() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.getImageAttr()?.let { fixUrlNull(it) }
        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1.entry-title")?.text()?.trim().orEmpty()
        val poster = document.selectFirst("div.bigcontent img")?.getImageAttr()?.let { fixUrlNull(it) }
        val description = document.select("div.entry-content p").joinToString("\n") { it.text() }.trim()
        val year = document.selectFirst("span:matchesOwn(Dirilis:)")?.ownText()?.filter { it.isDigit() }?.take(4)?.toIntOrNull()
        val duration = document.selectFirst("div.spe span:contains(Durasi:)")?.ownText()?.replace(Regex("\\D"), "")?.toIntOrNull()
        val tags = document.select("div.genxed a").map { it.text() }
        val actors = document.select("span:has(b:matchesOwn(Artis:)) a").map { it.text().trim() }
        val rating = document.selectFirst("div.rating strong")?.text()?.replace("Rating", "")?.trim()?.toDoubleOrNull()
        val trailer = document.selectFirst("div.bixbox.trailer iframe")?.attr("src")
        val recommendations = document.select("div.listupd article.bs").mapNotNull { it.toRecommendResult() }
        val episodes = document.select("div.eplister li a").map { ep ->
            val href = fixUrl(ep.attr("href"))
            val name = ep.selectFirst("div.epl-title")?.text() ?: "Episode"
            val episode = name.filter { it.isDigit() }.toIntOrNull()
            newEpisode(href) { this.name = name; this.episode = episode }
        }

        return if (episodes.size > 1) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.recommendations = recommendations
                this.duration = duration ?: 0
                if (rating != null) addScore(rating.toString(), 10)
                addActors(actors)
                addTrailer(trailer)
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, episodes.firstOrNull()?.data ?: url) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.recommendations = recommendations
                this.duration = duration ?: 0
                if (rating != null) addScore(rating.toString(), 10)
                addActors(actors)
                addTrailer(trailer)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        document.selectFirst("div.player-embed iframe")?.getIframeAttr()?.let { iframe ->
            if (iframe.isNotBlank()) loadExtractor(httpsify(iframe), data, subtitleCallback, callback)
        }

        val options = document.select("select.mirror option[value]:not([disabled])")
        for (option in options) {
            val base64 = option.attr("value")
            if (base64.isBlank()) continue
            try {
                val decodedHtml = base64Decode(base64)
                val iframeUrl = Jsoup.parse(decodedHtml).selectFirst("iframe")?.getIframeAttr()?.let(::httpsify)
                if (!iframeUrl.isNullOrBlank()) loadExtractor(iframeUrl, data, subtitleCallback, callback)
            } catch (e: Exception) {
                println("OppaDrama loadLinks decode error: ${e.localizedMessage}")
            }
        }

        return true
    }

    private fun Element.getImageAttr(): String = when {
        this.hasAttr("data-src") -> this.attr("abs:data-src")
        this.hasAttr("data-lazy-src") -> this.attr("abs:data-lazy-src")
        this.hasAttr("srcset") -> this.attr("abs:srcset").substringBefore(" ")
        else -> this.attr("abs:src")
    }

    private fun Element?.getIframeAttr(): String? =
        this?.attr("data-litespeed-src")?.takeIf { it.isNotEmpty() } ?: this?.attr("src")

    private fun String?.fixImageQuality(): String? {
        if (this == null) return null
        val regex = Regex("(-\\d*x\\d*)").find(this)?.groupValues?.get(0) ?: return this
        return this.replace(regex, "")
    }
}
