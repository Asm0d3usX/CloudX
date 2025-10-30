version = 1

cloudstream {
    // 🌐 Informasi Dasar
    description = "🎬 Pencurimovie — Streaming Film & Serial"
    language    = "🇮🇩 id"
    authors     = listOf("👤 Asm0d3usX")

    /**
     * ⚙️ Status Server:
     * 🟥 0 = Down (tidak aktif)
     * 🟩 1 = Ok (aktif & lancar)
     * 🟨 2 = Slow (lambat)
     * 🧪 3 = Beta only (uji coba)
     */
    status = 1 // 🟩 Aktif dan stabil

    // 🎥 Jenis konten yang tersedia
    tvTypes = listOf("🎞️ Movie", "📺 TvSeries")

    // 💻 Dukungan lintas platform
    isCrossPlatform = true
}
