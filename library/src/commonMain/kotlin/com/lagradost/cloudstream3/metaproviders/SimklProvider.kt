package com.lagradost.cloudstream3.metaproviders

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.APIHolder.unixTimeMS
import com.lagradost.cloudstream3.Actor
import com.lagradost.cloudstream3.ActorData
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbId
import com.lagradost.cloudstream3.LoadResponse.Companion.addTMDbId
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.NextAiring
import com.lagradost.cloudstream3.ProviderType
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SearchResponseList
import com.lagradost.cloudstream3.ShowStatus
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.addDate
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newSearchResponseList
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import java.text.SimpleDateFormat
import java.util.Locale

open class SimklProvider : MainAPI() {
    override var name = "Simkl"
    override val hasMainPage = true
    override val providerType = ProviderType.MetaProvider
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime,
    )

    private val simklApiUrl = "https://api.simkl.com"
    private val clientId = BuildConfig.SIMKL_CLIENT_ID

    override val mainPage = mainPageOf(
        "$simklApiUrl/tv/trending" to "Trending Series",
        "$simklApiUrl/movies/trending" to "Trending Movies",
        "$simklApiUrl/anime/trending" to "Trending Anime",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = "${request.data}?client_id=$clientId&extended=full&page=$page"
        val apiResponse = app.get(url).text

        val items = parseJson<List<SimklSearchItem>>(apiResponse)
            .mapNotNull { it.toSearchResponse() }

        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val res = app.get(
            "$simklApiUrl/search/anime?q=$query&client_id=$clientId&type=movie,tv,anime&extended=full&page=$page"
        ).text

        val items = parseJson<List<SimklSearchItem>>(res)
            .mapNotNull { it.toSearchResponse() }

        return newSearchResponseList(items)
    }

    override suspend fun load(url: String): LoadResponse {
        val data = parseJson<SimklData>(url)

        val typeStr = when (data.type) {
            TvType.Movie -> "movies"
            TvType.Anime -> "anime"
            else -> "tv"
        }

        val detailsRes = app.get("$simklApiUrl/$typeStr/${data.ids.slug}?client_id=$clientId&extended=full,episodes").text
        val details = parseJson<SimklFullItem>(detailsRes)

        val poster = details.posters?.maxByOrNull { it.rating ?: 0.0 }?.link
        val background = details.fanarts?.maxByOrNull { it.rating ?: 0.0 }?.link

        val isAnime = details.type == "anime"
        val isAsian = details.country == "jp" || details.country == "kr"

        val uniqueUrl = url

        if (data.type == TvType.Movie) {
            return newMovieLoadResponse(
                name = details.title ?: "Unknown",
                url = url,
                dataUrl = url,
                type = if (isAnime) TvType.AnimeMovie else TvType.Movie,
            ) {
                posterUrl = poster
                backgroundPosterUrl = background
                year = details.year
                plot = details.overview
                score = Score.from10(details.ratings?.simkl ?: 0.0)
                tags = details.genres
                duration = details.runtime
                this.uniqueUrl = uniqueUrl
            }
        }

        val episodes = mutableListOf<Episode>()
        var nextAiring: NextAiring? = null

        details.episodes?.forEach { ep ->
            val epData = SimklEpisodeData(
                slug = data.ids.slug,
                season = ep.season,
                episode = ep.number,
                type = if (isAnime) "anime" else "tv"
            ).toJson()

            episodes.add(
                newEpisode(epData) {
                    name = ep.title
                    season = ep.season
                    episode = ep.number
                    description = ep.overview
                    runTime = ep.runtime
                    posterUrl = ep.image
                    addDate(ep.firstAired, "yyyy-MM-dd")
                    if (nextAiring == null && date != null && date!! > unixTimeMS) {
                        nextAiring = NextAiring(
                            episode = episode!!,
                            unixTime = date!! / 1000,
                            season = if (season == 1) null else season,
                        )
                    }
                }
            )
        }

        return newTvSeriesLoadResponse(
            name = details.title ?: "",
            url = url,
            type = if (isAnime) TvType.Anime else TvType.TvSeries,
            episodes = episodes,
        ) {
            this.episodes = episodes
            this.posterUrl = poster
            this.backgroundPosterUrl = background
            this.plot = details.overview
            this.year = details.year
            this.score = Score.from10(details.ratings?.simkl ?: 0.0)
            this.tags = details.genres
            this.nextAiring = nextAiring
            this.uniqueUrl = uniqueUrl
        }
    }

    private fun SimklSearchItem.toSearchResponse(): SearchResponse? {
        val poster = this.poster
        val mediaType = when (this.type) {
            "movie" -> TvType.Movie
            "anime" -> TvType.Anime
            "show", "tv" -> TvType.TvSeries
            else -> return null
        }

        val data = SimklData(
            type = mediaType,
            ids = SimklIds(slug = this.ids?.slug ?: return null)
        ).toJson()

        return when (mediaType) {
            TvType.Movie -> newMovieSearchResponse(
                name = title ?: "",
                url = data,
                type = TvType.Movie,
            ) {
                posterUrl = poster
                score = Score.from10(ratings?.simkl)
            }

            TvType.Anime -> newTvSeriesSearchResponse(
                name = title ?: "",
                url = data,
                type = TvType.Anime,
            ) {
                posterUrl = poster
                score = Score.from10(ratings?.simkl)
            }

            else -> newTvSeriesSearchResponse(
                name = title ?: "",
                url = data,
                type = TvType.TvSeries,
            ) {
                posterUrl = poster
                score = Score.from10(ratings?.simkl)
            }
        }
    }

    data class SimklData(
        val type: TvType,
        val ids: SimklIds,
    )

    data class SimklIds(
        val slug: String,
    )

    data class SimklSearchItem(
        @JsonProperty("title") val title: String?,
        @JsonProperty("ids") val ids: SimklIds?,
        @JsonProperty("type") val type: String?,
        @JsonProperty("poster") val poster: String?,
        @JsonProperty("ratings") val ratings: SimklRatings?,
    )

    data class SimklRatings(
        @JsonProperty("simkl") val simkl: Double?,
    )

    data class SimklFullItem(
        val title: String?,
        val year: Int?,
        val overview: String?,
        val type: String?,
        val country: String?,
        val genres: List<String>?,
        val runtime: Int?,
        val ratings: SimklRatings?,
        val posters: List<SimklImage>?,
        val fanarts: List<SimklImage>?,
        val episodes: List<SimklEpisode>?,
    )

    data class SimklImage(
        val link: String?,
        val rating: Double?,
    )

    data class SimklEpisode(
        val title: String?,
        val overview: String?,
        @JsonProperty("first_aired") val firstAired: String?,
        val season: Int,
        @JsonProperty("ep") val number: Int,
        val runtime: Int?,
        val image: String?,
    )

    data class SimklEpisodeData(
        val slug: String,
        val season: Int,
        val episode: Int,
        val type: String,
    )
}
