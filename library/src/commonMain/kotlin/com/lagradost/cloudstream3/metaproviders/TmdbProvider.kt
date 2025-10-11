package com.lagradost.cloudstream3.metaproviders

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.api.Log
import com.lagradost.cloudstream3.Actor
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addImdbId
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.MovieLoadResponse
import com.lagradost.cloudstream3.MovieSearchResponse
import com.lagradost.cloudstream3.ProviderType
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.SearchResponseList
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.TvSeriesSearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.runAllAsync
import com.lagradost.cloudstream3.toNewSearchResponseList
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.BaseMovie
import com.uwetrottmann.tmdb2.entities.BaseTvShow
import com.uwetrottmann.tmdb2.entities.CastMember
import com.uwetrottmann.tmdb2.entities.ContentRating
import com.uwetrottmann.tmdb2.entities.Movie
import com.uwetrottmann.tmdb2.entities.ReleaseDate
import com.uwetrottmann.tmdb2.entities.ReleaseDatesResult
import com.uwetrottmann.tmdb2.entities.TvSeason
import com.uwetrottmann.tmdb2.entities.TvShow
import com.uwetrottmann.tmdb2.entities.Videos
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.tmdb2.enumerations.VideoType
import retrofit2.awaitResponse
import java.util.Calendar

data class TmdbLink(
    @JsonProperty("imdbID") val imdbID: String?,
    @JsonProperty("tmdbID") val tmdbID: Int?,
    @JsonProperty("episode") val episode: Int?,
    @JsonProperty("season") val season: Int?,
    @JsonProperty("movieName") val movieName: String? = null
)

open class TmdbProvider : MainAPI() {
    open val includeAdult = false
    open val useMetaLoadResponse = false
    open val apiName = "TMDB"
    open val disableSeasonZero = true

    override val hasMainPage = true
    override val providerType = ProviderType.MetaProvider

    private val tmdb = Tmdb("e6333b32409e02a4a6eba6fb7ff866bb")

    private fun getImageUrl(link: String?): String? {
        if (link == null) return null
        return if (link.startsWith("/")) "https://image.tmdb.org/t/p/w500/$link" else link
    }

    private fun getUrl(id: Int?, tvShow: Boolean): String {
        return if (tvShow) "https://www.themoviedb.org/tv/${id ?: -1}"
        else "https://www.themoviedb.org/movie/${id ?: -1}"
    }

    private fun BaseTvShow.toSearchResponse(): TvSeriesSearchResponse {
        return newTvSeriesSearchResponse(
            name = this.name ?: this.original_name,
            url = getUrl(id, true),
            type = TvType.TvSeries,
            fix = false
        ) {
            this.id = this@toSearchResponse.id
            this.posterUrl = getImageUrl(poster_path)
            this.score = Score.from10(vote_average)
            this.year = first_air_date?.let {
                Calendar.getInstance().apply { time = it }.get(Calendar.YEAR)
            }
        }
    }

    private fun BaseMovie.toSearchResponse(): MovieSearchResponse {
        return newMovieSearchResponse(
            name = this.title ?: this.original_title,
            url = getUrl(id, false),
            type = TvType.Movie,
            fix = false
        ) {
            this.id = this@toSearchResponse.id
            this.posterUrl = getImageUrl(poster_path)
            this.score = Score.from10(vote_average)
            this.year = release_date?.let {
                Calendar.getInstance().apply { time = it }.get(Calendar.YEAR)
            }
        }
    }

    private fun List<CastMember?>?.toActors(): List<Pair<Actor, String?>>? {
        return this?.mapNotNull {
            Pair(
                Actor(it?.name ?: return@mapNotNull null, getImageUrl(it.profile_path)),
                it.character
            )
        }
    }

