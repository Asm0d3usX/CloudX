package com.melongmovie

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

// 🎬 Plugin utama MelongMovie
@CloudstreamPlugin
class MelongmoviePlugin : Plugin() {

    // ⚡ Fungsi dijalankan saat plugin dimuat
    override fun load(context: Context) {
        // 🔗 Mendaftarkan API utama untuk MelongMovie
        registerMainAPI(Melongmovie())

        // 🔗 Mendaftarkan Extractor tambahan untuk streaming (Earnvids / Dingtezuni)
        registerExtractorAPI(Dingtezuni())
    }
}
