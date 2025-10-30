package com.pencurimovie

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

// 🎬 Plugin utama PencuriMovie
@CloudstreamPlugin
class PencurimoviePlugin: BasePlugin() {

    // ⚡ Fungsi ini dijalankan saat plugin dimuat
    override fun load() {
        // 🔗 Mendaftarkan API utama
        registerMainAPI(Pencurimovie())
    }
}