    private suspend fun TvShow.toLoadResponse(): TvSeriesLoadResponse {
        val tvSeasonsService = tmdb.tvSeasonsService()
        Log.d("TMDB", "Building TvShow load response for: ${this.name}")

        val episodes = mutableListOf<Episode>()

        val validSeasons = this.seasons?.filter { !disableSeasonZero || (it.season_number ?: 0) != 0 } ?: emptyList()
        for (season in validSeasons) {
            val seasonNumber = season.season_number ?: continue

            val response: retrofit2.Response<com.uwetrottmann.tmdb2.entities.TvSeason> =
                try {
                    tvSeasonsService.season(this.id, seasonNumber, "external_ids,images,episodes").awaitResponse()
                } catch (e: Exception) {
                    Log.e("TMDB", "Failed to load season $seasonNumber for ${this.name}: ${e.message}")
                    continue
                }

            val fullSeason = response.body() ?: continue
            Log.d("TMDB", "Fetched season $seasonNumber with ${fullSeason.episodes?.size ?: 0} episodes")

            fullSeason.episodes?.forEach { episode ->
                episodes += newEpisode(
                    TmdbLink(
                        episode.external_ids?.imdb_id ?: this.external_ids?.imdb_id,
                        this.id,
                        episode.episode_number,
                        episode.season_number,
                        this.name ?: this.original_name
                    ).toJson()
                ) {
                    this.name = episode.name
                    this.season = episode.season_number
                    this.episode = episode.episode_number
                    this.score = Score.from10(episode.vote_average)
                    this.description = episode.overview
                    this.date = episode.air_date?.time
                    this.posterUrl = getImageUrl(episode.still_path)
                    Log.d("TMDB", "Episode S${episode.season_number}E${episode.episode_number}: ${episode.name}")
                }
            }
        }

        return newTvSeriesLoadResponse(
            this.name ?: this.original_name,
            getUrl(id, true),
            TvType.TvSeries,
            episodes
        ) {
            posterUrl = getImageUrl(poster_path)
            year = first_air_date?.let { Calendar.getInstance().apply { time = it }.get(Calendar.YEAR) }
            plot = overview
            addImdbId(external_ids?.imdb_id)
            tags = genres?.mapNotNull { it.name }
            duration = episode_run_time?.average()?.toInt()
            score = Score.from10(vote_average)
            addTrailer(videos.toTrailers())
            recommendations = (this@toLoadResponse.recommendations ?: this@toLoadResponse.similar)
                ?.results?.map { it.toSearchResponse() }
            addActors(credits?.cast?.toList().toActors())
            contentRating = fetchContentRating(id, "US")
        }
    }

    private fun Videos?.toTrailers(): List<String>? {
        return this?.results
            ?.filter { it.type != VideoType.OPENING_CREDITS && it.type != VideoType.FEATURETTE }
            ?.sortedBy { it.type?.ordinal ?: 10000 }
            ?.mapNotNull {
                when (it.site?.trim()?.lowercase()) {
                    "youtube" -> "https://www.youtube.com/watch?v=${it.key}"
                    else -> null
                }
            }
    }

