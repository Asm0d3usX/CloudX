package com.pusatfilm

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

/**
 * Plugin utama untuk Pusatfilm.
 * Mendaftarkan MainAPI dan ExtractorAPI yang digunakan oleh plugin ini.
 */
@CloudstreamPlugin
class PusatfilmPlugin : Plugin() {

    override fun load(context: Context) {
        // ✅ Daftarkan Main API
        registerMainAPI(Pusatfilm())

        // 🎬 Daftarkan Extractor API tambahan
        registerExtractorAPI(Kotakajaib())
    }
}
