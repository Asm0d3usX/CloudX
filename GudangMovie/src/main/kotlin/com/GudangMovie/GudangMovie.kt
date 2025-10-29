package com.gudangmovie

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addScore
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import java.net.URI

class GudangMovie : MainAPI() {

    // 🌐 URL Utama
    override var mainUrl = "http://152.42.204.26"
    private var directUrl: String? = null

    // 🏷️ Identitas Plugin
    override var name = "GudangMovie"
    override val hasMainPage = true
    override var lang = "id"
    override val supportedTypes = setOf(
        TvType.Movie, TvType.TvSeries, TvType.Anime, TvType.AsianDrama
    )

    // 🏠 Halaman utama
    override val mainPage = mainPageOf(
        "best-rating/page/%d/" to "⭐ Best Rating",
        "quality/hd/page/%d/" to "🎞️ HD Movie",
        "category/action/page/%d/" to "🔥 Action",
        "category/fantasy/page/%d/" to "🪄 Fantasy",
        "category/comedy/page/%d/" to "😂 Comedy",
        "country/indonesia/page/%d/" to "🇮🇩 Indonesia",
        "country/philippines/page/%d/" to "🇵🇭 Philippines",
        "country/korea/page/%d/" to "🇰🇷 Korea",
        "country/japan/page/%d/" to "🇯🇵 Japan",
        "category/english-subtitle/page/%d/" to "🌍 English Subtitle",
    )

