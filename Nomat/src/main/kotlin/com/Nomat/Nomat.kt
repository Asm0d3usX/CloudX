package com.nomat

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.LoadResponse.Companion.addScore
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.httpsify
import com.lagradost.cloudstream3.utils.loadExtractor
import java.net.URI
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.base64Decode

class Nomat : MainAPI() {

    override var mainUrl = "https://nomat.site"
    private var directUrl: String? = null
    override var name = "Nomat"
    override val hasMainPage = true
    override var lang = "id"
    override val supportedTypes =
            setOf(
				TvType.Movie, 
				TvType.TvSeries, 
				TvType.Anime, 
				TvType.AsianDrama
			)

    override val mainPage = mainPageOf(
		"slug/film-terbaru/%d/" to "Terbaru",
		"slug/film-box-office/%d/" to "Box Office",
		"slug/film-serial-baru-terpopuler/%d/" to "TV Series",
		"category/genre/action/%d/" to "Action",
		"slug/film-movie-anime/%d/" to "Animation",
		"category/genre/history/%d/" to "History",
		"category/genre/horror/%d/" to "Horror",
		"category/genre/romance/%d/" to "Romance",
		"category/country/japan/%d/" to "Japan",
		"category/country/philippines/%d/" to "Philippines",
		"category/country/thailand/%d/" to "Thailand"
	)
	
	override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/${request.data.format(page)}").document
        val items = document.select("a:has(.item-content)").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, items)
    }
	
	private fun Element.toSearchResult(): SearchResponse? {
		val href = this.attr("href")
		if (href.isNullOrBlank()) return null
		val title = this.selectFirst(".title")?.text()?.trim() ?: return null
		val posterStyle = this.selectFirst(".poster")?.attr("style").orEmpty()
		val poster = Regex("url\\('(.*?)'\\)").find(posterStyle)?.groupValues?.get(1)
		val rating = this.selectFirst(".rtg")?.ownText()?.trim()
		val quality = this.selectFirst("div.qual")?.text()?.trim()
		val ratingText = selectFirst("div.rtg")?.ownText()?.trim()
		val epsText = this.selectFirst("div.qual")?.text()?.trim()
		val episode = Regex("Eps.?\\s?([0-9]+)", RegexOption.IGNORE_CASE)
			.find(epsText ?: "")
			?.groupValues?.getOrNull(1)?.toIntOrNull()
		
		return if (episode != null || title.contains("Season", true) || title.contains("Episode", true)) {
			newAnimeSearchResponse(title, href, TvType.TvSeries) {
				posterUrl = poster
				addQuality(quality ?: "")
				this.score = Score.from10(ratingText?.toDoubleOrNull())
				if (episode != null) addSub(episode)
			}
		} else {
			newMovieSearchResponse(title, href, TvType.Movie) {
				posterUrl = poster
				addQuality(quality ?: "")   
				this.score = Score.from10(ratingText?.toDoubleOrNull())
			}
		}
	}

	override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/search/$query/").document
        return document.select("a:has(div.item-content)").mapNotNull { it.toSearchResult() }
    }    

    private fun Element.toRecommendResult(): SearchResponse? {
		val href = this.attr("href")
		if (href.isNullOrBlank()) return null
		val title = this.selectFirst(".title")?.text()?.trim() ?: return null
		val posterStyle = this.selectFirst(".poster")?.attr("style").orEmpty()
		val poster = Regex("url\\('(.*?)'\\)").find(posterStyle)?.groupValues?.get(1)
		val rating = this.selectFirst(".rtg")?.ownText()?.trim()
		val quality = this.selectFirst("div.qual")?.text()?.trim()
		val ratingText = selectFirst("div.rtg")?.ownText()?.trim()
		val epsText = this.selectFirst("div.qual")?.text()?.trim()
		val episode = Regex("Eps.?\\s?([0-9]+)", RegexOption.IGNORE_CASE)
			.find(epsText ?: "")
			?.groupValues?.getOrNull(1)?.toIntOrNull()
		
		return if (episode != null || title.contains("Season", true) || title.contains("Episode", true)) {
			newAnimeSearchResponse(title, href, TvType.TvSeries) {
				posterUrl = poster
				addQuality(quality ?: "")
				this.score = Score.from10(ratingText?.toDoubleOrNull())
				if (episode != null) addSub(episode)
			}
		} else {
			newMovieSearchResponse(title, href, TvType.Movie) {
				posterUrl = poster
				addQuality(quality ?: "")   
				this.score = Score.from10(ratingText?.toDoubleOrNull())
			}
		}
	}

	override suspend fun load(url: String): LoadResponse {
		val document = app.get(url).document
		val title = document.selectFirst("div.video-title h1")?.text()
			?.substringBefore("Season")
			?.substringBefore("Episode")
			?.trim()
			?: ""

		val poster = fixUrlNull(
			document.selectFirst("div.video-poster")?.attr("style")
				?.substringAfter("url('")
				?.substringBefore("')")
		)?.fixImageQuality()

		val tags = document.select("div.video-genre a").map { it.text() }
		val year = document.select("div.video-duration a[href*=/category/year/]").text().toIntOrNull()
		val description = document.selectFirst("div.video-synopsis")?.text()?.trim()
		val trailer = document.selectFirst("div.video-trailer iframe")?.attr("src")
		val rating = document.selectFirst("div.rtg")?.text()?.trim()
		val actors = document.select("div.video-actor a").map { it.text() }
		val recommendations = document.select("a:has(.item-content)").mapNotNull { it.toRecommendResult() }

		val isSeries = url.contains("/serial-tv/") || document.select("div.video-episodes a").isNotEmpty()

		return if (isSeries) {
			val episodes = document.select("div.video-episodes a").map { eps ->
				val href = fixUrl(eps.attr("href"))
				val number = Regex("\\d+").find(eps.text())?.value?.toIntOrNull()
				val name = number?.let { "Episode $it" } ?: "Episode"
				val season = Regex("Season\\s?(\\d+)").find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()

				newEpisode(href) {
					this.name = name
					this.episode = number
					this.season = season
					this.posterUrl = poster
				}
			}

			newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
				this.posterUrl = poster
				this.year = year
				this.plot = description
				this.tags = tags
				addActors(actors)
				this.recommendations = recommendations
				addTrailer(trailer)
				addScore(rating ?: "")
			}
		} else {
			val playUrl = document.selectFirst("div.video-wrapper a[href*='nontonhemat.link']")?.attr("href")

			newMovieLoadResponse(title, url, TvType.Movie, playUrl ?: url) {
				this.posterUrl = poster
				this.year = year
				this.plot = description
				this.tags = tags
				addActors(actors)
				this.recommendations = recommendations
				addTrailer(trailer)
				addScore(rating ?: "")
			}
		}
	}
	
	override suspend fun loadLinks(
		data: String,
		isCasting: Boolean,
		subtitleCallback: (SubtitleFile) -> Unit,
		callback: (ExtractorLink) -> Unit
	): Boolean {
		return try {
			val nhDoc = app.get(data, referer = mainUrl, timeout = 100L).document

			nhDoc.select("div.server-item").forEach { el ->
				val encoded = el.attr("data-url")
				if (encoded.isNotBlank()) {
					try {
						val decoded = base64Decode(encoded)
						loadExtractor(decoded, data, subtitleCallback, callback)
					} catch (_: Exception) {
						println("Decode Error")
					}
				}
			}
			true
		} catch (_: Exception) {
			false
		}
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
