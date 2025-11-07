package com.sarangfilm

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class SarangfilmPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Sarangfilm())
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
        registerExtractorAPI(Gofile())
    }
}
