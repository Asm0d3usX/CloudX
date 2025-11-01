package com.wgfilm21

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class WGFilm21Plugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(WGFilm21())

        registerExtractorAPI(Dingtezuni())
        registerExtractorAPI(Movearnpre())
        registerExtractorAPI(Mivalyo())
        registerExtractorAPI(Bingezove())
        registerExtractorAPI(Ryderjet())
        registerExtractorAPI(Ghbrisk())
        registerExtractorAPI(Hglink())
        registerExtractorAPI(Dhcplay())
        registerExtractorAPI(Streamcasthub())
        registerExtractorAPI(Dm21upns())
    }
}
