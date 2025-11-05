package com.pusatfilm

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class PusatfilmPlugin : Plugin() {

    override fun load(context: Context) {
        registerMainAPI(Pusatfilm())
        registerExtractorAPI(Kotakajaib())
    }
}
