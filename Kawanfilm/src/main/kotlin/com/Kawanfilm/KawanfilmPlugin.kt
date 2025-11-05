package com.kawanfilm

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class KawanfilmPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Kawanfilm())

        registerExtractorAPI(Dingtezuni())
        registerExtractorAPI(Bingezove())
        registerExtractorAPI(Mivalyo())
        registerExtractorAPI(Hglink())
        registerExtractorAPI(Ryderjet())
        registerExtractorAPI(Ghbrisk())
        registerExtractorAPI(Dhcplay())
        registerExtractorAPI(Gofile())
        registerExtractorAPI(Movearnpre())
        registerExtractorAPI(Vidshare())
    }
}
