package com.layarmovie

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

/**
 * ============================================
 *  🎬 LayarMovie Plugin for Cloudstream3
 *  --------------------------------------------
 *  Plugin utama yang mendaftarkan semua API
 *  dan extractor dari LayarMovie.
 *
 *  Dibuat untuk menggabungkan berbagai sumber
 *  streaming seperti Earnvids, Gofile, StreamWish,
 *  dan VidStack.
 * ============================================
 */

@CloudstreamPlugin
class LayarMoviePlugin : Plugin() {

    override fun load(context: Context) {
        // 🔹 Main API utama situs
        registerMainAPI(LayarMovie())

        // 🔹 Kumpulan extractor yang digunakan plugin ini
        registerExtractorAPI(Dingtezuni())
        registerExtractorAPI(Bingezove())
        registerExtractorAPI(Mivalyo())
        registerExtractorAPI(Hglink())
        registerExtractorAPI(Ryderjet())
        registerExtractorAPI(Ghbrisk())
        registerExtractorAPI(Dhcplay())
        registerExtractorAPI(Gofile())
        registerExtractorAPI(Movearnpre())
        registerExtractorAPI(Streamcasthub())
        registerExtractorAPI(Dm21upns())
    }
}
