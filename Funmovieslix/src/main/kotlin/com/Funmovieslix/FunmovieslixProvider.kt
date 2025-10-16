package com.Funmovieslix

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.extractors.FileMoonIn

@CloudstreamPlugin
class FunmovieslixProvider: BasePlugin() {
    override fun load() {
        registerMainAPI(Funmovieslix())
    registerExtractorAPI(Ryderjet())
    // FilemoonV2 not available in this build environment; skip registration
    registerExtractorAPI(Dhtpre())
    registerExtractorAPI(FileMoonIn())
        registerExtractorAPI(Vidhideplus())
        registerExtractorAPI(VideyV2())
    }
}