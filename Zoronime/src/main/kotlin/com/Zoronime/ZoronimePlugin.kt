package com.zoronime

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class ZoronimePlugin : Plugin() {

    override fun load(context: Context) {
        registerMainAPI(Zoronime())
        registerExtractorAPI(Nanifile())
    }
}
