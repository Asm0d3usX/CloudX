package com.gudangmovie

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class GudangMoviePlugin : Plugin() {
    override fun load(context: Context) {
        // Register main provider
        registerMainAPI(GudangMovie())

        // Register extractors
        registerExtractorAPI(Dingtezuni())
        registerExtractorAPI(Bingezove())
        registerExtractorAPI(Mivalyo())
        registerExtractorAPI(Ryderjet())
        registerExtractorAPI(Movearnpre())

        registerExtractorAPI(Hglink())
        registerExtractorAPI(Ghbrisk())
        registerExtractorAPI(Dhcplay())
        registerExtractorAPI(Streamcasthub())
        registerExtractorAPI(Dm21upns())

        registerExtractorAPI(Gofile())
    }
}
