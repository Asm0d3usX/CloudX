version = 1

cloudstream {
    /**
     * ============================================
     *  🎬 LayarMovie - Cloudstream Plugin Config
     *  --------------------------------------------
     *  Informasi metadata untuk plugin DutaMovie.
     *  Menentukan bahasa, deskripsi, status, dan
     *  jenis konten yang disediakan oleh plugin.
     * ============================================
     */

    description = "LayarMovie - Sumber streaming film dan drama terbaru."
    language = "id" // Bahasa Indonesia
    authors = listOf("Asm0d3usX")

    /**
     * Status Plugin:
     * 0 = Down (tidak aktif)
     * 1 = Ok (stabil dan berfungsi)
     * 2 = Slow (lambat/tidak stabil)
     * 3 = Beta only (uji coba terbatas)
     */
    status = 1

    /**
     * Jenis konten yang disediakan plugin ini.
     * Bisa disesuaikan tergantung kategori situs utama.
     */
    tvTypes = listOf(
        "AsianDrama",
        "TvSeries",
        "Movie",
    )
}