    private suspend fun Movie.toLoadResponse(): MovieLoadResponse {
        return newMovieLoadResponse(
            this.title ?: this.original_title,
            getUrl(id, false),
            TvType.Movie,
            TmdbLink(
                this.imdb_id,
                this.id,
                null,
                null,
                this.title ?: this.original_title
            ).toJson()
        ) {
            posterUrl = getImageUrl(poster_path)
            year = release_date?.let {
                Calendar.getInstance().apply { time = it }.get(Calendar.YEAR)
            }
            plot = overview
            addImdbId(external_ids?.imdb_id)
            tags = genres?.mapNotNull { it.name }
            duration = runtime
            score = Score.from10(vote_average)
            addTrailer(videos.toTrailers())

            recommendations = (this@toLoadResponse.recommendations
                ?: this@toLoadResponse.similar)?.results?.map { it.toSearchResponse() }
            addActors(credits?.cast?.toList().toActors())
            contentRating = fetchContentRating(id, "US")
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        var discoverMovies: List<MovieSearchResponse> = listOf()
        var discoverSeries: List<TvSeriesSearchResponse> = listOf()
        var topMovies: List<MovieSearchResponse> = listOf()
        var topSeries: List<TvSeriesSearchResponse> = listOf()

        runAllAsync(
            {
                discoverMovies = tmdb.discoverMovie().page(page).build().awaitResponse().body()?.results?.map {
                    it.toSearchResponse()
                } ?: listOf()
            },
            {
                discoverSeries = tmdb.discoverTv().page(page).build().awaitResponse().body()?.results?.map {
                    it.toSearchResponse()
                } ?: listOf()
            },
            {
                topMovies = tmdb.moviesService().topRated(page, "en-US", "US").awaitResponse().body()?.results?.map {
                    it.toSearchResponse()
                } ?: listOf()
            },
            {
                topSeries = tmdb.tvService().topRated(page, "en-US").awaitResponse().body()?.results?.map {
                    it.toSearchResponse()
                } ?: listOf()
            }
        )

        return newHomePageResponse(
            listOf(
                HomePageList("Popular Movies", discoverMovies),
                HomePageList("Popular Series", discoverSeries),
                HomePageList("Top Movies", topMovies),
                HomePageList("Top Series", topSeries)
            )
        )
    }

    open fun loadFromImdb(imdb: String, seasons: List<TvSeason>): LoadResponse? = null
    open fun loadFromTmdb(tmdb: Int, seasons: List<TvSeason>): LoadResponse? = null
    open fun loadFromImdb(imdb: String): LoadResponse? = null
    open fun loadFromTmdb(tmdb: Int): LoadResponse? = null

    open suspend fun fetchContentRating(id: Int?, country: String): String? {
        id ?: return null
        val contentRatings = tmdb.tvService().content_ratings(id).awaitResponse().body()?.results
        return if (!contentRatings.isNullOrEmpty()) {
            contentRatings.firstOrNull { it.iso_3166_1 == country }?.rating
        } else {
            val releaseDates = tmdb.moviesService().releaseDates(id).awaitResponse().body()?.results
            releaseDates?.firstOrNull { it.iso_3166_1 == country }
                ?.release_dates?.firstOrNull { !it.certification.isNullOrBlank() }?.certification
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val idRegex = Regex("""themoviedb\.org/(.*)/(\d+)""")
        val found = idRegex.find(url)
        val isTvSeries = found?.groupValues?.getOrNull(1).equals("tv", ignoreCase = true)
        val id = found?.groupValues?.getOrNull(2)?.toIntOrNull()
            ?: throw ErrorLoadingException("No id found")

        return if (useMetaLoadResponse) {
            if (isTvSeries) {
                val body = tmdb.tvService()
                    .tv(
                        id,
                        "en-US",
                        AppendToResponse(
                            AppendToResponseItem.EXTERNAL_IDS,
                            AppendToResponseItem.VIDEOS
                        )
                    ).awaitResponse().body()

                val response = body?.toLoadResponse()
                if (response != null) {
                    if (response.recommendations.isNullOrEmpty()) {
                        tmdb.tvService().recommendations(id, 1, "en-US").awaitResponse().body()
                            ?.results?.map { it.toSearchResponse() }?.let {
                                response.recommendations = it
                            }
                    }

                    if (response.actors.isNullOrEmpty()) {
                        tmdb.tvService().credits(id, "en-US").awaitResponse().body()?.let {
                            response.addActors(it.cast?.toActors())
                        }
                    }
                }
                response
            } else {
                val body = tmdb.moviesService()
                    .summary(
                        id,
                        "en-US",
                        AppendToResponse(
                            AppendToResponseItem.EXTERNAL_IDS,
                            AppendToResponseItem.VIDEOS
                        )
                    ).awaitResponse().body()

                val response = body?.toLoadResponse()
                if (response != null) {
                    if (response.recommendations.isNullOrEmpty()) {
                        tmdb.moviesService().recommendations(id, 1, "en-US").awaitResponse().body()
                            ?.results?.map { it.toSearchResponse() }?.let {
                                response.recommendations = it
                            }
                    }

                    if (response.actors.isNullOrEmpty()) {
                        tmdb.moviesService().credits(id).awaitResponse().body()?.let {
                            response.addActors(it.cast?.toActors())
                        }
                    }
                }
                response
            }
        } else {
            loadFromTmdb(id)?.let { return it }
            if (isTvSeries) {
                tmdb.tvService().externalIds(id).awaitResponse().body()?.imdb_id?.let {
                    val fromImdb = loadFromImdb(it)
                    val result = if (fromImdb == null) {
                        val details = tmdb.tvService().tv(id, "en-US").awaitResponse().body()
                        loadFromImdb(it, details?.seasons ?: listOf())
                            ?: loadFromTmdb(id, details?.seasons ?: listOf())
                    } else fromImdb
                    result
                }
            } else {
                tmdb.moviesService().externalIds(id).awaitResponse().body()?.imdb_id?.let {
                    loadFromImdb(it)
                }
            }
        }
    }

    override suspend fun search(query: String, page: Int): SearchResponseList? {
        return tmdb.searchService().multi(query, page, "en-US", "US", includeAdult).awaitResponse()
            .body()?.results?.mapNotNull {
                it.movie?.toSearchResponse() ?: it.tvShow?.toSearchResponse()
            }?.toNewSearchResponseList()
    }
}
