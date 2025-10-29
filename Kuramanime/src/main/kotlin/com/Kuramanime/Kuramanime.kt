package com.kuramanime

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addAniListId
import com.lagradost.cloudstream3.LoadResponse.Companion.addMalId
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

// 🌸 Main API — Kuramanime
// 🎬 Menyediakan data anime, episode, dan link streaming dari kuramanime.club
class Kuramanime : MainAPI() {

    // 🌍 URL utama
    override var mainUrl = "https://kuramanime.club"

    // 🔖 Nama plugin
    override var name = "Kuramanime"

    // ⚙️ Konfigurasi dasar
    override val hasQuickSearch = false
    override val hasMainPage = true
    override var lang = "id"
    override var sequentialMainPage = true
    override val hasDownloadSupport = true

    // 📺 Jenis konten yang didukung
    override val supportedTypes = setOf(
        TvType.Anime,
        TvType.AnimeMovie,
        TvType.OVA
    )

    companion object {
        private var cookies: Map<String, String> = mapOf()

        // 🧩 Deteksi tipe anime berdasarkan teks & jumlah episode
        fun getType(t: String, s: Int): TvType {
            return when {
                t.contains("OVA", true) || t.contains("Special") -> TvType.OVA
                t.contains("Movie", true) && s == 1 -> TvType.AnimeMovie
                else -> TvType.Anime
            }
        }

        // 🎞️ Status penayangan
        fun getStatus(t: String): ShowStatus {
            return when (t) {
                "Selesai Tayang" -> ShowStatus.Completed
                "Sedang Tayang" -> ShowStatus.Ongoing
                else -> ShowStatus.Completed
            }
        }
    }

    // 🏠 Halaman utama dengan berbagai kategori anime
    override val mainPage = mainPageOf(
        "$mainUrl/anime/ongoing?order_by=updated&page=" to "Sedang Tayang",
        "$mainUrl/anime/finished?order_by=updated&page=" to "Selesai Tayang",
        "$mainUrl/properties/season/summer-2022?order_by=most_viewed&page=" to "Dilihat Terbanyak Musim Ini",
        "$mainUrl/anime/movie?order_by=updated&page=" to "Film Layar Lebar",
    )

    // 🧭 Ambil daftar anime dari halaman utama
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("div#animeList div.product__item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    // 🧱 Bersihkan URL anime (hapus "/episode" agar direct ke halaman utama anime)
    private fun getProperAnimeLink(uri: String): String {
        return if (uri.contains("/episode"))
            Regex("(.*)/episode/.+").find(uri)?.groupValues?.get(1).toString() + "/"
        else uri
    }

    // 🔍 Konversi elemen HTML menjadi hasil pencarian anime
    private fun Element.toSearchResult(): AnimeSearchResponse? {
        val href = getProperAnimeLink(fixUrl(this.selectFirst("a")!!.attr("href")))
        val title = this.selectFirst("h5 a")?.text() ?: return null
        val posterUrl = fixUrl(this.select("div.product__item__pic.set-bg").attr("data-setbg"))
        val episode = this.select("div.ep span").text().let {
            Regex("Ep\\s(\\d+)\\s/").find(it)?.groupValues?.getOrNull(1)?.toIntOrNull()
        }

        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
            addSub(episode)
        }
    }

    // 🔎 Fungsi pencarian
    override suspend fun search(query: String): List<SearchResponse> {
        return app.get("$mainUrl/anime?search=$query&order_by=latest")
            .document.select("div#animeList div.product__item")
            .mapNotNull { it.toSearchResult() }
    }

    // 📄 Load detail anime (judul, episode, rekomendasi, dll)
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst(".anime__details__title > h3")!!.text().trim()
        val poster = document.selectFirst(".anime__details__pic")?.attr("data-setbg")
        val tags = document.select("div.anime__details__widget > div > div:nth-child(2) > ul > li:nth-child(1)")
            .text().trim().replace("Genre: ", "").split(", ")

        val year = Regex("\\D").replace(
            document.select("div.anime__details__widget > div > div:nth-child(1) > ul > li:nth-child(5)")
                .text().trim().replace("Musim: ", ""), ""
        ).toIntOrNull()

        val status = getStatus(
            document.select("div.anime__details__widget > div > div:nth-child(1) > ul > li:nth-child(3)")
                .text().trim().replace("Status: ", "")
        )
        val description = document.select(".anime__details__text > p").text().trim()

        // 🎞️ Ambil semua episode dari tiap halaman
        val episodes = mutableListOf<Episode>()
        for (i in 1..30) {
            val doc = app.get("$url?page=$i").document
            val eps = Jsoup.parse(doc.select("#episodeLists").attr("data-content"))
                .select("a.btn.btn-sm.btn-danger")
                .mapNotNull {
                    val name = it.text().trim()
                    val episode = Regex("(\\d+[.,]?\\d*)").find(name)?.groupValues?.getOrNull(0)?.toIntOrNull()
                    val link = it.attr("href")
                    newEpisode(link) { this.episode = episode }
                }
            if (eps.isEmpty()) break else episodes.addAll(eps)
        }

        val type = getType(
            document.selectFirst("div.col-lg-6.col-md-6 ul li:contains(Tipe:) a")?.text()?.lowercase() ?: "tv",
            episodes.size
        )

