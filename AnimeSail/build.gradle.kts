version = 1 // 🔢 Versi plugin

cloudstream {
    language = "id" // 🇮🇩 Bahasa Indonesia

    // 🧑‍💻 Penulis plugin
    authors = listOf("Asm0d3usX")

    // 📝 Deskripsi (opsional)
    description = "Streaming Anime Subtitle Indonesia dari AnimeSail 🎬"

    /**
     * 📊 Status:
     * 0 = Down ❌
     * 1 = Ok ✅
     * 2 = Slow 🐢
     * 3 = Beta only 🧪
     */
    status = 1 // ✅ Aktif & stabil

    // 📺 Jenis konten yang didukung
    tvTypes = listOf(
        "AnimeMovie",
        "Anime",
        "OVA",
    )
}
