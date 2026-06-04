package com.lagradost.cloudstream3.utils

// TODO: FIX

import android.util.Log
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.lagradost.cloudstream3.APIHolder.apis
//import com.lagradost.cloudstream3.animeproviders.AniflixProvider
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import java.util.concurrent.TimeUnit

object SyncUtil {
    private val regexs = listOf(
        Regex("""(9anime)\.(?:to|center|id)/watch/.*?\.([^/?]*)"""),
        Regex("""(gogoanime|gogoanimes)\..*?/category/([^/?]*)"""),
        Regex("""(twist\.moe)/a/([^/?]*)"""),
    )

    private const val TAG = "SYNCUTIL"

    private const val GOGOANIME = "Gogoanime"
    private const val NINE_ANIME = "9anime"
    private const val TWIST_MOE = "Twistmoe"

    private val matchList =
        mapOf(
            "9anime" to NINE_ANIME,
            "gogoanime" to GOGOANIME,
            "gogoanimes" to GOGOANIME,
            "twist.moe" to TWIST_MOE
        )

    suspend fun getIdsFromUrl(url: String?): Pair<String?, String?>? {
        if (url == null) return null
        Log.i(TAG, "getIdsFromUrl $url")

        for (regex in regexs) {
            regex.find(url)?.let { match ->
                if (match.groupValues.size == 3) {
                    val site = match.groupValues[1]
                    val slug = match.groupValues[2]
                    matchList[site]?.let { realSite ->
                        getIdsFromSlug(slug, realSite)?.let {
                            return it
                        } ?: kotlin.run {
                            if (slug.endsWith("-dub")) {
                                println("testing non -dub slug $slug")
                                getIdsFromSlug(slug.removeSuffix("-dub"), realSite)?.let {
                                    return it
                                }
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    /** first. Mal, second. Anilist,
     * valid sites are: Gogoanime, Twistmoe and 9anime*/
    private suspend fun getIdsFromSlug(
        slug: String,
        site: String = "Gogoanime"
    ): Pair<String?, String?>? {
        Log.i(TAG, "getIdsFromSlug $slug $site")
        try {
            //Gogoanime, Twistmoe and 9anime
            val url =
                "https://raw.githubusercontent.com/MALSync/MAL-Sync-Backup/master/data/pages/$site/$slug.json"
            val response = app.get(url, cacheTime = 1, cacheUnit = TimeUnit.DAYS).text()
            val mapped = tryParseJson<MalSyncPage?>(response)

            val overrideMal = response?.malId ?: response?.mal?.id ?: response?.anilist?.malId
            val overrideAnilist = response?.aniId ?: response?.anilist?.id

            if (overrideMal != null) {
                return overrideMal.toString() to overrideAnilist?.toString()
            }
            return null
        } catch (e: Exception) {
            logError(e)
        }
        return null
    }

    suspend fun getUrlsFromId(id: String, type: String = "anilist"): List<String> {
        val url =
            "https://raw.githubusercontent.com/MALSync/MAL-Sync-Backup/master/data/$type/anime/$id.json"
        val response = app.get(url, cacheTime = 1, cacheUnit = TimeUnit.DAYS).parsed<SyncPage>()
        val pages = response.pages ?: return emptyList()
        val current =
            pages.gogoanime.values.union(pages.nineanime.values).union(pages.twistmoe.values)
                .mapNotNull { it.url }.toMutableList()

        if (type == "anilist") { // TODO MAKE BETTER
            apis.filter { it.name.contains("Aniflix", ignoreCase = true) }.forEach {
                current.add("${it.mainUrl}/anime/$id")
            }
        }
        return current
    }

    @Serializable
    data class SyncPage(
        @SerialName("Pages") val pages: SyncPages?,
    )

    @Serializable
    data class SyncPages(
        @SerialName("9anime") val nineanime: Map<String, ProviderPage> = emptyMap(),
        @SerialName("Gogoanime") val gogoanime: Map<String, ProviderPage> = emptyMap(),
        @SerialName("Twistmoe") val twistmoe: Map<String, ProviderPage> = emptyMap(),
    )

    @Serializable
    data class ProviderPage(
        @SerialName("url") val url: String?,
    )

    @Serializable
    data class MalSyncPage(
        @SerialName("identifier") val identifier: String?,
        @SerialName("type") val type: String?,
        @SerialName("page") val page: String?,
        @SerialName("title") val title: String?,
        @SerialName("url") val url: String?,
        @SerialName("image") val image: String?,
        @SerialName("hentai") val hentai: Boolean?,
        @SerialName("sticky") val sticky: Boolean?,
        @SerialName("active") val active: Boolean?,
        @SerialName("actor") val actor: String?,
        @SerialName("malId") val malId: Int?,
        @SerialName("aniId") val aniId: Int?,
        @SerialName("createdAt") val createdAt: String?,
        @SerialName("updatedAt") val updatedAt: String?,
        @SerialName("deletedAt") val deletedAt: String?,
        @SerialName("Mal") val mal: Mal?,
        @SerialName("Anilist") val anilist: Anilist?,
        @SerialName("malUrl") val malUrl: String?
    )

    @Serializable
    data class Anilist(
//            @SerialName("altTitle") val altTitle: List<String>?,
//            @SerialName("externalLinks") val externalLinks: List<String>?,
        @SerialName("id") val id: Int?,
        @SerialName("malId") val malId: Int?,
        @SerialName("type") val type: String?,
        @SerialName("title") val title: String?,
        @SerialName("url") val url: String?,
        @SerialName("image") val image: String?,
        @SerialName("category") val category: String?,
        @SerialName("hentai") val hentai: Boolean?,
        @SerialName("createdAt") val createdAt: String?,
        @SerialName("updatedAt") val updatedAt: String?,
        @SerialName("deletedAt") val deletedAt: String?
    )

    @Serializable
    data class Mal(
//            @SerialName("altTitle") val altTitle: List<String>?,
        @SerialName("id") val id: Int?,
        @SerialName("type") val type: String?,
        @SerialName("title") val title: String?,
        @SerialName("url") val url: String?,
        @SerialName("image") val image: String?,
        @SerialName("category") val category: String?,
        @SerialName("hentai") val hentai: Boolean?,
        @SerialName("createdAt") val createdAt: String?,
        @SerialName("updatedAt") val updatedAt: String?,
        @SerialName("deletedAt") val deletedAt: String?
    )
}
