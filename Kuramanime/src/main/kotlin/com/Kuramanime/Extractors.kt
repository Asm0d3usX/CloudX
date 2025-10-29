package com.kuramanime

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.extractors.StreamSB
import com.lagradost.cloudstream3.utils.*

// 🌐 Nyomo Extractor ⚡
class Nyomo : StreamSB() {
    override var name: String = "Nyomo"
    override var mainUrl = "https://nyomo.my.id"
}

// 🎥 Streamhide Extractor 🕵️‍♂️
class Streamhide : Filesim() {
    override var name: String = "Streamhide"
    override var mainUrl: String = "https://streamhide.to"
}

// 📦 Linkbox Extractor 🔗
open class Lbx : ExtractorApi() {
    override val name = "Linkbox"
    override val mainUrl = "https://lbx.to"
    private val realUrl = "https://www.linkbox.to"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        // 🧩 Ambil token unik dari URL
        val token = Regex("""(?:/f/|/file/|\?id=)(\w+)""").find(url)?.groupValues?.get(1)

        // 🔍 Ambil ID file berdasarkan token
        val id = app.get(
            "$realUrl/api/file/share_out_list/?sortField=utime&sortAsc=0&pageNo=1&pageSize=50&shareToken=$token"
        ).parsedSafe<Responses>()?.data?.itemId

        // 📡 Ambil daftar resolusi file yang tersedia
        app.get("$realUrl/api/file/detail?itemId=$id", referer = url)
            .parsedSafe<Responses>()?.data?.itemInfo?.resolutionList?.map { link ->
                callback.invoke(
                    newExtractorLink(
                        name,
                        name,
                        link.url ?: return@map null,
                        INFER_TYPE
                    ) {
                        this.referer = "$realUrl/"
                        this.quality = getQualityFromName(link.resolution)
                    }
                )
            }
    }

    // 🧠 Data Class untuk Response JSON
    data class Resolutions(
        @JsonProperty("url") val url: String? = null,
        @JsonProperty("resolution") val resolution: String? = null,
    )

    data class ItemInfo(
        @JsonProperty("resolutionList") val resolutionList: ArrayList<Resolutions>? = arrayListOf(),
    )

    data class Data(
        @JsonProperty("itemInfo") val itemInfo: ItemInfo? = null,
        @JsonProperty("itemId") val itemId: String? = null,
    )

    data class Responses(
        @JsonProperty("data") val data: Data? = null,
    )
}

// ☁️ KuramaDrive Extractor 🚀
open class Kuramadrive : ExtractorApi() {
    override val name = "DriveKurama"
    override val mainUrl = "https://kuramadrive.com"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        // 🌍 Ambil dokumen HTML dari URL
        val req = app.get(url, referer = referer)
        val doc = req.document

        val title = doc.select("title").text()
        val token = doc.select("meta[name=csrf-token]").attr("content")
        val routeCheckAvl = doc.select("input#routeCheckAvl").attr("value")

        // 🧾 Request JSON API untuk mendapatkan URL video
        val json = app.get(
            routeCheckAvl,
            headers = mapOf(
                "X-Requested-With" to "XMLHttpRequest",
                "X-CSRF-TOKEN" to token
            ),
            referer = url,
            cookies = req.cookies
        ).parsedSafe<Source>()

        // 🎬 Kirim hasil extractor
        callback.invoke(
            newExtractorLink(
                name,
                name,
                json?.url ?: return,
                INFER_TYPE
            ) {
                this.referer = "$mainUrl/"
                this.quality = getIndexQuality(title)
            }
        )
    }

    // 🔢 Ambil kualitas video dari judul (misal: 720p, 1080p)
    private fun getIndexQuality(str: String?): Int {
        return Regex("(\\d{3,4})[pP]").find(str ?: "")?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Qualities.Unknown.value
    }

    // 📜 Data class untuk response JSON API
    private data class Source(
        @JsonProperty("url") val url: String,
    )
}
