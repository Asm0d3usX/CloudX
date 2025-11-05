package com.moviebox

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class MovieboxPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(Moviebox())
    }
}
