// 🎬 Plugin utama untuk KawanFilm
// Mengatur pendaftaran MainAPI & ExtractorAPI agar Cloudstream dapat mengenali semua sumber video.

package com.kawanfilm

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class KawanfilmPlugin : Plugin() {
    override fun load(context: Context) {
        // 🎞️ Main provider utama situs KawanFilm
        registerMainAPI(Kawanfilm())

        // ⚙️ Daftar extractor pendukung dari berbagai sumber video eksternal
        registerExtractorAPI(Dingtezuni())   // Earnvids family
        registerExtractorAPI(Bingezove())    // Mirror domain
        registerExtractorAPI(Mivalyo())      // Alternate source
        registerExtractorAPI(Hglink())       // StreamWish source
        registerExtractorAPI(Ryderjet())     // Earnvids mirror
        registerExtractorAPI(Ghbrisk())      // StreamWish domain
        registerExtractorAPI(Dhcplay())      // StreamWish variant
        registerExtractorAPI(Gofile())       // API-based extractor
        registerExtractorAPI(Movearnpre())   // Additional domain
        registerExtractorAPI(Vidshare())     // VidStack player
    }
}
