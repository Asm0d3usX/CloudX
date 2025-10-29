version = 1

cloudstream {
    // 🌐 Bahasa utama konten
    language = "id"

    // 📝 Deskripsi plugin
    description = "Anime — Streaming Anime Sub Indo"

    // 👤 Pembuat / Kontributor
    authors = listOf("Asm0d3usX")

    /**
     * 📡 Status Plugin:
     * 0 = Down ❌
     * 1 = OK ✅
     * 2 = Slow 🐢
     * 3 = Beta Only ⚠️
     *
     * Default = 3 jika tidak diisi
     */
    status = 1 // ✅ Plugin aktif & berjalan normal

    // 🎥 Jenis konten yang disediakan plugin
    tvTypes = listOf(
        "AnimeMovie", // 🎬 Film Anime
        "OVA",        // 🧩 Original Video Animation
        "Anime",      // 📺 Seri Anime
    )
}
