package com.lagradost.cloudstream3.syncproviders.providers

import androidx.annotation.StringRes
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.ShowStatus
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.syncproviders.AuthData
import com.lagradost.cloudstream3.syncproviders.AuthLoginPage
import com.lagradost.cloudstream3.syncproviders.AuthToken
import com.lagradost.cloudstream3.syncproviders.AuthUser
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.ui.SyncWatchType
import com.lagradost.cloudstream3.ui.library.ListSorting
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.DataStore.toKotlinObject
import com.lagradost.cloudstream3.utils.txt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/** max 100 via https://myanimelist.net/apiconfig/references/api/v2#tag/anime */
const val MAL_MAX_SEARCH_LIMIT = 25

class MALApi : SyncAPI() {
    override var name = "MAL"
    override val idPrefix = "mal"

    private val key = BuildConfig.MAL_KEY
    private val apiUrl = "https://api.myanimelist.net"
    override val hasOAuth2 = true
    override val redirectUrlIdentifier: String? = "mallogin"
    override val mainUrl = "https://myanimelist.net"
    override val icon = R.drawable.mal_logo
    override val syncIdName = SyncIdName.MyAnimeList
    override val createAccountUrl = "$mainUrl/register.php"

    override val supportedWatchTypes = setOf(
        SyncWatchType.WATCHING,
        SyncWatchType.COMPLETED,
        SyncWatchType.PLANTOWATCH,
        SyncWatchType.DROPPED,
        SyncWatchType.ONHOLD,
        SyncWatchType.NONE
    )

    @Serializable
    data class Payload(
        @SerialName("requestId") val requestId: Int,
        @SerialName("codeVerifier") val codeVerifier: String,
    )

    override suspend fun login(redirectUrl: String, payload: String?): AuthToken? {
        val payloadData = parseJson<PayLoad>(payload!!)
        val sanitizer = splitRedirectUrl(redirectUrl)
        val state = sanitizer["state"]!!

        if (state != "RequestID${payloadData.requestId}") {
            return null
        }

        val currentCode = sanitizer["code"]!!

        val token = app.post(
            "$mainUrl/v1/oauth2/token",
            data = mapOf(
                "client_id" to key,
                "code" to currentCode,
                "code_verifier" to payloadData.codeVerifier,
                "grant_type" to "authorization_code"
            )
        ).parsed<ResponseToken>()
        return AuthToken(
            accessTokenLifetime = APIHolder.unixTime + token.expiresIn.toLong(),
            refreshToken = token.refreshToken,
            accessToken = token.accessToken
        )
    }

    override suspend fun user(token: AuthToken?): AuthUser? {
        val user = app.get(
            "$apiUrl/v2/users/@me",
            headers = mapOf(
                "Authorization" to "Bearer ${token?.accessToken ?: return null}"
            ), cacheTime = 0
        ).parsed<MalUser>()
        return AuthUser(
            id = user.id,
            name = user.name,
            profilePicture = user.picture
        )
    }

    override suspend fun search(auth: AuthData?, query: String): List<SyncAPI.SyncSearchResult>? {
        val auth = auth?.token?.accessToken ?: return null
        val url = "$apiUrl/v2/anime?q=$query&limit=$MAL_MAX_SEARCH_LIMIT"
        val res = app.get(
            url, headers = mapOf(
                "Authorization" to "Bearer $auth",
            ), cacheTime = 0
        ).parsed<MalSearch>()
        return res.data.map {
            val node = it.node
            SyncAPI.SyncSearchResult(
                node.title,
                this.name,
                node.id.toString(),
                "$mainUrl/anime/${node.id}/",
                node.mainPicture?.large ?: node.mainPicture?.medium
            )
        }
    }

    override fun urlToId(url: String): String? =
        Regex("""/anime/((.*)/|(.*))""").find(url)!!.groupValues.first()

