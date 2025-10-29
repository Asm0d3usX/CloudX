// 🌸 Samehadaku Plugin - Cloudstream3 Integration
package com.samehadaku

// 📦 Import library utama
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addAniListId
import com.lagradost.cloudstream3.LoadResponse.Companion.addMalId
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import kotlinx.coroutines.runBlocking
import org.jsoup.nodes.Element

// 🚀 Kelas utama untuk plugin Samehadaku
class Samehadaku : MainAPI() {
    // ⚙️ Konfigurasi dasar
    override var mainUrl = "https://v1.samehadaku.how" // 🌐 URL utama situs
    override var name = "Samehadaku"                  // 🏷️ Nama plugin
    override val hasMainPage = true                   // 🏠 Menampilkan halaman utama
    override var lang = "id"                          // 🗣️ Bahasa Indonesia
    override val hasDownloadSupport = true            // 💾 Mendukung download
    override val supportedTypes = setOf(              // 🎬 Jenis konten yang didukung
        TvType.Anime,
        TvType.AnimeMovie,
        TvType.OVA
    )

    // 💡 Utilitas tambahan
    companion object {
        // 🧭 Menentukan tipe konten berdasarkan teks
        fun getType(t: String): TvType = when {
            t.contains("OVA", true) || t.contains("Special", true) -> TvType.OVA
            t.contains("Movie", true) -> TvType.AnimeMovie
            else -> TvType.Anime
        }

        // 📆 Menentukan status anime
        fun getStatus(t: String): ShowStatus = when (t) {
            "Completed" -> ShowStatus.Completed // ✅ Tamat
            "Ongoing" -> ShowStatus.Ongoing     // 🔄 Sedang Tayang
            else -> ShowStatus.Completed
        }
    }

    // 🏠 Struktur halaman utama
    override val mainPage = mainPageOf(
        "$mainUrl/page/" to "Episode Terbaru",
        "$mainUrl/" to "HomePage",
    )

    // 📋 Ambil daftar anime dari halaman utama
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val items = mutableListOf<HomePageList>()

        // 🧩 Kategori selain "Episode Terbaru"
        if (request.name != "Episode Terbaru" && page <= 1) {
            val doc = app.get(request.data).document
            doc.select("div.widget_senction:not(:contains(Baca Komik))").forEach { block ->
                val header = block.selectFirst("div.widget-title h3")?.ownText() ?: return@forEach
                val home = block.select("div.animepost").mapNotNull { it.toSearchResult() }
                if (home.isNotEmpty()) items.add(HomePageList(header, home))
            }
        }

        // 🔔 Kategori "Episode Terbaru"
        if (request.name == "Episode Terbaru") {
            val home = app.get(request.data + page)
                .document
                .selectFirst("div.post-show")
                ?.select("ul li")
                ?.mapNotNull { it.toSearchResult() }
                ?: throw ErrorLoadingException("No Media Found")

            items.add(HomePageList(request.name, home, true))
        }

