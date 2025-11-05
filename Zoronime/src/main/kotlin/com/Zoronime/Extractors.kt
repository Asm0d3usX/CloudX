package com.zoronime

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

open class Nanifile : ExtractorApi() {
    override val name = "Nanifile"
    override val mainUrl = "https://nanifile.com"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val res = app.get(url).document
        val source = res.selectFirst("script:containsData(servers)")
            ?.data()
            ?.substringAfter("file: \"")
            ?.substringBefore("\"")

        callback.invoke(
            newExtractorLink(
                this.name,
                this.name,
                source ?: return
            ) {
                this.referer = "$mainUrl/"
                this.quality = Qualities.Unknown.value
            }
        )
    }
}