    override suspend fun updateStatus(
        auth: AuthData?,
        id: String,
        newStatus: SyncAPI.AbstractSyncStatus
    ): Boolean {
        return setScoreRequest(
            auth?.token ?: return false,
            id.toIntOrNull() ?: return false,
            fromIntToAnimeStatus(newStatus.status),
            newStatus.score?.toInt(10),
            newStatus.watchedEpisodes
        )
    }

    @Serializable
    data class MalAnime(
        @SerialName("id") val id: Int?,
        @SerialName("title") val title: String?,
        @SerialName("main_picture") val mainPicture: MainPicture?,
        @SerialName("alternative_titles") val alternativeTitles: AlternativeTitles?,
        @SerialName("start_date") val startDate: String?,
        @SerialName("end_date") val endDate: String?,
        @SerialName("synopsis") val synopsis: String?,
        @SerialName("mean") val mean: Double?,
        @SerialName("rank") val rank: Int?,
        @SerialName("popularity") val popularity: Int?,
        @SerialName("num_list_users") val numListUsers: Int?,
        @SerialName("num_scoring_users") val numScoringUsers: Int?,
        @SerialName("nsfw") val nsfw: String?,
        @SerialName("created_at") val createdAt: String?,
        @SerialName("updated_at") val updatedAt: String?,
        @SerialName("media_type") val mediaType: String?,
        @SerialName("status") val status: String?,
        @SerialName("genres") val genres: ArrayList<Genres>?,
        @SerialName("my_list_status") val myListStatus: MyListStatus?,
        @SerialName("num_episodes") val numEpisodes: Int?,
        @SerialName("start_season") val startSeason: StartSeason?,
        @SerialName("broadcast") val broadcast: Broadcast?,
        @SerialName("source") val source: String?,
        @SerialName("average_episode_duration") val averageEpisodeDuration: Int?,
        @SerialName("rating") val rating: String?,
        @SerialName("pictures") val pictures: ArrayList<MainPicture>?,
        @SerialName("background") val background: String?,
        @SerialName("related_anime") val relatedAnime: ArrayList<RelatedAnime>?,
        @SerialName("related_manga") val relatedManga: ArrayList<String>?,
        @SerialName("recommendations") val recommendations: ArrayList<Recommendations>?,
        @SerialName("studios") val studios: ArrayList<Studios>?,
        @SerialName("statistics") val statistics: Statistics?,
    )

    @Serializable
    data class Recommendations(
        @SerialName("node") val node: Node? = null,
        @SerialName("num_recommendations") val numRecommendations: Int? = null,
    )

    @Serializable
    data class Studios(
        @SerialName("id") val id: Int? = null,
        @SerialName("name") val name: String? = null,
    )

    @Serializable
    data class MyListStatus(
        @SerialName("status") val status: String? = null,
        @SerialName("score") val score: Int? = null,
        @SerialName("num_episodes_watched") val numEpisodesWatched: Int? = null,
        @SerialName("is_rewatching") val isRewatching: Boolean? = null,
        @SerialName("updated_at") val updatedAt: String? = null,
    )

    @Serializable
    data class RelatedAnime(
        @SerialName("node") val node: Node? = null,
        @SerialName("relation_type") val relationType: String? = null,
        @SerialName("relation_type_formatted") val relationTypeFormatted: String? = null,
    )

    @Serializable
    data class Status(
        @SerialName("watching") val watching: String? = null,
        @SerialName("completed") val completed: String? = null,
        @SerialName("on_hold") val onHold: String? = null,
        @SerialName("dropped") val dropped: String? = null,
        @SerialName("plan_to_watch") val planToWatch: String? = null,
    )

    @Serializable
    data class Statistics(
        @SerialName("status") val status: Status? = null,
        @SerialName("num_list_users") val numListUsers: Int? = null,
    )

