package com.oploverz

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

/* ⚡=========================✨ Oploverz Plugin ✨=========================⚡
 * 📦 Plugin ini mendaftarkan semua komponen utama:
 *  - Provider utama: OploverzProvider (MainAPI)
 *  - Extractor tambahan: Qiwi, Filedon, Buzzheavier
 * ===============================================================⚡ */
@CloudstreamPlugin
class OploverzPlugin : Plugin() {
    override fun load(context: Context) {
        // 🚀 Register provider utama
        registerMainAPI(Oploverz())

        // 🎬 Register extractor tambahan
        registerExtractorAPI(Qiwi())
        registerExtractorAPI(Filedon())
        registerExtractorAPI(Buzzheavier())
    }
}
