// 🌐 Informasi plugin Cloudstream untuk Kawanfilm
// ⚙️ Berisi metadata dasar seperti bahasa, status, dan jenis konten yang didukung.

version = 1

cloudstream {
    // 🎬 Deskripsi singkat plugin
    description = "Kawanfilm streaming movie dan series"

    // 🗣️ Bahasa utama konten
    language = "id"

    // 👨‍💻 Pengembang atau pembuat plugin
    authors = listOf("Asm0d3usX")

    /**
     * 📊 Status plugin:
     * 0 = Down ❌
     * 1 = Ok ✅
     * 2 = Slow 🐢
     * 3 = Beta only 🧪
     */
    status = 1 // ✅ Normal aktif

    // 📺 Jenis konten yang tersedia di situs ini
    tvTypes = listOf(
        "AsianDrama", // 🎎 Drama Asia
        "TvSeries",   // 📺 Serial TV
        "Movie"       // 🎥 Film
    )
}
