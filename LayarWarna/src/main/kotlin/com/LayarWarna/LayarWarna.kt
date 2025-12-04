package com.layarwarna

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addScore
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import java.net.URI

class LayarWarna : MainAPI() {
    override var mainUrl = "https://play5.layarwarna21.tv"
    override var name = "LayarWarna"
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
		"genre/bioskopkeren/page/%d/" to "Box Office",
		"action/page/%d/" to "Action",
		"animation/page/%d/" to "Animation",
		"comedy/page/%d/" to "Comedy",
		"drama/page/%d/" to "Drama",
		"romance/page/%d/" to "Romance",
        "thriller/page/%d/" to "Thriller",
		"war/page/%d/" to "War",
		"country/china/page/%d/" to "China",
        "country/indonesia/page/%d/" to "Indonesia",
        "country/philippines/page/%d/" to "Philippines"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/${request.data.format(page)}").document
        val items = document.select("article.item").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, items)
    }
	
	private fun Element.toSearchResult(): SearchResponse? {
		val title = selectFirst("h2.entry-title > a")?.text()?.trim() ?: return null
		val href = fixUrl(selectFirst("h2.entry-title > a")?.attr("href") ?: return null)
		val poster = fixUrlNull(selectFirst("div.content-thumbnail img")?.getImageAttr())?.fixImageQuality()
		val quality = selectFirst("div.gmr-quality-item a")?.text()?.trim()
		val ratingText = this.selectFirst("div.gmr-rating-item")?.ownText()?.trim()

		return newMovieSearchResponse(title, href, TvType.Movie) {
			this.posterUrl = poster
			this.quality = getQualityFromString(quality)
			this.score = Score.from10(ratingText?.toDoubleOrNull())
		}
	}

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl?s=$query&post_type[]=post&post_type[]=tv").document
        return document.select("article.item").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toRecommendResult(): SearchResponse? {
        val title = selectFirst("h2.entry-title > a")?.text()?.trim() ?: return null
		val href = fixUrl(selectFirst("h2.entry-title > a")?.attr("href") ?: return null)
		val poster = fixUrlNull(selectFirst("div.content-thumbnail img")?.getImageAttr())?.fixImageQuality()
		val quality = selectFirst("div.gmr-quality-item a")?.text()?.trim()
		val ratingText = this.selectFirst("div.gmr-rating-item")?.ownText()?.trim()

		return newMovieSearchResponse(title, href, TvType.Movie) {
			this.posterUrl = poster
			this.quality = getQualityFromString(quality)
			this.score = Score.from10(ratingText?.toDoubleOrNull())
		}
    }
	
	override suspend fun load(url: String): LoadResponse {
		val fetch = app.get(url)
		val document = fetch.document
		directUrl = getBaseUrl(fetch.url)

		val title = document.selectFirst("h1.entry-title")?.text()?.trim().orEmpty()
		val poster = fixUrlNull(document.selectFirst("div.gmr-movie-data figure img")?.getImageAttr()?.fixImageQuality())
		val tags = document.select("div.gmr-moviedata a, div.gmr-movie-on a[rel='category tag']")
			.map { it.text() }
		val year = document.selectFirst("div.gmr-moviedata strong:contains(Year:) > a")
			?.text()?.trim()?.toIntOrNull()
			?: Regex("\\((\\d{4})\\)").find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()

		val description = document.selectFirst("div[itemprop=description] p")?.text()?.trim()
		val trailer = document.selectFirst("ul.gmr-player-nav a.gmr-trailer-popup")?.attr("href")
		val rating = document.selectFirst("div.gmr-meta-rating span[itemprop=ratingValue], div.gmr-rating-item")
			?.text()?.trim()
		val actors = document.select("span[itemprop=actors] a").map { it.text() }
		val duration = document.selectFirst("span[property=duration], div.gmr-duration-item")
			?.text()?.replace(Regex("\\D"), "")?.toIntOrNull()
		val recommendations = document.select("article.item.col-md-20").mapNotNull { it.toRecommendResult() }

		return newMovieLoadResponse(title, url, TvType.Movie, url) {
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
	
	override suspend fun loadLinks(
		data: String,
		isCasting: Boolean,
		subtitleCallback: (SubtitleFile) -> Unit,
		callback: (ExtractorLink) -> Unit
	): Boolean {
		val document = app.get(data).document
		val id = document.selectFirst("div#muvipro_player_content_id")?.attr("data-id")

		if (id.isNullOrEmpty()) {
			document.select("div.gmr-embed-responsive iframe").forEach { frame ->
				val iframeUrl = frame.getIframeAttr()?.let { httpsify(it) } ?: return@forEach
				loadExtractor(iframeUrl, "$directUrl/", subtitleCallback, callback)
			}
		} else {
			document.select("div.tab-content-ajax").forEach { ele ->
				val tabId = ele.attr("id")
				if (tabId.isNotEmpty()) {
					val response = app.post(
						"$directUrl/wp-admin/admin-ajax.php",
						data = mapOf(
							"action" to "muvipro_player_content",
							"tab" to tabId,
							"post_id" to id
						),
					).document

					response.select("iframe").forEach { iframe ->
						val src = iframe.attr("src")?.let { httpsify(it) } ?: return@forEach
						loadExtractor(src, "$directUrl/", subtitleCallback, callback)
					}
				}
			}
		}

		document.select("ul.gmr-download-list li a").forEach { link ->
			val downloadUrl = link.attr("href")?.trim().orEmpty()
			if (downloadUrl.isNotEmpty()) {
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
