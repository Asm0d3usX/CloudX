// 🌩️ Plugin Utama - Idlix Cloudstream
// --------------------------------------------------  
// 📦 Package utama
package com.idlix

// 📚 Import library yang dibutuhkan
import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

// 🚀 Inisialisasi plugin utama dengan anotasi Cloudstream
@CloudstreamPlugin
class IdlixPlugin : BasePlugin() {

    // ⚙️ Fungsi utama untuk memuat plugin
    override fun load() {
        // 🔗 Daftarkan API utama (Main API)
        registerMainAPI(Idlix())

        // 🎬 Daftarkan Extractor API tambahan
        registerExtractorAPI(Jeniusplay())
    }
}
