package com.dutamovie

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addScore
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import java.net.URI

class DutaMovie : MainAPI() {
    override var mainUrl = "https://offshorebankservices.com"
	private var directUrl: String? = null
    override var name = "Dutamovie"
    override val hasMainPage = true
    override var lang = "id"
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama
    )

    override val mainPage = mainPageOf(
        "category/box-office/page/%d/" to "Box Office",
        "category/serial-tv/page/%d/" to "TV Series",
		"action/page/%d/" to "Action",
		"adventure/page/%d/" to "Adventure",
		"animation/page/%d/" to "Animation",
		"comedy/page/%d/" to "Comedy",
		"crime/page/%d/" to "Crime",
		"drama/page/%d/" to "Drama",
		"fantasy/page/%d/" to "Fantasy",
		"horror/page/%d/" to "Horror",
		"mystery/page/%d/" to "Mystery",
		"romance/page/%d/" to "Romance",
		"science-fiction/page/%d/" to "Sci-Fi",
		"thriller/page/%d/" to "Thriller",
		"country/china/page/%d/" to "China",
		"country/indonesia/page/%d/" to "Indonesia",
		"country/korea/page/%d/" to "Korea",
		"country/philippines/page/%d/" to "Philippines",
        "country/thailand/page/%d/" to "Thailand"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/${request.data.format(page)}").document
        val items = document.select("article.item").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = selectFirst("h2.entry-title > a")?.text()?.trim() ?: return null
        val href = fixUrl(selectFirst("a")?.attr("href") ?: return null)
        val poster = fixUrlNull(selectFirst("a > img")?.getImageAttr())?.fixImageQuality()
        val quality = select("div.gmr-qual, div.gmr-quality-item > a")
            .text().trim().replace("-", "")
		val ratingText = this.selectFirst("div.gmr-rating-item")?.ownText()?.trim()

        return if (quality.isEmpty()) {
            val episode = Regex("Episode\\s?([0-9]+)").find(title)
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: select("div.gmr-numbeps > span").text().toIntOrNull()

            newAnimeSearchResponse(title, href, TvType.TvSeries) {
                posterUrl = poster
                addSub(episode)
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                posterUrl = poster
                addQuality(quality)
				this.score = Score.from10(ratingText?.toDoubleOrNull())
            }
        }
    }
	
	override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl?s=$query&post_type[]=post&post_type[]=tv").document
        return document.select("article.item-infinite").mapNotNull { it.toSearchResult() }
    }
	
	private fun Element.toRecommendResult(): SearchResponse? {
		val title = selectFirst("h2.entry-title > a")?.text()?.trim() ?: return null
		val href = selectFirst("h2.entry-title > a")?.attr("href") ?: return null
		val poster = fixUrlNull(selectFirst("div.content-thumbnail img")?.attr("src"))?.fixImageQuality()
		val quality = select("div.gmr-qual, div.gmr-quality-item > a")
            .text().trim().replace("-", "")
		val ratingText = this.selectFirst("div.gmr-rating-item")?.ownText()?.trim()
		return if (quality.isEmpty()) {
            val episode = Regex("Episode\\s?([0-9]+)").find(title)
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: select("div.gmr-numbeps > span").text().toIntOrNull()

            newAnimeSearchResponse(title, href, TvType.TvSeries) {
                posterUrl = poster
                addSub(episode)
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                posterUrl = poster
                addQuality(quality)
				this.score = Score.from10(ratingText?.toDoubleOrNull())
            }
        }
	}

    override suspend fun load(url: String): LoadResponse {
        val fetch = app.get(url)
        val document = fetch.document
        directUrl = getBaseUrl(fetch.url)

        val title = document.selectFirst("h1.entry-title")?.text()
            ?.substringBefore("Season")?.substringBefore("Episode")?.trim().orEmpty()

        val poster = fixUrlNull(document.selectFirst("figure.pull-left > img")?.getImageAttr())
            ?.fixImageQuality()
        val tags = document.select("div.gmr-moviedata a").map { it.text() }
        val year = document.select("div.gmr-moviedata strong:contains(Year:) > a")
            .text().trim().toIntOrNull()
        val tvType = if (url.contains("/tv/")) TvType.TvSeries else TvType.Movie
        val description = document.selectFirst("div[itemprop=description] > p")?.text()?.trim()
        val trailer = document.selectFirst("ul.gmr-player-nav li a.gmr-trailer-popup")?.attr("href")
        val rating = document.selectFirst("div.gmr-meta-rating span[itemprop=ratingValue]")
            ?.text()?.trim()
        val actors = document.select("div.gmr-moviedata").last()
            ?.select("span[itemprop=actors] a")?.map { it.text() }
        val duration = document.selectFirst("div.gmr-moviedata span[property=duration]")
            ?.text()?.replace(Regex("\\D"), "")?.toIntOrNull()
        val recommendations = document.select("article.item.col-md-20").mapNotNull { it.toRecommendResult() }
        return if (tvType == TvType.TvSeries) {				
			val episodes = document.select("div.vid-episodes a, div.gmr-listseries a")
				.mapNotNull { eps ->
					val href = fixUrl(eps.attr("href"))
					val rawTitle = eps.attr("title").takeIf { it.isNotBlank() } ?: eps.text()
					val cleanTitle = rawTitle.replaceFirst(Regex("(?i)Permalink to\\s*"), "").trim()

					val epNum = Regex("Episode\\s*(\\d+)").find(cleanTitle)?.groupValues?.getOrNull(1)?.toIntOrNull()
						?: cleanTitle.split(" ").lastOrNull()?.filter { it.isDigit() }?.toIntOrNull()

					val formattedName = epNum?.let { "Episode $it" } ?: cleanTitle

					newEpisode(href) {
						this.name = formattedName
						this.episode = epNum
						this.posterUrl = poster
					}
				}.filter { it.episode != null }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                posterUrl = poster
                this.year = year
                plot = description
                this.tags = tags
                addScore(rating)
                addActors(actors)
                this.recommendations = recommendations
                this.duration = duration ?: 0
                addTrailer(trailer)
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
                addTrailer(trailer)
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

        if (id.isNullOrEmpty()) {
            document.select("ul.muvipro-player-tabs li a").amap { ele ->
                val iframe = app.get(fixUrl(ele.attr("href"))).document
                    .selectFirst("div.gmr-embed-responsive iframe")
                    ?.getIframeAttr()?.let { httpsify(it) } ?: return@amap
                loadExtractor(iframe, "$directUrl/", subtitleCallback, callback)
            }
        } else {
            document.select("div.tab-content-ajax").amap { ele ->
                val server = app.post(
                    "$directUrl/wp-admin/admin-ajax.php",
                    data = mapOf(
                        "action" to "muvipro_player_content",
                        "tab" to ele.attr("id"),
                        "post_id" to "$id"
                    )
                ).document.select("iframe").attr("src").let { httpsify(it) }
                loadExtractor(server, "$directUrl/", subtitleCallback, callback)
            }
        }

        document.select("ul.gmr-download-list li a").forEach { link ->
            val downloadUrl = link.attr("href")
            if (downloadUrl.isNotBlank()) {
                loadExtractor(downloadUrl, data, subtitleCallback, callback)
            }
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
