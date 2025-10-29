package com.animasu

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class AnimasuPlugin : Plugin() {
    override fun load(context: Context) {
        // 🌸 Daftarkan semua API di sini
        registerMainAPI(Animasu())           // 🧩 Sumber utama
        registerExtractorAPI(Archivd())      // 📂 Extractor 1
        registerExtractorAPI(Newuservideo()) // 🎥 Extractor 2
        registerExtractorAPI(Vidhidepro())   // ⚡ Extractor 3
        registerExtractorAPI(Vectorx())      // 💎 Extractor 4
    }
}
