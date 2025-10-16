// use an integer for version numbers
version = 3

cloudstream {
    // All of these properties are optional, you can safely remove them
    description = "Pusatfilm is a plugin that provides streaming links for movies and TV series."
    language = "id"
    authors = listOf("Hexated", "TeKuma25")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "AsianDrama",
        "TvSeries",
        "Movie",
    )

    iconUrl = "https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://v1.pusatfilm21info.net&size=%size%"
}