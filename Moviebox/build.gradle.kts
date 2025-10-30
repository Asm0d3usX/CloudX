// 🎬 Cloudstream Plugin Config - Movibox
// --------------------------------------------------
// 🌐 Informasi dasar plugin

version = 1

cloudstream {
    // 🗣️ Bahasa konten
    language = "id"

    // 📝 Deskripsi plugin
    description = "Movibox - Movie subtitle indonesia"

    // 👨‍💻 Pembuat / pengembang plugin
    authors = listOf("Asm0d3usX")

    /**
     * 🚦 Status server:
     * 0️⃣ Down
     * 1️⃣ Ok
     * 2️⃣ Slow
     * 3️⃣ Beta only
     */
    status = 1 // ✅ Default ke 3 jika tidak ditentukan

    // 📺 Jenis konten yang didukung
    tvTypes = listOf(
        "TvSeries",
        "Movie",
        "Anime",
        "AsianDrama",
    )
}
