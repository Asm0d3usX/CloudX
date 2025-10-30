// 🎬 Cloudstream Plugin - Moviebox
// --------------------------------------------------
// 📦 Package utama
package com.moviebox

// 📚 Import library dan dependency yang diperlukan
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

// 🚀 Inisialisasi Plugin Utama Moviebox
@CloudstreamPlugin
class MovieboxPlugin : Plugin() {

    // ⚙️ Fungsi utama yang dijalankan saat plugin dimuat
    override fun load(context: Context) {
        // 🔗 Daftarkan API utama untuk Moviebox
        // ⚠️ Jangan edit daftar provider secara langsung — gunakan metode ini.
        registerMainAPI(Moviebox())
    }
}
