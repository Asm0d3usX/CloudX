package com.ngefilm

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class NgefilmPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Ngefilm())
        registerExtractorAPI(Dingtezuni())
        registerExtractorAPI(Bingezove())
        registerExtractorAPI(Mivalyo())
        registerExtractorAPI(Movearnpre())
        registerExtractorAPI(Dhtpre())
        registerExtractorAPI(Hglink())
        registerExtractorAPI(Gdriveplayerto())
        registerExtractorAPI(Playerngefilm21())
        registerExtractorAPI(P2pplay())
        registerExtractorAPI(Shorticu())
    }
}

