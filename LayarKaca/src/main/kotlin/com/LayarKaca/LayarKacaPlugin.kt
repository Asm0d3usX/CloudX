package com.layarkaca

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.BasePlugin

@CloudstreamPlugin
class LayarKacaPlugin : BasePlugin() {
    override fun load() {
        registerMainAPI(LayarKaca())
        registerExtractorAPI(Furher())
        registerExtractorAPI(Furher2())
        registerExtractorAPI(Hownetwork())
        registerExtractorAPI(Cloudhownetwork())
        registerExtractorAPI(Turbovidhls())
        registerExtractorAPI(Co4nxtrl())
    }
}
