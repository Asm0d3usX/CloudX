package com.animesail

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class AnimeSailPlugin : Plugin() {
    override fun load(context: Context) {

        // ⚙️ Register provider utama (Main API)
        // Semua provider harus ditambahkan di sini.
        // Jangan ubah daftar provider secara langsung agar tetap aman.
        registerMainAPI(AnimeSail())

        // ✅ Plugin AnimeSail berhasil dimuat
    }
}
