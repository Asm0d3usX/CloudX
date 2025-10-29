package com.kuronime

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

// 🌙 ─────────────── 🌸 KURONIME PLUGIN 🌸 ─────────────── 🌙
// 🔹 Plugin ini mendaftarkan sumber utama (MainAPI) Kuronime
// 🔹 Dibuat oleh: Asm0d3usX 💻
// 🔹 Bahasa: Indonesia 🇮🇩
// 🔹 Fokus: Anime, Movie, dan OVA 🎬
// ────────────────────────────────────────────────

@CloudstreamPlugin
class KuronimePlugin : Plugin() {
    override fun load(context: Context) {
        // ⚙️ Daftarkan provider utama (Main API)
        registerMainAPI(Kuronime())
        // 📦 Tidak ada extractor tambahan di sini
    }
}
