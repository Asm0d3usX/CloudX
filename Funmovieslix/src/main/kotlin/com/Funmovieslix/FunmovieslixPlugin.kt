package com.funmovieslix

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.extractors.FileMoonIn

@CloudstreamPlugin
class FunmovieslixPlugin: BasePlugin() {
    override fun load() {
		registerMainAPI(Funmovieslix())
		registerExtractorAPI(Ryderjet())
		registerExtractorAPI(Dhtpre())
		registerExtractorAPI(FileMoonIn())
        registerExtractorAPI(Vidhideplus())
        registerExtractorAPI(VideyV2())
    }
}