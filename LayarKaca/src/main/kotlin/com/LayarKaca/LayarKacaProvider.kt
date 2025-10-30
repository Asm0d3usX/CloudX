package com.layarKacaProvider

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.json.JSONObject
import org.jsoup.nodes.Element
import java.net.URI

// 🎬 Provider utama untuk situs LayarKaca
class LayarKacaProvider : MainAPI() {

    // 🌐 URL utama dan konfigurasi dasar
    override var mainUrl = "https://lk21.de"
    private var seriesUrl = "https://series.lk21.de"
    private var searchurl = "https://gudangvape.com"
    override var name = "LayarKaca"
    override val hasMainPage = true
    override var lang = "id"
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.AsianDrama
    )

    // 🏠 Halaman utama (main page sections)
    override val mainPage = mainPageOf(
        "$mainUrl/populer/page/" to "🔥 Film Terpopuler",
        "$mainUrl/rating/page/" to "⭐ Berdasarkan IMDb Rating",
        "$mainUrl/most-commented/page/" to "💬 Film dengan Komentar Terbanyak",
        "$seriesUrl/latest-series/page/" to "🎞️ Series Terbaru",
        "$seriesUrl/series/asian/page/" to "🌏 Film Asian Terbaru",
        "$mainUrl/latest/page/" to "🆕 Film Upload Terbaru",
        "$mainUrl/genre/animation/page/" to "🎨 Animation",
        "$mainUrl/country/thailand/page/" to "🇹🇭 Thailand",
        "$mainUrl/country/philippines/page/" to "🇵🇭 Philippines",
    )

    // 📺 Ambil konten utama
    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data + page).document
        val home = document.select("article figure").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    // 🔗 Mengatur URL redirect untuk series atau movie
    private suspend fun getProperLink(url: String): String {
        if (url.startsWith(seriesUrl)) return url
        val res = app.get(url).document
        return if (res.select("title").text().contains("Nontondrama", true)) {
            res.selectFirst("a#openNow")?.attr("href")
                ?: res.selectFirst("div.links a")?.attr("href")
                ?: url
        } else {
            url
        }
    }

    // 🎯 Konversi elemen HTML ke hasil pencarian
    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3")?.ownText()?.trim() ?: return null
        val href = fixUrl(this.selectFirst("a")!!.attr("href"))
        val posterUrl = fixUrlNull(this.selectFirst("img")?.getImageAttr())
        val type = if (this.selectFirst("span.episode") == null) TvType.Movie else TvType.TvSeries
        val posterheaders = mapOf("Referer" to getBaseUrl(posterUrl))

        return if (type == TvType.TvSeries) {
            val episode = this.selectFirst("span.episode strong")?.text()?.filter { it.isDigit() }?.toIntOrNull()
            newAnimeSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
                this.posterHeaders = posterheaders
                addSub(episode)
            }
        } else {
            val quality = this.select("div.quality").text().trim()
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
                this.posterHeaders = posterheaders
                addQuality(quality)
            }
        }
    }

    // 🔍 Fungsi pencarian
    override suspend fun search(query: String): List<SearchResponse> {
		val url = "$mainUrl/?s=$query&post_type[]=post&post_type[]=tv"
		val document = app.get(url).document

		val results = mutableListOf<SearchResponse>()

		document.select("article").forEach { article ->
			val a = article.selectFirst("a[itemprop=url]") ?: return@forEach
			val href = fixUrl(a.attr("href"))
			val title = article.selectFirst("h3.poster-title")?.text()?.trim() ?: return@forEach
			val poster = article.selectFirst("img")?.attr("src")?.ifEmpty { null }
			val year = article.selectFirst("span.year")?.text()?.toIntOrNull()
			val epsElement = article.selectFirst("span.episode")
			val epsText = epsElement?.selectFirst("strong")?.text()?.trim()
			val isSeries = epsElement != null

			val type = if (isSeries) {
				TvType.TvSeries
			} else {
				TvType.Movie
			}

			results.add(
				if (type == TvType.TvSeries) {
					newTvSeriesSearchResponse(title, href, type) {
						this.posterUrl = poster
						this.year = year
					}
				} else {
					newMovieSearchResponse(title, href, type) {
						this.posterUrl = poster
						this.year = year
					}
				}
			)
		}
		
		return results
	}


    // 📄 Load detail film/series
    override suspend fun load(url: String): LoadResponse {
        val fixUrl = getProperLink(url)
        val document = app.get(fixUrl).document
        val baseurl = fetchURL(fixUrl)
        val title = document.selectFirst("div.movie-info h1")?.text()?.trim().toString()
        val poster = document.select("meta[property=og:image]").attr("content")
        val tags = document.select("div.tag-list span").map { it.text() }
        val posterheaders = mapOf("Referer" to getBaseUrl(poster))

        val year = Regex("\\d, (\\d+)").find(
            document.select("div.movie-info h1").text().trim()
        )?.groupValues?.get(1).toString().toIntOrNull()
        val tvType = if (document.selectFirst("#season-data") != null) TvType.TvSeries else TvType.Movie
        val description = document.selectFirst("div.meta-info")?.text()?.trim()
        val trailer = document.selectFirst("ul.action-left > li:nth-child(3) > a")?.attr("href")
        val rating = document.selectFirst("div.info-tag strong")?.text()

        val recommendations = document.select("li.slider article").map {
            val recName = it.selectFirst("h3")?.text()?.trim().toString()
            val recHref = baseurl + it.selectFirst("a")!!.attr("href")
            val recPosterUrl = fixUrl(it.selectFirst("img")?.attr("src").toString())
            newTvSeriesSearchResponse(recName, recHref, TvType.TvSeries) {
                this.posterUrl = recPosterUrl
                this.posterHeaders = posterheaders
            }
        }

        return if (tvType == TvType.TvSeries) {
            val json = document.selectFirst("script#season-data")?.data()
            val episodes = mutableListOf<Episode>()

            if (json != null) {
                val root = JSONObject(json)
                root.keys().forEach { seasonKey ->
                    val seasonArr = root.getJSONArray(seasonKey)
                    for (i in 0 until seasonArr.length()) {
                        val ep = seasonArr.getJSONObject(i)
                        val href = fixUrl("$baseurl/" + ep.getString("slug"))
                        val episodeNo = ep.optInt("episode_no")
                        val seasonNo = ep.optInt("s")

                        episodes.add(
                            newEpisode(href) {
                                this.name = "Episode $episodeNo"
                                this.season = seasonNo
                                this.episode = episodeNo
                            }
                        )
                    }
                }
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.posterHeaders = posterheaders
                this.year = year
                this.plot = description
                this.tags = tags
                this.score = Score.from10(rating)
                this.recommendations = recommendations
                addTrailer(trailer)
            }

        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.posterHeaders = posterheaders
                this.year = year
                this.plot = description
                this.tags = tags
                this.score = Score.from10(rating)
                this.recommendations = recommendations
                addTrailer(trailer)
            }
        }
    }

    // 🎥 Load link streaming & subtitle
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        document.select("ul#player-list > li").map {
            fixUrl(it.select("a").attr("href"))
        }.amap {
            val test = it.getIframe()
            val referer = getBaseUrl(it)
            Log.d("Phisher", test)
            loadExtractor(it.getIframe(), referer, subtitleCallback, callback)
        }
        return true
    }

    // 🧩 Ambil iframe dari halaman
    private suspend fun String.getIframe(): String {
        return app.get(this, referer = "$seriesUrl/").document
            .select("div.embed-container iframe")
            .attr("src")
    }

    // 🌍 Ambil base URL tanpa path
    private suspend fun fetchURL(url: String): String {
        val res = app.get(url, allowRedirects = false)
        val href = res.headers["location"]
        return if (href != null) {
            val it = URI(href)
            "${it.scheme}://${it.host}"
        } else {
            url
        }
    }

    // 🖼️ Ambil atribut gambar (src/data-src)
    private fun Element.getImageAttr(): String {
        return when {
            this.hasAttr("src") -> this.attr("src")
            this.hasAttr("data-src") -> this.attr("data-src")
            else -> this.attr("src")
        }
    }

    // 🔗 Ambil base domain dari URL
    fun getBaseUrl(url: String?): String {
        return URI(url).let {
            "${it.scheme}://${it.host}"
        }
    }
}