    // 📄 Ambil data dari halaman utama
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val data = request.data.format(page)
        val document = app.get("$mainUrl/$data").document
        val home = document.select("article.item").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, home)
    }

    // 🔍 Konversi elemen HTML ke hasil pencarian
    private fun Element.toSearchResult(): SearchResponse? {
        val title = selectFirst("h2.entry-title > a")?.text()?.trim() ?: return null
        val href = fixUrl(selectFirst("a")!!.attr("href"))
        val posterUrl = fixUrlNull(selectFirst("a > img")?.getImageAttr()).fixImageQuality()
        val quality = select("div.gmr-qual, div.gmr-quality-item > a").text().trim().replace("-", "")

        return if (quality.isEmpty()) {
            // 📺 Untuk TV Series
            val episode = Regex("Episode\\s?([0-9]+)")
                .find(title)
                ?.groupValues?.getOrNull(1)
                ?.toIntOrNull()
                ?: select("div.gmr-numbeps > span").text().toIntOrNull()

            newAnimeSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
                addSub(episode)
            }
        } else {
            // 🎬 Untuk Movie
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
                addQuality(quality)
            }
        }
    }

    // 🔎 Fungsi pencarian
    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}?s=$query&post_type[]=post&post_type[]=tv", timeout = 50L).document
        return document.select("article.item").mapNotNull { it.toSearchResult() }
    }

    // 🎯 Hasil rekomendasi
    private fun Element.toRecommendResult(): SearchResponse? {
        val title = selectFirst("a > span.idmuvi-rp-title")?.text()?.trim() ?: return null
        val href = selectFirst("a")!!.attr("href")
        val posterUrl = fixUrlNull(selectFirst("a > img")?.getImageAttr().fixImageQuality())
        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    // 📥 Load detail konten
    override suspend fun load(url: String): LoadResponse {
        val fetch = app.get(url)
        directUrl = getBaseUrl(fetch.url)
        val document = fetch.document

        // 🎞️ Metadata film/series
        val title = document.selectFirst("h1.entry-title")
            ?.text()?.substringBefore("Season")?.substringBefore("Episode")?.trim().toString()

        val poster = fixUrlNull(document.selectFirst("figure.pull-left > img")?.getImageAttr())?.fixImageQuality()
        val tags = document.select("div.gmr-moviedata a").map { it.text() }
        val year = document.select("div.gmr-moviedata strong:contains(Year:) > a").text().trim().toIntOrNull()
        val description = document.selectFirst("div[itemprop=description] > p")?.text()?.trim()
        val trailer = document.selectFirst("ul.gmr-player-nav li a.gmr-trailer-popup")?.attr("href")
        val rating = document.selectFirst("div.gmr-meta-rating > span[itemprop=ratingValue]")?.text()?.trim()
        val actors = document.select("div.gmr-moviedata").last()
            ?.select("span[itemprop=actors]")?.map { it.select("a").text() }
        val duration = document.selectFirst("div.gmr-moviedata span[property=duration]")
            ?.text()?.replace(Regex("\\D"), "")?.toIntOrNull()
        val recommendations = document.select("div.idmuvi-rp ul li").mapNotNull { it.toRecommendResult() }

        val tvType = if (url.contains("/tv/")) TvType.TvSeries else TvType.Movie

        // 🎬 Load episode atau movie
        return if (tvType == TvType.TvSeries) {
            val episodes = document.select("div.vid-episodes a, div.gmr-listseries a").map { eps ->
                val href = fixUrl(eps.attr("href"))
                val name = eps.text()
                val episode = name.split(" ").lastOrNull()?.filter { it.isDigit() }?.toIntOrNull()
                val season = name.split(" ").firstOrNull()?.filter { it.isDigit() }?.toIntOrNull()
                newEpisode(href) {
                    this.name = name
                    this.episode = episode
                    this.season = if (name.contains(" ")) season else null
                }
            }.filter { it.episode != null }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                addScore(rating)
                addActors(actors)
                this.recommendations = recommendations
                this.duration = duration ?: 0
                addTrailer(trailer)
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                addScore(rating)
                addActors(actors)
                this.recommendations = recommendations
                this.duration = duration ?: 0
                addTrailer(trailer)
            }
        }
    }

    // 🔗 Ambil link streaming & download
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val id = document.selectFirst("div#muvipro_player_content_id")?.attr("data-id")

        // ▶️ Player Streaming
        if (id.isNullOrEmpty()) {
            document.select("ul.muvipro-player-tabs li a").amap { ele ->
                val iframe = app.get(fixUrl(ele.attr("href"))).document
                    .selectFirst("div.gmr-embed-responsive iframe")
                    ?.getIframeAttr()?.let { httpsify(it) } ?: return@amap

                loadExtractor(iframe, "$directUrl/", subtitleCallback, callback)
            }
        } else {
            // ⚙️ AJAX Player
            document.select("div.tab-content-ajax").amap { ele ->
                val server = app.post(
                    "$directUrl/wp-admin/admin-ajax.php",
                    data = mapOf(
                        "action" to "muvipro_player_content",
                        "tab" to ele.attr("id"),
                        "post_id" to "$id"
                    )
                ).document.select("iframe").attr("src").let { httpsify(it) }

                loadExtractor(server, "$directUrl/", subtitleCallback, callback)
            }
        }

        // 💾 Tambahkan link download
        document.select("ul.gmr-download-list li a").forEach { linkEl ->
            val downloadUrl = linkEl.attr("href")
            if (downloadUrl.isNotBlank()) {
                loadExtractor(downloadUrl, data, subtitleCallback, callback)
            }
        }

        return true
    }

    // 🖼️ Ambil atribut gambar
    private fun Element.getImageAttr(): String = when {
        hasAttr("data-src") -> attr("abs:data-src")
        hasAttr("data-lazy-src") -> attr("abs:data-lazy-src")
        hasAttr("srcset") -> attr("abs:srcset").substringBefore(" ")
        else -> attr("abs:src")
    }

    // 📺 Ambil atribut iframe
    private fun Element?.getIframeAttr(): String? =
        this?.attr("data-litespeed-src").takeIf { it?.isNotEmpty() == true } ?: this?.attr("src")

    // 🪄 Perbaiki kualitas gambar
    private fun String?.fixImageQuality(): String? {
        if (this == null) return null
        val regex = Regex("(-\\d*x\\d*)").find(this)?.groupValues?.get(0) ?: return this
        return this.replace(regex, "")
    }

    // 🔧 Ambil base URL dari alamat
    private fun getBaseUrl(url: String): String {
        return URI(url).let { "${it.scheme}://${it.host}" }
    }
}
