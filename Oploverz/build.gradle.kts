version = 1

cloudstream {
    // 🌐 Bahasa utama plugin
    language = "id"

    // 📝 Deskripsi singkat plugin
    description = "Oploverz - Anime Sub Indo"

    // 👨‍💻 Pengembang plugin
    authors = listOf("Asm0d3usX")

    /**
     * 🚦 Status plugin:
     * 0️⃣ Down (tidak aktif)
     * 1️⃣ Ok (normal)
     * 2️⃣ Slow (lambat)
     * 3️⃣ Beta only (uji coba)
     */
    status = 1 // ✅ aktif & berfungsi normal

    // 🎬 Jenis konten yang didukung
    tvTypes = listOf(
        "AnimeMovie", // 🎞️ Film anime
        "Anime",      // 📺 Seri anime
        "OVA",        // 🧩 Original Video Animation
    )
}
