// 🌸 Zoronime Main Plugin - CloudStream3 Integration
package com.zoronime

// 📦 Import Library
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addAniListId
import com.lagradost.cloudstream3.LoadResponse.Companion.addMalId
import com.lagradost.cloudstream3.utils.*
import kotlinx.coroutines.runBlocking
import org.jsoup.nodes.Element

// 🚀 Kelas utama untuk plugin Zoronime
class Zoronime : MainAPI() {
    // ⚙️ Konfigurasi dasar
    override var mainUrl = "https://zoronime.com" // 🌐 Situs utama
    override var name = "Zoronime"                // 🏷️ Nama plugin
    override val hasMainPage = true               // 🏠 Ada halaman utama
    override var lang = "id"                      // 🗣️ Bahasa Indonesia

    // 🎬 Jenis konten yang didukung
    override val supportedTypes = setOf(
        TvType.Anime,      // 📺 Anime Series
        TvType.AnimeMovie, // 🎥 Anime Movie
        TvType.OVA         // 💿 OVA
    )

    // 💡 Utilitas tambahan
    companion object {
        // 🧭 Deteksi tipe konten berdasarkan URL
        fun getType(t: String): TvType = when {
            t.contains("/anime/", true) -> TvType.Anime
            t.contains("/movie/", true) -> TvType.AnimeMovie
            t.contains("/ova/", true) || t.contains("Special", true) -> TvType.OVA
            else -> TvType.Anime
        }

        // 📆 Status tayangan
        fun getStatus(t: String?): ShowStatus = when {
            t?.contains("On-Going", true) == true -> ShowStatus.Ongoing // 🔄 Sedang Tayang
            else -> ShowStatus.Completed // ✅ Tamat
        }
    }

    // 🏠 Halaman utama (kategori utama)
    override val mainPage = mainPageOf(
        "anime" to "Anime",
        "movie" to "Movie",
    )

    // 📄 Ambil daftar konten utama
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/${request.data}/page/$page").document
        val home = document.select("div.film_list-wrap div.flw-item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    // 🔍 Fungsi bantu untuk hasil pencarian & daftar
    private fun Element.toSearchResult(): AnimeSearchResponse? {
        val href = fixUrl(this.selectFirst("a")!!.attr("href"))
        val title = this.selectFirst("h3.film-name a")?.text() ?: return null
        val posterUrl = this.selectFirst("img.film-poster-img")?.attr("data-src")
        val episode = this.selectFirst("div.tick.rtl")?.ownText()
            ?.filter { it.isDigit() }?.toIntOrNull()

        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
            addSub(episode)
        }
    }

    // 🔎 Fungsi pencarian
    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        for (i in 1..2) {
            val res = app.get("$mainUrl/page/$i/?s=$query")
                .document
                .select("div.film_list-wrap div.flw-item")
                .mapNotNull { it.toSearchResult() }

            searchResponse.addAll(res)
        }
        return searchResponse
    }

    // 📘 Load detail anime (halaman judul)
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        // 🏷️ Ambil metadata
        val title = document.selectFirst("h2.film-name.dynamic-name, h2.film-name")?.text() ?: ""
        val poster = document.selectFirst("div#ani_detail div.film-poster img.film-poster-img")
            ?.attr("data-src")
        val tags = document.select("div.item.item-list a").map { it.text() }
        val year = document
            .select("div.item.item-title:has(span:contains(Aired)) a, div.film-stats a[href*=year]")
            .text().filter { it.isDigit() }.toIntOrNull()
        val status = getStatus(
            document.selectFirst("div.item.item-title:has(span:contains(Status)) a")
                ?.text()
                ?: document.selectFirst("div.film-stats > span:nth-child(4)")?.text()
        )
        val type = getType(url)
        val description = document.select("div.film-description.m-hide").text().trim()

        // 🎞️ Ambil daftar episode
        val episodes = if (type == TvType.AnimeMovie) {
            listOf(newEpisode(url))
        } else {
            val button = document.select("div.film-buttons a").attr("href")
            app.get(button).document.select("div#episodes-page-1 a").mapNotNull {
                val episode = it.attr("id").toIntOrNull()
                val link = it.attr("href")
                newEpisode(url = link, initializer = { this.episode = episode }, fix = false)
            }
        }

        // 💡 Rekomendasi anime lain
        val recommendations = document.select("div.film_list-wrap div.flw-item").mapNotNull {
            it.toSearchResult()
        }

        // 📊 Sinkronisasi dengan tracker (Anilist/MAL)
        val tracker = APIHolder.getTracker(listOf(title), TrackerType.getTypes(type), year, true)

        // 📦 Kembalikan data lengkap anime
        return newAnimeLoadResponse(title, url, type) {
            engName = title
            posterUrl = tracker?.image ?: poster
            backgroundPosterUrl = tracker?.cover ?: poster
            this.year = year
            addEpisodes(DubStatus.Subbed, episodes)
            showStatus = status
            plot = description
            this.tags = tags
            this.recommendations = recommendations
            addMalId(tracker?.malId)
            addAniListId(tracker?.aniId?.toIntOrNull())
        }
    }

    // 🔗 Ambil semua link streaming dari halaman episode
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        doc.select("div.item a[id*=server]").amap { server ->
            val quality = server.text().filter { it.isDigit() }.toIntOrNull()
            loadFixedExtractor(server.attr("href"), quality, null, subtitleCallback, callback)
        }
        return true
    }

    // ⚙️ Fungsi helper untuk memproses extractor
    private suspend fun loadFixedExtractor(
        url: String,
        quality: Int?,
        referer: String? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        loadExtractor(url, referer, subtitleCallback) { link ->
            runBlocking {
                callback.invoke(
                    newExtractorLink(
                        link.name, // 🔖 Nama extractor
                        link.name, // 📌 Label
                        link.url,  // 🌍 URL video
                        link.type  // 📺 Jenis link
                    ) {
                        this.referer = link.referer
                        this.quality = quality ?: Qualities.Unknown.value
                        this.headers = link.headers
                        this.extractorData = link.extractorData
                    }
                )
            }
        }
    }
}
