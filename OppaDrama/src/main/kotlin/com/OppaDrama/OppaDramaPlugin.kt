package com.oppadrama

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

// 🌟 Plugin OppaDrama untuk Cloudstream3
@CloudstreamPlugin
class OppaDramaPlugin : Plugin() {
    override fun load(context: Context) {
        // 🏠 Daftarkan MainAPI OppaDrama
        registerMainAPI(OppaDrama())

        // 🎬 Daftarkan Extractor Smoothpre (EarnVids)
        registerExtractorAPI(Smoothpre())
    }
}
