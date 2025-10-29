// 🌸 Zoronime Extractor - Nanifile Handler
package com.zoronime

// 📦 Import dependensi penting
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

// ⚙️ Kelas utama untuk mengekstrak video dari Nanifile
open class Nanifile : ExtractorApi() {
    // 🏷️ Nama extractor
    override val name = "Nanifile"

    // 🌐 URL utama situs
    override val mainUrl = "https://nanifile.com"

    // 🚫 Tidak butuh referer
    override val requiresReferer = false

    // 🎬 Fungsi utama untuk mendapatkan link video
    override suspend fun getUrl(
        url: String, // 🔗 URL target
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit, // 💬 Callback subtitle (jika ada)
        callback: (ExtractorLink) -> Unit          // 🔗 Callback untuk link hasil ekstraksi
    ) {
        // 📄 Ambil data HTML dari halaman
        val res = app.get(url).document

        // 🧩 Ambil sumber video dari script yang berisi data server
        val source = res.selectFirst("script:containsData(servers)")
            ?.data()
            ?.substringAfter("file: \"")
            ?.substringBefore("\"")

        // 🚀 Kirim hasil link ke callback
        callback.invoke(
            newExtractorLink(
                this.name, // Nama provider
                this.name, // Judul link
                source ?: return // ❗ Jika source null, hentikan proses
            ) {
                this.referer = "$mainUrl/"          // 🌍 Referer
                this.quality = Qualities.Unknown.value // ⚖️ Kualitas tidak diketahui
            }
        )
    }
}
