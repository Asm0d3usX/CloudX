package com.sarangfilm

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addScore
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import java.net.URI

class Sarangfilm : MainAPI() {
    override var mainUrl = "https://sarangfilm.world"
    override var name = "Sarangfilm"
    override val hasMainPage = true
    override var lang = "id"
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama
    )

    private var directUrl: String? = null

    override val mainPage = mainPageOf(
		"category/trending/page/%d/" to "Trending",
        "tv/page/%d/" to "TV Series",
		"category/action/page/%d/" to "Action",
		"category/adventure/page/%d/" to "Adventure",
		"category/comedy/page/%d/" to "Comedy",
		"category/drama/page/%d/" to "Drama",
		"category/fantasy/page/%d/" to "Fantasy",
		"category/horror/page/%d/" to "Horror",
		"category/thriller/page/%d/" to "Thriller",
		"country/indonesia/page/%d/" to "Indonesia",
		"country/thailand/page/%d/" to "Thailand",
		"country/philippines/page/%d/" to "Philippines",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/${request.data.format(page)}").document
        val items = document.select("article.item-infinite").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = selectFirst("h2.entry-title > a")?.text()?.trim() ?: return null
        val href = fixUrl(selectFirst("h2.entry-title > a")?.attr("href") ?: return null)
        val imgEl = selectFirst("div.content-thumbnail img")
		val posterRaw =
			imgEl?.attr("data-srcset")
				?.split(",")
				?.map { it.trim() }
				?.maxByOrNull { it.substringAfterLast(" ").removeSuffix("w").toIntOrNull() ?: 0 }
				?.split(" ")
				?.firstOrNull()
				?: imgEl?.attr("srcset")
					?.split(",")
					?.map { it.trim() }
					?.maxByOrNull { it.substringAfterLast(" ").removeSuffix("w").toIntOrNull() ?: 0 }
					?.split(" ")
					?.firstOrNull()
				?: imgEl?.getImageAttr()
				?: imgEl?.attr("src")

		val poster = fixUrlNull(posterRaw)
        val quality = select("div.gmr-quality-item > a, div.gmr-qual > a").text().trim().replace("-", "")
        val episodes = select("div.gmr-numbeps > span").text().toIntOrNull() ?: 0
        val postType = selectFirst("div.gmr-posttype-item")?.text()?.trim()?.lowercase() ?: "movie"
		val ratingText = this.selectFirst("div.gmr-rating-item")?.ownText()?.trim()

        return if (postType.contains("tv")) {
            newAnimeSearchResponse(title, href, TvType.TvSeries) {
                posterUrl = poster
				if (quality.isNotEmpty()){
					addQuality(quality)
				} else {
					this.score = Score.from10(ratingText?.toDoubleOrNull())
				}
                if (episodes > 0) addSub(episodes)
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                posterUrl = poster
				this.score = Score.from10(ratingText?.toDoubleOrNull())
                if (quality.isNotEmpty()) addQuality(quality)
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl?s=$query&post_type[]=post&post_type[]=tv").document
        return document.select("article.item-infinite").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toRecommendResult(): SearchResponse? {
        val title = selectFirst("h2.entry-title > a")?.text()?.trim() ?: return null
        val href = fixUrl(selectFirst("h2.entry-title > a")?.attr("href") ?: return null)
        val poster = fixUrlNull(selectFirst("div.content-thumbnail img")?.getImageAttr()?.fixImageQuality())
        val quality = select("div.gmr-quality-item > a, div.gmr-qual > a").text().trim().replace("-", "")
        val episodes = select("div.gmr-numbeps > span").text().toIntOrNull() ?: 0
        val postType = selectFirst("div.gmr-posttype-item")?.text()?.trim()?.lowercase() ?: "movie"
		val ratingText = this.selectFirst("div.gmr-rating-item")?.ownText()?.trim()

        return if (postType.contains("tv")) {
            newAnimeSearchResponse(title, href, TvType.TvSeries) {
                posterUrl = poster
				if (quality.isNotEmpty()){
					addQuality(quality)
				} else {
					this.score = Score.from10(ratingText?.toDoubleOrNull())
				}
                if (episodes > 0) addSub(episodes)
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                posterUrl = poster
				this.score = Score.from10(ratingText?.toDoubleOrNull())
                if (quality.isNotEmpty()) addQuality(quality)
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val fetch = app.get(url)
        val document = fetch.document
        directUrl = getBaseUrl(fetch.url)

        val title = document.selectFirst("h1.entry-title")?.text()
            ?.substringBefore("Season")?.substringBefore("Episode")?.trim().orEmpty()
		val imgEl = document.selectFirst("div.gmr-movie-data figure img")
		val posterRaw =
			imgEl?.attr("data-srcset")
				?.split(",")
				?.map { it.trim() }
				?.maxByOrNull { it.substringAfterLast(" ").removeSuffix("w").toIntOrNull() ?: 0 }
				?.split(" ")
				?.firstOrNull()
				?: imgEl?.attr("srcset")
					?.split(",")
					?.map { it.trim() }
					?.maxByOrNull { it.substringAfterLast(" ").removeSuffix("w").toIntOrNull() ?: 0 }
					?.split(" ")
					?.firstOrNull()
				?: imgEl?.getImageAttr()
				?: imgEl?.attr("src")

		val poster = fixUrlNull(posterRaw)
        val tags = document.select("div.gmr-moviedata a").map { it.text() }
        val year = document.select("div.gmr-moviedata strong:contains(Year:) > a")
            .text().trim().toIntOrNull()
        val tvType = if (url.contains("/tv/")) TvType.TvSeries else TvType.Movie
        val description = document.selectFirst("div[itemprop=description] > p")?.text()?.trim()

		val trailer = document.selectFirst("ul.gmr-player-nav a.gmr-trailer-popup")?.attr("href")
        val rating = document.selectFirst("div.gmr-meta-rating span[itemprop=ratingValue]")?.text()?.trim()
        val actors = document.select("div.gmr-moviedata span[itemprop=actors] a").map { it.text() }
        val duration = document.selectFirst("div.gmr-moviedata span[property=duration]")?.text()?.replace(Regex("\\D"), "")?.toIntOrNull()
        val recommendations = document.select("article.item.col-md-20").mapNotNull { it.toRecommendResult() }

        return if (tvType == TvType.TvSeries) {
            val episodes = document.select("div.vid-episodes a, div.gmr-listseries a")
			.mapNotNull { eps ->
				val href = fixUrl(eps.attr("href"))
				val name = eps.text()
				val episode = name.split(" ").lastOrNull()?.filter { it.isDigit() }?.toIntOrNull()
				val season = name.split(" ").firstOrNull()?.filter { it.isDigit() }?.toIntOrNull()
				if (episode == null) return@mapNotNull null

				newEpisode(href) {
					this.name = "Episode $episode"
					this.episode = episode
					this.season = season
					this.posterUrl = poster
				}
			}

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                posterUrl = poster
                this.year = year
                plot = description
                this.tags = tags
                addScore(rating)
                addActors(actors)
                this.recommendations = recommendations
                this.duration = duration ?: 0
                trailer?.let { addTrailer(it) }
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                posterUrl = poster
                this.year = year
                plot = description
                this.tags = tags
                addScore(rating)
                addActors(actors)
                this.recommendations = recommendations
                this.duration = duration ?: 0
                trailer?.let { addTrailer(it) }
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val id = document.selectFirst("div#muvipro_player_content_id")?.attr("data-id")

        if (!id.isNullOrEmpty()) {
            document.select("div.tab-content-ajax").forEach { ele ->
                val serverUrl = app.post(
                    "$directUrl/wp-admin/admin-ajax.php",
                    data = mapOf(
                        "action" to "muvipro_player_content",
                        "tab" to ele.attr("id"),
                        "post_id" to id
                    )
                ).document.selectFirst("iframe")?.attr("src")?.let { httpsify(it) }

                serverUrl?.let { loadExtractor(it, "$directUrl/", subtitleCallback, callback) }
            }
        } else {
            document.select("ul.muvipro-player-tabs li a").forEach { ele ->
                val iframeUrl = app.get(fixUrl(ele.attr("href"))).document
                    .selectFirst("div.gmr-embed-responsive iframe")
                    ?.attr("src")
                    ?.let { httpsify(it) }

                iframeUrl?.let { loadExtractor(it, "$directUrl/", subtitleCallback, callback) }
            }
        }

        document.select("ul.gmr-download-list li a").forEach { link ->
            val downloadUrl = link.attr("href")
            if (downloadUrl.isNotBlank()) loadExtractor(downloadUrl, data, subtitleCallback, callback)
        }

        return true
    }

    private fun Element.getImageAttr(): String = when {
        hasAttr("data-src") -> attr("abs:data-src")
        hasAttr("data-lazy-src") -> attr("abs:data-lazy-src")
        hasAttr("srcset") -> attr("abs:srcset").substringBefore(" ")
        else -> attr("abs:src")
    }

    private fun Element?.getIframeAttr(): String? =
        this?.attr("data-litespeed-src").takeIf { !it.isNullOrEmpty() } ?: this?.attr("src")

    private fun String?.fixImageQuality(): String? {
        if (this == null) return null
        val regex = Regex("(-\\d*x\\d*)").find(this)?.value ?: return this
        return replace(regex, "")
    }

    private fun getBaseUrl(url: String): String =
        URI(url).let { "${it.scheme}://${it.host}" }
}
