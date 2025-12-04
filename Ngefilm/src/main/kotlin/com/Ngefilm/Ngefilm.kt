package com.ngefilm

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addScore
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.httpsify
import com.lagradost.cloudstream3.utils.loadExtractor
import java.net.URI
import org.jsoup.nodes.Element

class Ngefilm : MainAPI() {

    override var mainUrl = "https://new28.ngefilm.site"
    private var directUrl: String? = null
    override var name = "Ngefilm"
    override val hasMainPage = true
    override var lang = "id"
    override val supportedTypes = setOf(
		TvType.Movie,
		TvType.TvSeries,
		TvType.Anime,
		TvType.AsianDrama
	)

    override val mainPage = mainPageOf(
		"year/2025/page/%d/" to "Terbaru",
		"page/%d/?s=&search=advanced&post_type=tv" to "TV Series",
		"Genre/action/page/%d/" to "Action",
		"Genre/adventure/page/%d/" to "Adventure",
		"Genre/animation/page/%d/" to "Animation",
		"Genre/fantasy/page/%d/" to "Fantasy",
		"country/japan/page/%d/" to "Japan",
		"country/indonesia/page/%d/" to "Indonesia",
		"country/philippines/page/%d/" to "Philippines"
    )
	
	override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/${request.data.format(page)}").document
        val items = document.select("article.item-infinite").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, items)
    }

	private fun Element.toSearchResult(): SearchResponse? {
		val title = this.selectFirst("h2.entry-title > a")?.text()?.trim() ?: return null
		val href = fixUrl(this.selectFirst("a")!!.attr("href"))
		val posterUrl = fixUrlNull(this.selectFirst("a > img")?.getImageAttr()).fixImageQuality()
		val quality = this.select("div.gmr-qual, div.gmr-quality-item > a").text().trim().replace("-", "")
		val ratingText = this.selectFirst("div.gmr-rating-item")?.ownText()?.trim()
		val eps = selectFirst(".gmr-numbeps span")?.text()?.trim()?.toIntOrNull()
		val isSeries = eps != null

		return if (isSeries) {
			newAnimeSearchResponse(title, href, TvType.TvSeries) {
				this.posterUrl = posterUrl
				if (eps !=null){
					addSub(eps)
				} else {
					this.score = Score.from10(ratingText?.toDoubleOrNull())
				}
			}
		} else {
			newMovieSearchResponse(title, href, TvType.Movie) {
				this.posterUrl = posterUrl
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
        val title = this.selectFirst("h2.entry-title > a")?.text()?.trim() ?: return null
		val href = fixUrl(this.selectFirst("a")!!.attr("href"))
		val posterUrl = fixUrlNull(this.selectFirst("a > img")?.getImageAttr()).fixImageQuality()
		val quality = this.select("div.gmr-qual, div.gmr-quality-item > a").text().trim().replace("-", "")
		val ratingText = this.selectFirst("div.gmr-rating-item")?.ownText()?.trim()
		val eps = selectFirst(".gmr-numbeps span")?.text()?.trim()?.toIntOrNull()
		val isSeries = eps != null

		return if (isSeries) {
			newAnimeSearchResponse(title, href, TvType.TvSeries) {
				this.posterUrl = posterUrl
				if (eps !=null){
					addSub(eps)
				} else {
					this.score = Score.from10(ratingText?.toDoubleOrNull())
				}
			}
		} else {
			newMovieSearchResponse(title, href, TvType.Movie) {
				this.posterUrl = posterUrl
				addQuality(quality)
				this.score = Score.from10(ratingText?.toDoubleOrNull())
			}
		}
    }

    override suspend fun load(url: String): LoadResponse {
		val fetch = app.get(url)
		directUrl = getBaseUrl(fetch.url)
		val document = fetch.document

		val title = document.selectFirst("h1.entry-title")?.text()?.substringBefore("Season")
			?.substringBefore("Episode")?.trim().toString()
		val poster = fixUrlNull(document.selectFirst("figure.pull-left > img")?.getImageAttr())
			?.fixImageQuality()
		val tags = document.select("div.gmr-moviedata a").map { it.text() }
		val year = document.select("div.gmr-moviedata strong:contains(Year:) > a").text()
			.trim().toIntOrNull()
		val tvType = if (url.contains("/tv/")) TvType.TvSeries else TvType.Movie
		val description = document.selectFirst("div[itemprop=description] > p")?.text()?.trim()
		val trailer = document.selectFirst("ul.gmr-player-nav li a.gmr-trailer-popup")?.attr("href")
		val rating = document.selectFirst("div.gmr-meta-rating > span[itemprop=ratingValue]")
			?.text()?.trim()
		val actors = document.select("div.gmr-moviedata").last()?.select("span[itemprop=actors]")
			?.map { it.select("a").text() }
		val duration = document.selectFirst("div.gmr-moviedata span[property=duration]")?.text()
			?.replace(Regex("\\D"), "")?.toIntOrNull()
		val recommendations = document.select("article.item.col-md-20").mapNotNull { it.toRecommendResult() }

		return if (tvType == TvType.TvSeries) {
			val episodes = document.select("div.vid-episodes a, div.gmr-listseries a")
				.mapNotNull { eps ->
					val href = fixUrl(eps.attr("href"))
					val rawTitle = eps.attr("title").takeIf { it.isNotBlank() } ?: eps.text()
					val cleanTitle = rawTitle.replaceFirst(Regex("(?i)Permalink ke\\s*"), "").trim()

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
				this.posterUrl = poster
				this.year = year
				this.plot = description
				this.tags = tags
				addScore(rating)
				addActors(actors)
				this.recommendations = recommendations
				this.duration = duration ?: 0
				addTrailer(trailer)
			}
		} else {
			newMovieLoadResponse(title, url, TvType.Movie, url) {
				this.posterUrl = poster
				this.year = year
				this.plot = description
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
				val iframe = app.get(fixUrl(ele.attr("href")))
					.document
					.selectFirst("div.gmr-embed-responsive iframe")
					.getIframeAttr()
					?.let { httpsify(it) }
					?: return@amap

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
				)
					.document
					.select("iframe")
					.attr("src")
					.let { httpsify(it) }

				loadExtractor(server, "$directUrl/", subtitleCallback, callback)
			}
		}

		return true
	}

    private fun Element.getImageAttr(): String {
        return when {
            this.hasAttr("data-src") -> this.attr("abs:data-src")
            this.hasAttr("data-lazy-src") -> this.attr("abs:data-lazy-src")
            this.hasAttr("srcset") -> this.attr("abs:srcset").substringBefore(" ")
            else -> this.attr("abs:src")
        }
    }

    private fun Element?.getIframeAttr(): String? {
        return this?.attr("data-litespeed-src").takeIf { it?.isNotEmpty() == true }
                ?: this?.attr("src")
    }

    private fun String?.fixImageQuality(): String? {
        if (this == null) return null
        val regex = Regex("(-\\d*x\\d*)").find(this)?.groupValues?.get(0) ?: return this
        return this.replace(regex, "")
    }

    private fun getBaseUrl(url: String): String {
        return URI(url).let { "${it.scheme}://${it.host}" }
    }
}