        return newHomePageResponse(items)
    }

    // 🔍 Mengubah elemen HTML jadi hasil pencarian
    private fun Element.toSearchResult(): AnimeSearchResponse? {
        val title = this.selectFirst("div.title, h2.entry-title a, div.lftinfo h2")?.text()?.trim()
            ?: return null
        val href = fixUrlNull(this.selectFirst("a")?.attr("href") ?: return null)
        val posterUrl = fixUrlNull(this.select("img").attr("src"))
        val epNum = this.selectFirst("div.dtla author")?.text()?.toIntOrNull()

        return newAnimeSearchResponse(title, href ?: return null, TvType.Anime) {
            this.posterUrl = posterUrl
            addSub(epNum)
        }
    }

    // 🔎 Fungsi pencarian anime
    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document
        return document.select("main#main div.animepost").mapNotNull { it.toSearchResult() }
    }

    // 📘 Muat detail dari halaman anime
    override suspend fun load(url: String): LoadResponse? {
        val fixUrl = if (url.contains("/anime/")) url
        else app.get(url).document.selectFirst("div.nvs.nvsc a")?.attr("href")

        val document = app.get(fixUrl ?: return null).document

        // 🏷️ Ambil metadata
        val title = document.selectFirst("h1.entry-title")?.text()?.removeBloat() ?: return null
        val poster = document.selectFirst("div.thumb > img")?.attr("src")
        val tags = document.select("div.genre-info > a").map { it.text() }
        val year = document.selectFirst("div.spe > span:contains(Rilis)")?.ownText()?.let {
            Regex("\\d,\\s(\\d*)").find(it)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }
        val status = getStatus(document.selectFirst("div.spe > span:contains(Status)")?.ownText() ?: return null)
        val type = getType(document.selectFirst("div.spe > span:contains(Type)")?.ownText()?.trim()?.lowercase() ?: "tv")
        val rating = document.selectFirst("span.ratingValue")?.text()?.trim()?.toRatingInt()
        val description = document.select("div.desc p").text().trim()
        val trailer = document.selectFirst("div.trailer-anime iframe")?.attr("src")

        // 🎞️ Daftar episode
        val episodes = document.select("div.lstepsiode.listeps ul li").mapNotNull {
            val header = it.selectFirst("span.lchx > a") ?: return@mapNotNull null
            val episode = Regex("Episode\\s?(\\d+)").find(header.text())?.groupValues?.getOrNull(1)?.toIntOrNull()
            val link = fixUrl(header.attr("href"))
            newEpisode(link) { this.episode = episode }
        }.reversed()

        // 🎯 Rekomendasi anime lainnya
        val recommendations = document.select("aside#sidebar ul li").mapNotNull { it.toSearchResult() }

        // 📊 Sinkronisasi tracker (Anilist / MAL)
        val tracker = APIHolder.getTracker(listOf(title), TrackerType.getTypes(type), year, true)

        // 📦 Kembalikan data lengkap anime
        return newAnimeLoadResponse(title, url, type) {
            engName = title
            posterUrl = tracker?.image ?: poster
            backgroundPosterUrl = tracker?.cover
            this.year = year
            addEpisodes(DubStatus.Subbed, episodes)
            showStatus = status
            this.rating = rating
            plot = description
            addTrailer(trailer)
            this.tags = tags
            this.recommendations = recommendations
            addMalId(tracker?.malId)
            addAniListId(tracker?.aniId?.toIntOrNull())
        }
    }

    // 🎬 Ambil semua link streaming dari halaman episode
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        document.select("div#downloadb li").apmap { el ->
            el.select("a").apmap {
                loadFixedExtractor(
                    fixUrl(it.attr("href")),
                    el.select("strong").text(),
                    "$mainUrl/",
                    subtitleCallback,
                    callback
                )
            }
        }
        return true
    }

    // ⚙️ Fungsi helper untuk memproses extractor link
    private suspend fun loadFixedExtractor(
        url: String,
        name: String,
        referer: String? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        loadExtractor(url, referer, subtitleCallback) { link ->
            runBlocking {
                callback.invoke(
                    newExtractorLink(link.name, link.name, link.url, link.type) {
                        this.referer = link.referer
                        this.quality = name.fixQuality()
                        this.headers = link.headers
                        this.extractorData = link.extractorData
                    }
                )
            }
        }
    }

    // 🎚️ Konversi kualitas teks ke nilai angka
    private fun String.fixQuality(): Int = when (this.uppercase()) {
        "4K" -> Qualities.P2160.value
        "FULLHD" -> Qualities.P1080.value
        "MP4HD" -> Qualities.P720.value
        else -> this.filter { it.isDigit() }.toIntOrNull() ?: Qualities.Unknown.value
    }

    // 🧹 Bersihkan judul dari kata tambahan
    private fun String.removeBloat(): String =
        this.replace(Regex("(Nonton)|(Anime)|(Subtitle\\sIndonesia)"), "").trim()
}