        // 💡 Rekomendasi anime di sidebar
        val recommendations = document.select("div#randomList > a").mapNotNull {
            val epHref = it.attr("href")
            val epTitle = it.select("h5.sidebar-title-h5.px-2.py-2").text()
            val epPoster = it.select(".product__sidebar__view__item.set-bg").attr("data-setbg")
            newAnimeSearchResponse(epTitle, epHref, TvType.Anime) {
                this.posterUrl = epPoster
                addDubStatus(dubExist = false, subExist = true)
            }
        }

        // 🔍 Tracker info (mal/anilist)
        val tracker = APIHolder.getTracker(listOf(title), TrackerType.getTypes(type), year, true)

        return newAnimeLoadResponse(title, url, type) {
            engName = title
            posterUrl = tracker?.image ?: poster
            backgroundPosterUrl = tracker?.cover
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

    // 🎥 Pemanggilan video lokal (seperti KuramaDrive / Archive)
    private suspend fun invokeLocalSource(
        url: String,
        server: String,
        headers: Map<String, String>,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val document = app.get(url, headers = headers, cookies = cookies).document

        // 🔗 Video langsung
        document.select("video#player > source").map {
            val link = fixUrl(it.attr("src"))
            val quality = it.attr("size").toIntOrNull()

            callback.invoke(
                newExtractorLink(
                    fixTitle(server),
                    fixTitle(server),
                    link,
                    INFER_TYPE
                ) {
                    this.headers = mapOf(
                        "Accept" to "video/*",
                        "Range" to "bytes=0-",
                        "Sec-Fetch-Dest" to "video",
                        "Sec-Fetch-Mode" to "no-cors",
                    )
                    this.quality = quality ?: Qualities.Unknown.value
                }
            )
        }

        // ☁️ KuramaDrive — cek juga link download
        if (server == "kuramadrive") {
            document.select("div#animeDownloadLink a").apmap {
                loadExtractor(it.attr("href"), "$mainUrl/", subtitleCallback, callback)
            }
        }
    }

    // 🧩 Load semua server video (kuramadrive, streamhide, dll)
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val req = app.get(data)
        val res = req.document
        cookies = req.cookies

        val token = res.selectFirst("meta[name=csrf-token]")?.attr("content") ?: return false
        val dataKps = res.selectFirst("div.col-lg-12.mt-3")?.attributes()?.last()?.value ?: return false

        val assets = getAssets(dataKps)

        // 🔑 Header untuk autentikasi streaming
        var headers = mapOf(
            "X-CSRF-TOKEN" to token,
            "X-Fuck-ID" to "${assets.MIX_AUTH_KEY}:${assets.MIX_AUTH_TOKEN}",
            "X-Request-ID" to randomId(),
            "X-Request-Index" to "0",
            "X-Requested-With" to "XMLHttpRequest",
        )

        val tokenKey = app.get(
            "$mainUrl/${assets.MIX_PREFIX_AUTH_ROUTE_PARAM}${assets.MIX_AUTH_ROUTE_PARAM}",
            headers = headers,
            cookies = cookies
        ).text

        headers = mapOf(
            "X-CSRF-TOKEN" to token,
            "X-Requested-With" to "XMLHttpRequest",
        )

        // 🔁 Loop semua server
        res.select("select#changeServer option").apmap { source ->
            val server = source.attr("value")
            val link = "$data?${assets.MIX_PAGE_TOKEN_KEY}=$tokenKey&${assets.MIX_STREAM_SERVER_KEY}=$server"

            if (server.contains(Regex("(?i)kuramadrive|archive"))) {
                invokeLocalSource(link, server, headers, subtitleCallback, callback)
            } else {
                app.get(link, referer = data, headers = headers, cookies = cookies)
                    .document.select("div.iframe-container iframe")
                    .attr("src").let { videoUrl ->
                        loadExtractor(fixUrl(videoUrl), "$mainUrl/", subtitleCallback, callback)
                    }
            }
        }

        return true
    }

    // 🧬 Ambil konfigurasi asset (auth token)
    private suspend fun getAssets(bpjs: String?): Assets {
        val env = app.get("$mainUrl/assets/js/$bpjs.js").text
        return Assets(
            env.substringAfter("MIX_PREFIX_AUTH_ROUTE_PARAM: '").substringBefore("',"),
            env.substringAfter("MIX_AUTH_ROUTE_PARAM: '").substringBefore("',"),
            env.substringAfter("MIX_AUTH_KEY: '").substringBefore("',"),
            env.substringAfter("MIX_AUTH_TOKEN: '").substringBefore("',"),
            env.substringAfter("MIX_PAGE_TOKEN_KEY: '").substringBefore("',"),
            env.substringAfter("MIX_STREAM_SERVER_KEY: '").substringBefore("',")
        )
    }

    // 🔢 Random ID Generator
    private fun randomId(length: Int = 6): String {
        val allowedChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..length).map { allowedChars.random() }.joinToString("")
    }

    // 📦 Data class untuk aset internal
    data class Assets(
        val MIX_PREFIX_AUTH_ROUTE_PARAM: String?,
        val MIX_AUTH_ROUTE_PARAM: String?,
        val MIX_AUTH_KEY: String?,
        val MIX_AUTH_TOKEN: String?,
        val MIX_PAGE_TOKEN_KEY: String?,
        val MIX_STREAM_SERVER_KEY: String?,
    )
}
