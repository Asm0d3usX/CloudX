package com.idlix

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class IdlixPlugin: BasePlugin() {
    override fun load() {
        registerMainAPI(Idlix())
        registerExtractorAPI(Jeniusplay())
    }
}