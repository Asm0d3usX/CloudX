package com.animasu

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class AnimasuPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Animasu())
        registerExtractorAPI(Archivd())
        registerExtractorAPI(Newuservideo())
        registerExtractorAPI(Vidhidepro())
        registerExtractorAPI(Vectorx())
    }
}
