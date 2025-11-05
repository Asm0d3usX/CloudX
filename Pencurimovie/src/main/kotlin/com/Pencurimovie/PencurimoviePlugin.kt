package com.pencurimovie

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class PencurimoviePlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(Pencurimovie())
    }
}
