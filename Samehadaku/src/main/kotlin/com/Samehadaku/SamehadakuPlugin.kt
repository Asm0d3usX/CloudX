// 🌸 Samehadaku Plugin Entry Point — Integrasi dengan Cloudstream3
package com.samehadaku

// 📦 Import dependensi utama
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

// ⚡ Menandai kelas ini sebagai plugin Cloudstream
@CloudstreamPlugin
class SamehadakuPlugin : Plugin() {

    // 🚀 Fungsi yang dijalankan saat plugin dimuat
    override fun load(context: Context) {

        // 🎬 Daftarkan sumber utama (MainAPI)
        registerMainAPI(Samehadaku())

        // ✅ Plugin aktif dan siap digunakan!
    }
}
