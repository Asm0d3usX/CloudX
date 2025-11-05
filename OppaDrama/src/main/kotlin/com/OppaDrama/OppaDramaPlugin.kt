package com.oppadrama

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class OppaDramaPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(OppaDrama())
        registerExtractorAPI(Smoothpre())
    }
}