    private fun parseDate(string: String?): Long? {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(string ?: return null)?.time
        } catch (e: Exception) {
            null
        }
    }

    private fun toSearchResult(node: Node?): SyncAPI.SyncSearchResult? {
        return SyncAPI.SyncSearchResult(
            name = node?.title ?: return null,
            apiName = this.name,
            syncId = node.id.toString(),
            url = "$mainUrl/anime/${node.id}",
            posterUrl = node.mainPicture?.large
        )
    }

    override suspend fun load(auth: AuthData?, id: String): SyncAPI.SyncResult? {
        val auth = auth?.token?.accessToken ?: return null
        val internalId = id.toIntOrNull() ?: return null
        val url =
            "$apiUrl/v2/anime/$internalId?fields=id,title,main_picture,alternative_titles,start_date,end_date,synopsis,mean,rank,popularity,num_list_users,num_scoring_users,nsfw,created_at,updated_at,media_type,status,genres,my_list_status,num_episodes,start_season,broadcast,source,average_episode_duration,rating,pictures,background,related_anime,related_manga,recommendations,studios,statistics"

        val res = app.get(
            url, headers = mapOf(
                "Authorization" to "Bearer $auth"
            )
        ).text()
        return parseJson<MalAnime>(res).let { malAnime ->
            SyncAPI.SyncResult(
                id = internalId.toString(),
                totalEpisodes = malAnime.numEpisodes,
                title = malAnime.title,
                publicScore = Score.from10(malAnime.mean),
                duration = malAnime.averageEpisodeDuration,
                synopsis = malAnime.synopsis,
                airStatus = when (malAnime.status) {
                    "finished_airing" -> ShowStatus.Completed
                    "currently_airing" -> ShowStatus.Ongoing
                    // "not_yet_aired"
                    else -> null
                },
                nextAiring = null,
                studio = malAnime.studios?.mapNotNull { it.name },
                genres = malAnime.genres?.map { it.name },
                trailers = null,
                startDate = parseDate(malAnime.startDate),
                endDate = parseDate(malAnime.endDate),
                recommendations = malAnime.recommendations?.mapNotNull { rec ->
                    val node = rec.node ?: return@mapNotNull null
                    toSearchResult(node)
                },
                nextSeason = malAnime.relatedAnime?.firstOrNull {
                    return@firstOrNull it.relationType == "sequel"
                }?.let { toSearchResult(it.node) },
                prevSeason = malAnime.relatedAnime?.firstOrNull {
                    return@firstOrNull it.relationType == "prequel"
                }?.let { toSearchResult(it.node) },
                actors = null,
            )
        }
    }

    override suspend fun status(auth: AuthData?, id: String): SyncAPI.AbstractSyncStatus? {
        val auth = auth?.token?.accessToken ?: return null

        // https://myanimelist.net/apiconfig/references/api/v2#operation/anime_anime_id_get
        val url =
            "$apiUrl/v2/anime/$id?fields=id,title,num_episodes,my_list_status"
        val data = app.get(
            url, headers = mapOf(
                "Authorization" to "Bearer $auth"
            ), cacheTime = 0
        ).parsed<SmallMalAnime>().myListStatus

        return SyncAPI.SyncStatus(
            score = Score.from10(data?.score),
            status = SyncWatchType.fromInternalId(malStatusAsString.indexOf(data?.status)),
            isFavorite = null,
            watchedEpisodes = data?.numEpisodesWatched,
        )
    }

    companion object {
        private val malStatusAsString =
            arrayOf("watching", "completed", "on_hold", "dropped", "plan_to_watch")

        const val MAL_CACHED_LIST: String = "mal_cached_list"

        fun convertToStatus(string: String): MalStatusType {
            return when (string) {
                "watching" -> MalStatusType.Watching
                "completed" -> MalStatusType.Completed
                "on_hold" -> MalStatusType.OnHold
                "dropped" -> MalStatusType.Dropped
                "plan_to_watch" -> MalStatusType.PlanToWatch
                else -> MalStatusType.None
            }
        }

        enum class MalStatusType(var value: Int, @StringRes val stringRes: Int) {
            Watching(0, R.string.type_watching),
            Completed(1, R.string.type_completed),
            OnHold(2, R.string.type_on_hold),
            Dropped(3, R.string.type_dropped),
            PlanToWatch(4, R.string.type_plan_to_watch),
            None(-1, R.string.type_none)
        }

        private fun fromIntToAnimeStatus(inp: SyncWatchType): MalStatusType {//= AniListStatusType.values().first { it.value == inp }
            return when (inp) {
                SyncWatchType.NONE -> MalStatusType.None
                SyncWatchType.WATCHING -> MalStatusType.Watching
                SyncWatchType.COMPLETED -> MalStatusType.Completed
                SyncWatchType.ONHOLD -> MalStatusType.OnHold
                SyncWatchType.DROPPED -> MalStatusType.Dropped
                SyncWatchType.PLANTOWATCH -> MalStatusType.PlanToWatch
                SyncWatchType.REWATCHING -> MalStatusType.Watching
            }
        }

        private fun parseDateLong(string: String?): Long? {
            return try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault()).parse(
                    string ?: return null
                )?.time?.div(1000)
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun loginRequest(): AuthLoginPage? {
        val codeVerifier = generateCodeVerifier()
        val requestId = ++requestIdCounter
        val codeChallenge = codeVerifier
        val request = "$mainUrl/v1/oauth2/authorize?response_type=code&client_id=$key&code_challenge=$codeChallenge&state=RequestID$requestId"
        return AuthLoginPage(
            url = request,
            payload = Payload(requestId, codeVerifier).toJson()
        )
    }

    override suspend fun refreshToken(token: AuthToken): AuthToken? {
        val res = app.post(
            "$mainUrl/v1/oauth2/token",
            data = mapOf(
                "client_id" to key,
                "grant_type" to "refresh_token",
                "refresh_token" to token.refreshToken!!
            )
        ).parsed<ResponseToken>()
        return AuthToken(
            accessToken = res.accessToken,
            refreshToken = res.refreshToken,
            accessTokenLifetime = APIHolder.unixTime + res.expiresIn.toLong()
        )
    }

    private var requestIdCounter = 0
    private val allTitles = hashMapOf<Int, MalTitleHolder>()

    @Serializable
    data class MalList(
        @SerialName("data") val data: List<Data>,
        @SerialName("paging") val paging: Paging,
    )

    @Serializable
    data class MainPicture(
        @SerialName("medium") val medium: String,
        @SerialName("large") val large: String,
    )

    @Serializable
    data class Node(
        @SerialName("id") val id: Int,
        @SerialName("title") val title: String,
        @SerialName("main_picture") val mainPicture: MainPicture?,
        @SerialName("alternative_titles") val alternativeTitles: AlternativeTitles?,
        @SerialName("media_type") val mediaType: String?,
        @SerialName("num_episodes") val numEpisodes: Int?,
        @SerialName("status") val status: String?,
        @SerialName("start_date") val startDate: String?,
        @SerialName("end_date") val endDate: String?,
        @SerialName("average_episode_duration") val averageEpisodeDuration: Int?,
        @SerialName("synopsis") val synopsis: String?,
        @SerialName("mean") val mean: Double?,
        @SerialName("genres") val genres: List<Genres>?,
        @SerialName("rank") val rank: Int?,
        @SerialName("popularity") val popularity: Int?,
        @SerialName("num_list_users") val numListUsers: Int?,
        @SerialName("num_favorites") val numFavorites: Int?,
        @SerialName("num_scoring_users") val numScoringUsers: Int?,
        @SerialName("start_season") val startSeason: StartSeason?,
        @SerialName("broadcast") val broadcast: Broadcast?,
        @SerialName("nsfw") val nsfw: String?,
        @SerialName("created_at") val createdAt: String?,
        @SerialName("updated_at") val updatedAt: String?,
    )

    @Serializable
    data class ListStatus(
        @SerialName("status") val status: String?,
        @SerialName("score") val score: Int,
        @SerialName("num_episodes_watched") val numEpisodesWatched: Int,
        @SerialName("is_rewatching") val isRewatching: Boolean,
        @SerialName("updated_at") val updatedAt: String,
    )

    @Serializable
    data class Data(
        @SerialName("node") val node: Node,
        @SerialName("list_status") val listStatus: ListStatus?,
    ) {
        fun toLibraryItem(): SyncAPI.LibraryItem {
            return SyncAPI.LibraryItem(
                this.node.title,
                "https://myanimelist.net/anime/${this.node.id}/",
                this.node.id.toString(),
                this.listStatus?.numEpisodesWatched,
                this.node.numEpisodes,
                Score.from10(this.listStatus?.score),
                parseDateLong(this.listStatus?.updatedAt),
                "MAL",
                TvType.Anime,
                this.node.mainPicture?.large ?: this.node.mainPicture?.medium,
                null,
                null,
                plot = this.node.synopsis,
                releaseDate = if (this.node.startDate == null) null else try {
                    Date.from(
                        Instant.from(
                            DateTimeFormatter.ofPattern(if (this.node.startDate.length == 4) "yyyy" else if (this.node.startDate.length == 7) "yyyy-MM" else "yyyy-MM-dd")
                                .parse(this.node.startDate)
                        )
                    )
                } catch (_: RuntimeException) {
                    null
                }
            )
        }
    }

    @Serializable
    data class Paging(
        @SerialName("next") val next: String?,
    )

    @Serializable
    data class AlternativeTitles(
        @SerialName("synonyms") val synonyms: List<String>,
        @SerialName("en") val en: String,
        @SerialName("ja") val ja: String,
    )

    @Serializable
    data class Genres(
        @SerialName("id") val id: Int,
        @SerialName("name") val name: String,
    )

    @Serializable
    data class StartSeason(
        @SerialName("year") val year: Int,
        @SerialName("season") val season: String,
    )

    @Serializable
    data class Broadcast(
        @SerialName("day_of_the_week") val dayOfTheWeek: String?,
        @SerialName("start_time") val startTime: String?,
    )

    override suspend fun library(auth: AuthData?): LibraryMetadata? {
        val list = getMalAnimeListSmart(auth ?: return null)?.groupBy {
            convertToStatus(it.listStatus?.status ?: "").stringRes
        }?.mapValues { group ->
            group.value.map { it.toLibraryItem() }
        } ?: emptyMap()

        // To fill empty lists when MAL does not return them
        val baseMap =
            MalStatusType.entries.filter { it.value >= 0 }.associate {
                it.stringRes to emptyList<SyncAPI.LibraryItem>()
            }

        return SyncAPI.LibraryMetadata(
            (baseMap + list).map { SyncAPI.LibraryList(txt(it.key), it.value) },
            setOf(
                ListSorting.AlphabeticalA,
                ListSorting.AlphabeticalZ,
                ListSorting.UpdatedNew,
                ListSorting.UpdatedOld,
                ListSorting.ReleaseDateNew,
                ListSorting.ReleaseDateOld,
                ListSorting.RatingHigh,
                ListSorting.RatingLow,
            )
        )
    }

    private suspend fun getMalAnimeListSmart(auth: AuthData): Array<Data>? {
        return if (requireLibraryRefresh) {
            val list = getMalAnimeList(auth.token)
            setKey(MAL_CACHED_LIST, auth.user.id.toString(), list)
            list
        } else {
            getKey<Array<Data>>(MAL_CACHED_LIST, auth.user.id.toString()) as? Array<Data>
        }
    }

    private suspend fun getMalAnimeList(token: AuthToken): Array<Data> {
        var offset = 0
        val fullList = mutableListOf<Data>()
        val offsetRegex = Regex("""offset=(\d+)""")
        while (true) {
            val data: MalList = getMalAnimeListSlice(token, offset) ?: break
            fullList.addAll(data.data)
            offset =
                data.paging.next?.let { offsetRegex.find(it)?.groupValues?.get(1)?.toInt() }
                    ?: break
        }
        return fullList.toTypedArray()
    }

    private suspend fun getMalAnimeListSlice(token: AuthToken, offset: Int = 0): MalList? {
        val user = "@me"
        // Very lackluster docs
        // https://myanimelist.net/apiconfig/references/api/v2#operation/users_user_id_animelist_get
        val url =
            "$apiUrl/v2/users/$user/animelist?fields=list_status,num_episodes,media_type,status,start_date,end_date,synopsis,alternative_titles,mean,genres,rank,num_list_users,nsfw,average_episode_duration,num_favorites,popularity,num_scoring_users,start_season,favorites_info,broadcast,created_at,updated_at&nsfw=1&limit=100&offset=$offset"
        val res = app.get(
            url, headers = mapOf(
                "Authorization" to "Bearer ${token.accessToken}",
            ), cacheTime = 0
        ).text()
        return res.toKotlinObject()
    }

    private suspend fun setScoreRequest(
        token: AuthToken,
        id: Int,
        status: MalStatusType? = null,
        score: Int? = null,
        numWatchedEpisodes: Int? = null,
    ): Boolean {
        val res = setScoreRequest(
            token,
            id,
            if (status == null) null else malStatusAsString[maxOf(0, status.value)],
            score,
            numWatchedEpisodes
        )

        return if (res.isNullOrBlank()) {
            false
        } else {
            val malStatus = parseJson<MalStatus>(res)
            if (allTitles.containsKey(id)) {
                val currentTitle = allTitles[id]!!
                allTitles[id] = MalTitleHolder(malStatus, id, currentTitle.name)
            } else {
                allTitles[id] = MalTitleHolder(malStatus, id, "")
            }
            true
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun setScoreRequest(
        token: AuthToken,
        id: Int,
        status: String? = null,
        score: Int? = null,
        numWatchedEpisodes: Int? = null,
    ): String? {
        val data = mapOf(
            "status" to status,
            "score" to score?.toString(),
            "num_watched_episodes" to numWatchedEpisodes?.toString()
        ).filterValues { it != null } as Map<String, String>

        return app.put(
            "$apiUrl/v2/anime/$id/my_list_status",
            headers = mapOf(
                "Authorization" to "Bearer ${token.accessToken}"
            ),
            data = data
        ).text()
    }

    @Serializable
    data class ResponseToken(
        @SerialName("token_type") val tokenType: String,
        @SerialName("expires_in") val expiresIn: Int,
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String,
    )

    @Serializable
    data class MalRoot(
        @SerialName("data") val data: List<MalDatum>,
    )

    @Serializable
    data class MalDatum(
        @SerialName("node") val node: MalNode,
        @SerialName("list_status") val listStatus: MalStatus,
    )

    @Serializable
    data class MalNode(
        @SerialName("id") val id: Int,
        @SerialName("title") val title: String,
    )

    @Serializable
    data class MalStatus(
        @SerialName("status") val status: String,
        @SerialName("score") val score: Int,
        @SerialName("num_episodes_watched") val numEpisodesWatched: Int,
        @SerialName("is_rewatching") val isRewatching: Boolean,
        @SerialName("updated_at") val updatedAt: String,
    )

    @Serializable
    data class MalUser(
        @SerialName("id") val id: Int,
        @SerialName("name") val name: String,
        @SerialName("location") val location: String,
        @SerialName("joined_at") val joinedAt: String,
        @SerialName("picture") val picture: String?,
    )

    @Serializable
    data class MalMainPicture(
        @SerialName("large") val large: String?,
        @SerialName("medium") val medium: String?,
    )

    // Used for getDataAboutId()
    @Serializable
    data class SmallMalAnime(
        @SerialName("id") val id: Int,
        @SerialName("title") val title: String?,
        @SerialName("num_episodes") val numEpisodes: Int,
        @SerialName("my_list_status") val myListStatus: MalStatus?,
        @SerialName("main_picture") val mainPicture: MalMainPicture?,
    )

    @Serializable
    data class MalSearchNode(
        @SerialName("node") val node: Node,
    )

    @Serializable
    data class MalSearch(
        @SerialName("data") val data: List<MalSearchNode>,
        // paging
    )

    data class MalTitleHolder(
        val status: MalStatus,
        val id: Int,
        val name: String,
    )
}
