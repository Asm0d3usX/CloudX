package com.wgfilm21

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.extractors.StreamWishExtractor
import com.lagradost.cloudstream3.extractors.VidStack
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.M3u8Helper.Companion.generateM3u8
import java.net.URI

open class Dingtezuni : ExtractorApi() {
    override val name = "Earnvids"
    override val mainUrl = "https://dingtezuni.com"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf(
            "Sec-Fetch-Dest" to "empty",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Site" to "cross-site",
            "Origin" to mainUrl,
            "User-Agent" to USER_AGENT,
        )

        val response = app.get(getEmbedUrl(url), referer = referer)
        val html = response.text

        val script = if (!getPacked(html).isNullOrEmpty()) {
            var result = getAndUnpack(html)
            if (result.contains("var links")) result = result.substringAfter("var links")
            result
        } else {
            response.document.selectFirst("script:containsData(sources:)")?.data()
        } ?: return

        Regex(":\\s*\"(.*?m3u8.*?)\"").findAll(script).forEach { match ->
            generateM3u8(
                name,
                fixUrl(match.groupValues[1]),
                referer = "$mainUrl/",
                headers = headers
            ).forEach(callback)
        }

        val allText = html + "\n" + script
        val vttRegex = Regex("""https?://[^\s"'<>\\]+?\.vtt""")
        val foundVtts = vttRegex.findAll(allText).map { it.value }.distinct().toList()

        foundVtts.forEach { vtt ->
            val lower = vtt.lowercase()
            when {
                "_ind.vtt" in lower || "ind" in lower -> subtitleCallback(SubtitleFile("Indonesian", vtt))
                "_eng.vtt" in lower || "english" in lower -> subtitleCallback(SubtitleFile("English", vtt))
                else -> subtitleCallback(SubtitleFile("Auto", vtt))
            }
        }
    }

    private fun getEmbedUrl(url: String): String = when {
        url.contains("/d/") -> url.replace("/d/", "/v/")
        url.contains("/download/") -> url.replace("/download/", "/v/")
        url.contains("/file/") -> url.replace("/file/", "/v/")
        else -> url.replace("/f/", "/v/")
    }
}

class Movearnpre : Dingtezuni() {
    override var name = "Movearnpre"
    override var mainUrl = "https://movearnpre.com"
}

class Mivalyo : Dingtezuni() {
    override var name = "Earnvids"
    override var mainUrl = "https://mivalyo.com"
}

class Ryderjet : Dingtezuni() {
    override var name = "Ryderjet"
    override var mainUrl = "https://ryderjet.com"
}

class Bingezove : Dingtezuni() {
    override var name = "Earnvids"
    override var mainUrl = "https://bingezove.com"
}

class Dinisglows : Dingtezuni() {
    override var name = "Dinisglows"
    override var mainUrl = "https://dinisglows.com"
}

class Smoothpre : Dingtezuni() {
	override var name = "Smoothpre"
	override var mainUrl = "https://smoothpre.com"
}

class Dhtpre : Dingtezuni() {
	override var name = "Dhtpre"
	override var mainUrl = "https://dhtpre.com"
}