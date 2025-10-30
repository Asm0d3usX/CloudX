// 🌩️ Extractor API - Jeniusplay untuk Cloudstream
// --------------------------------------------------
// 📦 Package utama
package com.idlix

// 📚 Import library dan class yang dibutuhkan
import com.idlix.Idlix.ResponseSource
import com.idlix.Idlix.Tracks
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.getAndUnpack
import com.lagradost.cloudstream3.utils.newExtractorLink

// 🎬 Kelas utama untuk mengekstrak link video dari Jeniusplay
class Jeniusplay : ExtractorApi() {

    // 🔖 Identitas dan pengaturan dasar
    override var name = "Jeniusplay"
    override var mainUrl = "https://jeniusplay.com"
    override val requiresReferer = true

    // ⚙️ Fungsi utama untuk mengambil URL video dan subtitle
    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        // 🌐 Ambil dokumen HTML dari halaman
        val document = app.get(url, referer = referer).document

        // 🔑 Ambil hash unik dari URL
        val hash = url.split("/").last().substringAfter("data=")

        // 📡 Kirim permintaan POST untuk mendapatkan video source (m3u8)
        val m3uLink = app.post(
            url = "$mainUrl/player/index.php?data=$hash&do=getVideo",
            data = mapOf("hash" to hash, "r" to "$referer"),
            referer = referer,
            headers = mapOf("X-Requested-With" to "XMLHttpRequest")
        ).parsed<ResponseSource>().videoSource

        // 🔗 Kirim hasil link video ke callback
        callback.invoke(
            newExtractorLink(
                name,
                name,
                url = m3uLink,
                ExtractorLinkType.M3U8
            )
        )

        // 💬 Cek script untuk menemukan data subtitle
        document.select("script").map { script ->
            if (script.data().contains("eval(function(p,a,c,k,e,d)")) {
                val subData = getAndUnpack(script.data())
                    .substringAfter("\"tracks\":[")
                    .substringBefore("],")

                // 🗂️ Parse JSON subtitle dan kirim ke callback
                AppUtils.tryParseJson<List<Tracks>>("[$subData]")?.map { subtitle ->
                    subtitleCallback.invoke(
                        SubtitleFile(
                            getLanguage(subtitle.label ?: ""),
                            subtitle.file
                        )
                    )
                }
            }
        }
    }

    // 🗣️ Deteksi bahasa subtitle
    private fun getLanguage(str: String): String {
        return when {
            str.contains("indonesia", true) || str.contains("bahasa", true) -> "Indonesian"
            else -> str
        }
    }
}
