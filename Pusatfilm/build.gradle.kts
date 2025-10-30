version = 1

cloudstream {
    // Deskripsi plugin (opsional, bisa dihapus bila tidak perlu)
    description = "PusatFilm menyediakan tautan streaming untuk film dan serial TV."
    language = "id"
    authors = listOf("Asm0d3usX")

    /**
     * Status:
     * 0 = Down
     * 1 = Ok
     * 2 = Slow
     * 3 = Beta only
     */
    status = 1

    tvTypes = listOf(
        "AsianDrama",
        "TvSeries",
        "Movie"
    )
}
