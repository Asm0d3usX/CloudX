package com.animasu

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.extractors.Bestx
import com.lagradost.cloudstream3.extractors.Chillx
import com.lagradost.cloudstream3.extractors.Filesim
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson

/** 🎞️ ARCHIVD EXTRACTOR **/
class Archivd : ExtractorApi() {
    override val name = "Archivd"
    override val mainUrl = "https://archivd.net"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        // 📄 Ambil halaman dan ekstrak JSON dari data-page
        val res = app.get(url).document
        val json = res.select("div#app").attr("data-page")

        // 🧩 Parse struktur JSON kompleks untuk mendapatkan link media
        val video = tryParseJson<Sources>(json)?.props?.datas?.data?.link?.media

        // 🔗 Kirim link video ke callback
        callback.invoke(
            newExtractorLink(name, name, video ?: return, INFER_TYPE) {
                this.referer = "$mainUrl/"
            }
        )
    }

    // 🧱 Struktur JSON (nested)
    data class Link(@JsonProperty("media") val media: String? = null)
    data class Data(@JsonProperty("link") val link: Link? = null)
    data class Datas(@JsonProperty("data") val data: Data? = null)
    data class Props(@JsonProperty("datas") val datas: Datas? = null)
    data class Sources(@JsonProperty("props") val props: Props? = null)
}

/** 📺 USERVIDEO EXTRACTOR **/
class Newuservideo : ExtractorApi() {
    override val name = "Uservideo"
    override val mainUrl = "https://new.uservideo.xyz"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        // 🧩 Ambil iframe utama
        val iframe = app.get(url, referer = referer).document
            .select("iframe#videoFrame").attr("src")

        // 📜 Ambil halaman iframe dan cari konfigurasi video JSON
        val doc = app.get(iframe, referer = "$mainUrl/").text
        val json = Regex("VIDEO_CONFIG\\s?=\\s?(.*)").find(doc)?.groupValues?.get(1)

        // 🎥 Parse daftar stream
        tryParseJson<Sources>(json)?.streams?.map {
            callback.invoke(
                newExtractorLink(name, name, it.playUrl ?: return@map, INFER_TYPE) {
                    this.referer = "$mainUrl/"
                    this.quality = when (it.formatId) {
                        18 -> Qualities.P360.value
                        22 -> Qualities.P720.value
                        else -> Qualities.Unknown.value
                    }
                }
            )
        }
    }

    data class Streams(
        @JsonProperty("play_url") val playUrl: String? = null,
        @JsonProperty("format_id") val formatId: Int? = null,
    )

    data class Sources(
        @JsonProperty("streams") val streams: ArrayList<Streams>? = null,
    )
}

/** 💾 VIDHIDEPRO (FileSim-based) **/
class Vidhidepro : Filesim() {
    override val name = "Vidhidepro"
    override val mainUrl = "https://vidhidepro.com"
}

/** 🌀 VECTORX (Chillx-based) **/
class Vectorx : Chillx() {
    override val name = "Vectorx"
    override val mainUrl = "https://vectorx.top"
}
