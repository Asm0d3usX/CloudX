// 🌩️ Plugin Configuration - Cloudstream Setup  
// --------------------------------------------------  
// 📦 Versi konfigurasi plugin
version = 1  

// ⚙️ Cloudstream Plugin Information
cloudstream {
    // 🌐 Bahasa utama plugin
    language = "id"

    // 📝 Deskripsi singkat plugin
    description = "Idlix Asia"

    // 👨‍💻 Pengembang / Penulis plugin
    authors = listOf("Asm0d3usX")

    /**
     * 🚦 Status plugin:
     * 0️⃣ : Down ❌
     * 1️⃣ : Ok ✅
     * 2️⃣ : Slow 🐢
     * 3️⃣ : Beta Only ⚠️
     */
    status = 1 // 💡 Default: 3 (Beta Only) jika tidak diatur

    // 🎬 Jenis konten yang didukung
    tvTypes = listOf(
        "TvSeries",   // 📺 Serial TV
        "Movie",      // 🎥 Film
        "Anime",      // 🍥 Anime
        "AsianDrama", // 🎎 Drama Asia
    )

    // 💻 Kompatibel lintas platform
    isCrossPlatform = true
}
