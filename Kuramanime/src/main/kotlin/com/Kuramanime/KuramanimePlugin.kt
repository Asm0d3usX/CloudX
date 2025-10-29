package com.kuramanime

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

// 🔌 Plugin Utama — Kuramanime ⚡
// 🎬 Memuat semua provider & extractor terkait
@CloudstreamPlugin
class KuramanimePlugin : Plugin() {
    override fun load(context: Context) {

        // 🌐 Daftarkan Main API (Sumber utama konten)
        registerMainAPI(Kuramanime())

        // 📦 Daftarkan semua Extractor tambahan
        registerExtractorAPI(Nyomo())        // 🌀 Nyomo
        registerExtractorAPI(Streamhide())   // 🕵️ Streamhide
        registerExtractorAPI(Kuramadrive())  // ☁️ KuramaDrive
        registerExtractorAPI(Lbx())          // 🔗 Linkbox
    }
}
