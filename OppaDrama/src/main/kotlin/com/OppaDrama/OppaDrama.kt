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

    // 🏠 Halaman utama
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

    // 🔎 Ambil halaman utama / update terbaru
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = "$mainUrl/${request.data}&page=$page"
        val document = app.get(url).document
        val items = document.select("div.listupd article.bs").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(HomePageList(request.name, items), hasNext = items.isNotEmpty())
    }

    // 📝 Convert Element ke SearchResult
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

    // 🔍 Pencarian
    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query", timeout = 50L).document
        return document.select("div.listupd article.bs").mapNotNull { it.toSearchResult() }
    }

    // 🏆 Rekomendasi
    private fun Element.toRecommendResult(): SearchResponse? {
        val title = this.selectFirst("div.tt")?.text()?.trim() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.getImageAttr()?.let { fixUrlNull(it) }
        return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
    }

    // 🎬 Load Movie / TV Series
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        // 🏷️ Judul & Poster
        val title = document.selectFirst("h1.entry-title")?.text()?.trim().orEmpty()
        val poster = document.selectFirst("div.bigcontent img")?.getImageAttr()?.let { fixUrlNull(it) }

        // 📝 Deskripsi / Sinopsis
        val description = document.select("div.entry-content p").joinToString("\n") { it.text() }.trim()

        // 📅 Tahun rilis
        val year = document.selectFirst("span:matchesOwn(Dirilis:)")?.ownText()?.filter { it.isDigit() }?.take(4)?.toIntOrNull()

        // ℹ️ Info tambahan
        val status = document.selectFirst("span:matchesOwn(Status:)")?.ownText()?.trim()
        val duration = document.selectFirst("div.spe span:contains(Durasi:)")?.ownText()?.replace(Regex("\\D"), "")?.toIntOrNull()
        val country = document.selectFirst("span:matchesOwn(Negara:)")?.ownText()?.trim()
        val type = document.selectFirst("span:matchesOwn(Tipe:)")?.ownText()?.trim()

        // 🏷️ Genre / tags
        val tags = document.select("div.genxed a").map { it.text() }

        // 🎭 Aktor
        val actors = document.select("span:has(b:matchesOwn(Artis:)) a").map { it.text().trim() }

        // ⭐ Rating
        val rating = document.selectFirst("div.rating strong")?.text()?.replace("Rating", "")?.trim()?.toDoubleOrNull()

        // 🎥 Trailer
        val trailer = document.selectFirst("div.bixbox.trailer iframe")?.attr("src")

        // 🔄 Rekomendasi
        val recommendations = document.select("div.listupd article.bs").mapNotNull { it.toRecommendResult() }

        // 📺 Episodes (jika TV Series)
        val episodes = document.select("div.eplister li a").map { ep ->
            val href = fixUrl(ep.attr("href"))
            val name = ep.selectFirst("div.epl-title")?.text() ?: "Episode"
            val episode = name.filter { it.isDigit() }.toIntOrNull()
            newEpisode(href) { this.name = name; this.episode = episode }
        }

        return if (episodes.size > 1) {
            // 📺 TV Series
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
            // 🎬 Movie
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

    // 🌐 Load semua link video / server
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        // ===== CASE 1: iframe default =====
        document.selectFirst("div.player-embed iframe")?.getIframeAttr()?.let { iframe ->
            if (iframe.isNotBlank()) loadExtractor(httpsify(iframe), data, subtitleCallback, callback) // 🎯 Default iframe
        }

        // ===== CASE 2: semua server dari <select.mirror> =====
        val options = document.select("select.mirror option[value]:not([disabled])")
        for (option in options) {
            val base64 = option.attr("value")
            if (base64.isBlank()) continue
            try {
                // 🗝️ decode base64 → HTML → iframe
                val decodedHtml = base64Decode(base64)
                val iframeUrl = Jsoup.parse(decodedHtml).selectFirst("iframe")?.getIframeAttr()?.let(::httpsify)
                if (!iframeUrl.isNullOrBlank()) loadExtractor(iframeUrl, data, subtitleCallback, callback) // 🎯 Mirror iframe
            } catch (e: Exception) {
                println("OppaDrama loadLinks decode error: ${e.localizedMessage} ⚠️")
            }
        }

        return true
    }

    // 🖼️ Utility untuk ambil URL gambar
    private fun Element.getImageAttr(): String = when {
        this.hasAttr("data-src") -> this.attr("abs:data-src")          // 📌 Lazy loading biasa
        this.hasAttr("data-lazy-src") -> this.attr("abs:data-lazy-src") // 📌 Lazy load alternatif
        this.hasAttr("srcset") -> this.attr("abs:srcset").substringBefore(" ") // 📌 Srcset, ambil ukuran pertama
        else -> this.attr("abs:src")                                   // 📌 Fallback src langsung
    }

    // 🖥️ Utility untuk ambil URL iframe
    private fun Element?.getIframeAttr(): String? =
        this?.attr("data-litespeed-src")?.takeIf { it.isNotEmpty() }  // ⚡ Litespeed embed
            ?: this?.attr("src")                                       // 🌐 Embed standar

    // 🔧 Hapus info kualitas dari URL gambar
    private fun String?.fixImageQuality(): String? {
        if (this == null) return null
        val regex = Regex("(-\\d*x\\d*)").find(this)?.groupValues?.get(0) ?: return this
        return this.replace(regex, "") // 🧹 Bersihkan ukuran dari URL
    }
}
