// 🌸 Zoronime Plugin Entry Point - CloudStream3 Integration
package com.zoronime

// 📦 Import dependensi utama
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

// ⚡ Anotasi wajib agar plugin dikenali oleh Cloudstream
@CloudstreamPlugin
class ZoronimePlugin : Plugin() {

    // 🚀 Fungsi yang dijalankan saat plugin dimuat
    override fun load(context: Context) {

        // 🧩 Daftarkan sumber utama (MainAPI)
        registerMainAPI(Zoronime())

        // 🎬 Daftarkan extractor tambahan (untuk video)
        registerExtractorAPI(Nanifile())

        // ✅ Plugin siap digunakan!
    }
}
