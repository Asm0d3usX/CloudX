version = 1

cloudstream {
    // 📝 Deskripsi plugin
    description = "GudangMovie — Movie dan Drama Subtitle Indonesia"

    // 🌐 Bahasa utama konten
    language = "id"

    // 👤 Pembuat / Kontributor
    authors = listOf("Asm0d3usX")

    /**
     * 📡 Status Plugin:
     * 0 = Down ❌
     * 1 = OK ✅
     * 2 = Slow 🐢
     * 3 = Beta Only ⚠️
     *
     * Jika tidak ditentukan, default = 3 (Beta Only)
     */
    status = 1 // ✅ Plugin aktif & berfungsi

    // 🎬 Jenis konten yang disediakan plugin
    tvTypes = listOf(
        "AsianDrama", // 🇰🇷 Drama Asia
        "TvSeries",   // 📺 Serial TV
        "Movie",      // 🎞️ Film
    )
}
