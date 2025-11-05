package com.wgfilm21

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addScore
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import java.net.URI

class WGFilm21 : MainAPI() {
    override var mainUrl = "https://go1.wgfilm21.com"
    override var name = "WGFilm21"
    override val hasMainPage = true
    override var lang = "id"
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama
    )

    override val mainPage = mainPageOf(
        "best-rating/page/%d/" to "Best Rating",
        "category/action/page/%d/" to "Action",
        "category/adventure/page/%d/" to "Adventure",
        "category/mystery/page/%d/" to "Mystery",
        "category/animation/page/%d/" to "Animation",
        "country/semi-filipina/page/%d/" to "Philippines",
        "country/china/page/%d/" to "China",
        "country/japan/page/%d/" to "Japan",
        "category/adult/page/%d/" to "Adult"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${mainUrl}/${request.data.format(page)}").document
        val items = document.select("article.item-infinite.item").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, items)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = selectFirst("h2.entry-title > a")?.text()?.trim() ?: return null
        val href = fixUrl(selectFirst("h2.entry-title > a")?.attr("href") ?: return null)
        val poster = fixUrlNull(selectFirst("div.content-thumbnail img")?.getImageAttr())?.fixImageQuality()
        val quality = selectFirst("div.gmr-qual, div.gmr-quality-item")?.text()?.trim()?.replace("-", "")
        val isTvShow = this.selectFirst("div.gmr-posttype-item:contains(TV Show)") != null ||
                this.selectFirst("div.gmr-numbeps") != null

        return if (isTvShow) {
            val epsCount = selectFirst("div.gmr-numbeps span")?.text()?.toIntOrNull()
            newAnimeSearchResponse(title, href, TvType.TvSeries) {
                posterUrl = poster
                addSub(epsCount)
            }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) {
                posterUrl = poster
                if (!quality.isNullOrEmpty()) addQuality(quality)
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val fixedQuery = query.replace(" ", "+")
        val searchUrl = "$mainUrl/?s=$fixedQuery&post_type[]=post&post_type[]=tv"
        val document = app.get(searchUrl).document
        return document.select("article.item-infinite.item").mapNotNull { it.toSearchResult() }
    }

    private fun Element.toRecommendResult(): SearchResponse? {
        val title = selectFirst(".gmr-slide-title a")?.text()?.trim() ?: return null
        val href = fixUrl(selectFirst("a")?.attr("href") ?: return null)
        val poster = fixUrlNull(
            selectFirst("img")?.attr("data-src") ?: selectFirst("img")?.attr("src")
        )?.fixImageQuality()
        val isTvShow = href.contains("/tv/", ignoreCase = true)

        return if (isTvShow) {
            newAnimeSearchResponse(title, href, TvType.TvSeries) { posterUrl = poster }
        } else {
            newMovieSearchResponse(title, href, TvType.Movie) { posterUrl = poster }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1.entry-title")?.text()
            ?.substringBefore("Season")?.substringBefore("Episode")?.trim().orEmpty()
        val poster = fixUrlNull(
            document.selectFirst("figure.pull-left img, div.content-thumbnail img, div.other-content-thumbnail img")
                ?.getImageAttr()
        )?.fixImageQuality()
        val tags = document.select("div.gmr-movie-on a, div.gmr-moviedata a").map { it.text() }
        val year = document.select("time[itemprop=dateCreated]")
            .attr("datetime").takeIf { it.isNotEmpty() }?.substringBefore("-")?.toIntOrNull()
        val tvType = if (url.contains("/tv/")) TvType.TvSeries else TvType.Movie
        val description = document.selectFirst("div[itemprop=description] > p, div.gmr-moviedata p")?.text()?.trim()
        val trailer = document.selectFirst("a.gmr-trailer-popup")?.attr("href")
        val rating = document.selectFirst("div.gmr-meta-rating span[itemprop=ratingValue], div.gmr-rating-item")
            ?.text()?.trim()
        val actors = document.select("span[itemprop=director] a, span[itemprop=actors] a").map { it.text() }
        val duration = document.selectFirst("time[property=duration], span[property=duration]")
            ?.text()?.replace(Regex("\\D"), "")?.toIntOrNull()
        val recommendations = document.select("div.gmr-slider-content").mapNotNull { it.toRecommendResult() }

        return if (tvType == TvType.TvSeries) {
            val episodes = document.select("div.vid-episodes a, div.gmr-listseries a")
                .mapNotNull { eps ->
                    val href = fixUrl(eps.attr("href"))
                    val name = eps.text().trim()
                    val episode = name.split(" ").lastOrNull()?.filter { it.isDigit() }?.toIntOrNull()
                    val season = Regex("Season\\s?(\\d+)").find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    newEpisode(href) {
                        this.name = name
                        this.episode = episode
                        this.season = season
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

		document.select("div.gmr-embed-responsive iframe").forEach { frame ->
			val iframe = frame.getIframeAttr()?.let { httpsify(it) } ?: return@forEach
			println("🎥 [IFRAME] URL ➜ $iframe")
            println("📦 [DATA] ➜ $data")
			loadExtractor(iframe, data, subtitleCallback, callback)
		}

		document.select("ul.muvipro-player-tabs li a[href^=#]").forEach { tab ->
			val tabId = tab.attr("href").removePrefix("#")
			val iframe = document
				.selectFirst("div#$tabId iframe")
				?.getIframeAttr()
				?.let { httpsify(it) }

			if (iframe != null) {
				println("🎥 [IFRAME] URL ➜ $iframe")
                println("📦 [DATA] ➜ $data")
				loadExtractor(iframe, data, subtitleCallback, callback)
			}
		}

		if (id != null && document.select("iframe").isEmpty()) {
			document.select("div.tab-content-ajax").forEach { ele ->
				val tabId = ele.attr("id")
				val serverUrl = "$mainUrl/wp-admin/admin-ajax.php"

				val res = app.post(
					serverUrl,
					data = mapOf(
						"action" to "muvipro_player_content",
						"tab" to tabId,
						"post_id" to id
					)
				).document
					.selectFirst("iframe")
					?.attr("src")
					?.let { httpsify(it) }

				if (!res.isNullOrEmpty()) {
					println("🎥 [RES] URL ➜ $res")
					println("📦 [DATA] ➜ $data")
					loadExtractor(res, data, subtitleCallback, callback)
				}
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
