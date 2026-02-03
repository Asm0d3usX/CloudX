package com.sflix

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.LoadResponse.Companion.addScore
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.nicehttp.RequestBodyTypes
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class Sflix : MainAPI() {
    override var mainUrl = "https://sflix.film"
    private val apiUrl = "https://sflix.film"
    override val instantLinkLoading = true
    override var name = "Sflix"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override var lang = "id"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Anime, TvType.AsianDrama)

    override val mainPage = mainPageOf(
        "872031290915189720" to "Trending",
		"6528093688173053896" to "Indonesian Movie",
		"5283462032510044280" to "Indonesian Drama",
		"997144265920760504" to "Hollywood",
		"4380734070238626200" to "K-Drama",
		"5004039650198045488" to "Comedy",
		"5307082080063488480" to "Western TV",
		"8617025562613270856" to "Anime",
		"5549742004948601072" to "Dub Indo",
		"8624142774394406504" to "C-Drama",
		"7132534597631837112" to "Animation",
		"1653005382303864120" to "Monster & Titan",
		"4539350473970797944" to "Romance",
		"3766111568753312664" to "Cyberpunk",
		"1164329479448281992" to "Thai-Drama",
		"4753014112567937896" to "The Hero",
		"5848753831881965888" to "Horror",
		"7749480172839487888" to "Family"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = "$mainUrl/wefeed-h5-bff/web/ranking-list/content?id=${request.data}&page=$page&perPage=12"
        val home = app.get(url).parsedSafe<Media>()?.data?.subjectList?.map { it.toSearchResponse(this) }
            ?: throw ErrorLoadingException("No Data Found")
        return newHomePageResponse(request.name, home)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun search(query: String): List<SearchResponse> {
        val body = mapOf("keyword" to query, "page" to "1", "perPage" to "0", "subjectType" to "0")
            .toJson().toRequestBody(RequestBodyTypes.JSON.toMediaTypeOrNull())
        return app.post("$mainUrl/wefeed-h5-bff/web/subject/search", requestBody = body)
            .parsedSafe<Media>()?.data?.items?.map { it.toSearchResponse(this) } ?: throw ErrorLoadingException()
    }

    override suspend fun load(url: String): LoadResponse {
        val id = url.substringAfterLast("/")
        val doc = app.get("$mainUrl/wefeed-h5-bff/web/subject/detail?subjectId=$id").parsedSafe<MediaDetail>()?.data
        val subject = doc?.subject
        val title = subject?.title ?: ""
        val poster = subject?.cover?.url
        val tags = subject?.genre?.split(",")?.map { it.trim() }
        val year = subject?.releaseDate?.substringBefore("-")?.toIntOrNull()
        val tvType = if (subject?.subjectType == 2) TvType.TvSeries else TvType.Movie
        val description = subject?.description
        val trailer = subject?.trailer?.videoAddress?.url
        val score = subject?.imdbRatingValue?.let { Score.from10(it.toFloat()) }
        val actors = doc?.stars?.mapNotNull { cast ->
            ActorData(Actor(cast.name ?: return@mapNotNull null, cast.avatarUrl), roleString = cast.character)
        }?.distinctBy { it.actor }
        val recommendations = app.get("$mainUrl/wefeed-h5-bff/web/subject/detail-rec?subjectId=$id&page=1&perPage=12")
            .parsedSafe<Media>()?.data?.items?.map { it.toSearchResponse(this) }

        return if (tvType == TvType.TvSeries) {
            val episodes = doc?.resource?.seasons?.map { seasons ->
                (if (seasons.allEp.isNullOrEmpty()) (1..seasons.maxEp!!) else seasons.allEp.split(",").map { it.toInt() })
                    .map { episode -> newEpisode(LoadData(id, seasons.se, episode, subject?.detailPath).toJson()) {
                        this.season = seasons.se
                        this.episode = episode
						this.posterUrl = poster
                    }}
            }?.flatten() ?: emptyList()
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.score = score
                this.actors = actors
                this.recommendations = recommendations
                addTrailer(trailer, addRaw = true)
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, LoadData(id, detailPath = subject?.detailPath).toJson()) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.score = score
                this.actors = actors
                this.recommendations = recommendations
                addTrailer(trailer, addRaw = true)
            }
        }
    }
	
	override suspend fun loadLinks(
		data: String,
		isCasting: Boolean,
		subtitleCallback: (SubtitleFile) -> Unit,
		callback: (ExtractorLink) -> Unit
	): Boolean {
		val media = parseJson<LoadData>(data)

		try {
			val referer = "$apiUrl/spa/videoPlayPage/movies/${media.detailPath}?id=${media.id}&type=/movie/detail&lang=en"
			val streams = app.get("$apiUrl/wefeed-h5-bff/web/subject/play?subjectId=${media.id}&se=${media.season ?: 0}&ep=${media.episode ?: 0}",
				referer = referer
			).parsedSafe<Media>()?.data?.streams

			if (!streams.isNullOrEmpty()) {
				streams.reversed().distinctBy { it.url }.forEach { source ->
					callback(
						newExtractorLink(
							source = this.name,
							name = "SFLIX",
							url = source.url ?: return@forEach,
							type = INFER_TYPE
						) {
							this.referer = "$apiUrl/"
							this.quality = getQualityFromName(source.resolutions)
						}
					)
				}

				val id = streams.first().id
				val format = streams.first().format

				app.get("$apiUrl/wefeed-h5-bff/web/subject/caption?format=$format&id=$id&subjectId=${media.id}",
					referer = referer
				).parsedSafe<Media>()?.data?.captions?.forEach { sub ->
					subtitleCallback(
						SubtitleFile(sub.lanName ?: "", sub.url ?: return@forEach)
					)
				}

				return true
			}

		} catch (_: Exception) {}

		return false
	}

    data class LoadData(val id: String? = null, val season: Int? = null, val episode: Int? = null, val detailPath: String? = null)

    data class Media(@JsonProperty("data") val data: Data? = null) {
        data class Data(
            @JsonProperty("subjectList") val subjectList: ArrayList<Items>? = arrayListOf(),
            @JsonProperty("items") val items: ArrayList<Items>? = arrayListOf(),
            @JsonProperty("streams") val streams: ArrayList<Streams>? = arrayListOf(),
            @JsonProperty("captions") val captions: ArrayList<Captions>? = arrayListOf()
        ) {
            data class Streams(@JsonProperty("id") val id: String? = null, @JsonProperty("format") val format: String? = null, @JsonProperty("url") val url: String? = null, @JsonProperty("resolutions") val resolutions: String? = null)
            data class Captions(@JsonProperty("lan") val lan: String? = null, @JsonProperty("lanName") val lanName: String? = null, @JsonProperty("url") val url: String? = null)
        }
    }

    data class MediaDetail(@JsonProperty("data") val data: Data? = null) {
        data class Data(
            @JsonProperty("subject") val subject: Items? = null,
            @JsonProperty("stars") val stars: ArrayList<Stars>? = arrayListOf(),
            @JsonProperty("resource") val resource: Resource? = null
        ) {
            data class Stars(@JsonProperty("name") val name: String? = null, @JsonProperty("character") val character: String? = null, @JsonProperty("avatarUrl") val avatarUrl: String? = null)
            data class Resource(@JsonProperty("seasons") val seasons: ArrayList<Seasons>? = arrayListOf()) {
                data class Seasons(@JsonProperty("se") val se: Int? = null, @JsonProperty("maxEp") val maxEp: Int? = null, @JsonProperty("allEp") val allEp: String? = null)
            }
        }
    }

    data class Items(
		@JsonProperty("subjectId") val subjectId: String? = null,
		@JsonProperty("subjectType") val subjectType: Int? = null,
		@JsonProperty("title") val title: String? = null,
		@JsonProperty("description") val description: String? = null,
		@JsonProperty("releaseDate") val releaseDate: String? = null,
		@JsonProperty("duration") val duration: Long? = null,
		@JsonProperty("genre") val genre: String? = null,
		@JsonProperty("cover") val cover: Cover? = null,
		@JsonProperty("imdbRatingValue") val imdbRatingValue: String? = null,
		@JsonProperty("countryName") val countryName: String? = null,
		@JsonProperty("trailer") val trailer: Trailer? = null,
		@JsonProperty("detailPath") val detailPath: String? = null
	) {
		fun toSearchResponse(provider: Sflix): SearchResponse {
			return provider.newMovieSearchResponse(
				title ?: "",
				subjectId!!,
				if (subjectType == 1) TvType.Movie else TvType.TvSeries,
				false
			) {
				this.posterUrl = cover?.url
				imdbRatingValue?.toDoubleOrNull()?.let { score ->
					this.score = Score.from10(score)
				}
				this.quality = extractQuality()
			}
		}

		fun extractQuality(): SearchQuality? {
		val height = trailer?.videoAddress?.height ?: return null
    
		return when {
			height >= 2160 -> SearchQuality.UHD
			height >= 1440 -> SearchQuality.UHD
			height >= 1080 -> SearchQuality.BlueRay
			height >= 720  -> SearchQuality.HD
			height >= 480  -> SearchQuality.SD
			else -> SearchQuality.HD 
		}
	}

		data class Cover(@JsonProperty("url") val url: String? = null)

		data class Trailer(
			@JsonProperty("videoAddress") val videoAddress: VideoAddress? = null
		) {
			data class VideoAddress(
				@JsonProperty("url") val url: String? = null,
				@JsonProperty("height") val height: Int? = null,
				@JsonProperty("width") val width: Int? = null
			)
		}
	}
}
