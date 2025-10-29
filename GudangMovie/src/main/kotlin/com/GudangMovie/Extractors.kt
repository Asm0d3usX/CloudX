package com.gudangmovie

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.extractors.*
import com.lagradost.cloudstream3.utils.M3u8Helper.Companion.generateM3u8
import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI

// 🌐 ────────────────────────────────
// 🔰 Kelas Turunan Domain Earnvids
// ───────────────────────────────────
class Movearnpre : Dingtezuni() {
    override var name = "Movearnpre"
    override var mainUrl = "https://movearnpre.com"
}

class Mivalyo : Dingtezuni() {
    override var name = "Earnvids"
    override var mainUrl = "https://mivalyo.com"
}

class Ryderjet : Dingtezuni() {
    override var name = "Ryderjet"
    override var mainUrl = "https://ryderjet.com"
}

class Bingezove : Dingtezuni() {
    override var name = "Earnvids"
    override var mainUrl = "https://bingezove.com"
}

// ⚙️ ────────────────────────────────
// 🧩 Extractor Utama: Dingtezuni / Earnvids
// ───────────────────────────────────
open class Dingtezuni : ExtractorApi() {
    override val name = "Earnvids"
    override val mainUrl = "https://dingtezuni.com"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        // 🪄 Header dasar
        val headers = mapOf(
            "Sec-Fetch-Dest" to "empty",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Site" to "cross-site",
            "Origin" to mainUrl,
            "User-Agent" to USER_AGENT,
        )

        // 🌍 Ambil halaman embed
        val response = app.get(getEmbedUrl(url), referer = referer)

        // 🔐 Coba decode script video
        val script = if (!getPacked(response.text).isNullOrEmpty()) {
            var result = getAndUnpack(response.text)
            if (result.contains("var links")) result = result.substringAfter("var links")
            result
        } else {
            response.document.selectFirst("script:containsData(sources:)")?.data()
        } ?: return

        // 🎞️ Ambil semua URL M3U8 dari script
        Regex(":\\s*\"(.*?m3u8.*?)\"").findAll(script).forEach { m3u8Match ->
            generateM3u8(
                name,
                fixUrl(m3u8Match.groupValues[1]),
                referer = "$mainUrl/",
                headers = headers
            ).forEach(callback)
        }
    }

    // 🔁 Ubah URL embed ke format '/v/'
    private fun getEmbedUrl(url: String): String = when {
        url.contains("/d/") -> url.replace("/d/", "/v/")
        url.contains("/download/") -> url.replace("/download/", "/v/")
        url.contains("/file/") -> url.replace("/file/", "/v/")
        else -> url.replace("/f/", "/v/")
    }
}

// 💾 ────────────────────────────────
// 🗂️ Extractor: Gofile.io
// ───────────────────────────────────
open class Gofile : ExtractorApi() {
    override val name = "Gofile"
    override val mainUrl = "https://gofile.io"
    override val requiresReferer = false
    private val mainApi = "https://api.gofile.io"

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        // 🔑 Ambil ID file dari URL
        val id = Regex("/(?:\\?c=|d/)([\\da-zA-Z-]+)").find(url)?.groupValues?.get(1)

        // 🧾 Ambil token akun sementara
        val token = app.get("$mainApi/createAccount")
            .parsedSafe<Account>()?.data?.get("token")

        // 🔍 Ambil website token (wt)
        val websiteToken = app.get("$mainUrl/dist/js/alljs.js").text.let {
            Regex("fetchData.wt\\s*=\\s*\"([^\"]+)").find(it)?.groupValues?.get(1)
        }

        // 📦 Ambil daftar file dari API
        app.get("$mainApi/getContent?contentId=$id&token=$token&wt=$websiteToken")
            .parsedSafe<Source>()?.data?.contents?.forEach {
                callback.invoke(
                    newExtractorLink(
                        this.name,
                        this.name,
                        it.value["link"] ?: return@forEach
                    ) {
                        this.quality = getQuality(it.value["name"])
                        this.headers = mapOf("Cookie" to "accountToken=$token")
                    }
                )
            }
    }

    // 🧮 Deteksi kualitas video dari nama file
    private fun getQuality(str: String?): Int {
        return Regex("(\\d{3,4})[pP]").find(str ?: "")
            ?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Qualities.Unknown.value
    }

    // 🧱 Data models untuk parsing API
    data class Account(
        @JsonProperty("data") val data: HashMap<String, String>? = null,
    )

    data class Data(
        @JsonProperty("contents") val contents: HashMap<String, HashMap<String, String>>? = null,
    )

    data class Source(
        @JsonProperty("data") val data: Data? = null,
    )
}

// 🔗 ────────────────────────────────
// 🎥 Stream Extractors (StreamWish / VidStack)
// ───────────────────────────────────
class Hglink : StreamWishExtractor() {
    override val name = "Hglink"
    override val mainUrl = "https://hglink.to"
}

class Ghbrisk : StreamWishExtractor() {
    override val name = "Ghbrisk"
    override val mainUrl = "https://ghbrisk.com"
}

class Dhcplay : StreamWishExtractor() {
    override var name = "DHC Play"
    override var mainUrl = "https://dhcplay.com"
}

class Streamcasthub : VidStack() {
    override var name = "Streamcasthub"
    override var mainUrl = "https://live.streamcasthub.store"
    override var requiresReferer = true
}

class Dm21upns : VidStack() {
    override var name = "Dm21upns"
    override var mainUrl = "https://dm21.upns.live"
    override var requiresReferer = true
}
