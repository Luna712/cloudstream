package com.lagradost.cloudstream3.ui.player

import android.util.Log
import com.lagradost.cloudstream3.APIHolder.getApiFromNameNull
import com.lagradost.cloudstream3.APIHolder.unixTime
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.ui.APIRepository
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.utils.AppContextUtils.html
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import kotlin.math.max
import kotlin.math.min

data class Cache(
    val linkCache: MutableSet<ExtractorLink>,
    val subtitleCache: MutableSet<SubtitleData>,
    /** When it was last updated */
    var lastCachedTimestamp: Long = unixTime,
    /** If it has fully loaded */
    var saturated: Boolean,
)

class RepoLinkGenerator(
    episodes: List<ResultEpisode>,
    currentIndex: Int = 0,
    val page: LoadResponse? = null,
) : VideoGenerator<ResultEpisode>(episodes, currentIndex) {
    companion object {
        const val TAG = "RepoLink"
        val cache: HashMap<Pair<String, Int>, Cache> =
            hashMapOf()
    }

    override val hasCache = true
    override val canSkipLoading = true

    // this is a simple array that is used to instantly load links if they are already loaded
    //var linkCache = Array<Set<ExtractorLink>>(size = episodes.size, init = { setOf() })
    //var subsCache = Array<Set<SubtitleData>>(size = episodes.size, init = { setOf() })

    @Throws
    override suspend fun generateLinks(
        clearCache: Boolean,
        sourceTypes: Set<ExtractorLinkType>,
        callback: (Pair<ExtractorLink?, ExtractorUri?>) -> Unit,
        subtitleCallback: (SubtitleData) -> Unit,
        offset: Int,
        isCasting: Boolean,
    ): Boolean {
        val current = getCurrent(offset) ?: return false

        val currentCache = synchronized(cache) {
            cache[current.apiName to current.id] ?: Cache(
                mutableSetOf(),
                mutableSetOf(),
                unixTime,
                false
            ).also {
                cache[current.apiName to current.id] = it
            }
        }

        // These dedup sets are accessed from concurrent plugin callbacks, so they must live
        // inside the same lock as currentCache to prevent TOCTOU races.  All reads and writes
        // to currentLinksUrls / currentSubsUrls / lastCountedSuffix go through
        // synchronized(currentCache) below.
        val currentLinksUrls = mutableSetOf<String>()
        val currentSubsUrls  = mutableSetOf<String>()
        val lastCountedSuffix = mutableMapOf<String, UInt>()

        synchronized(currentCache) {
            val outdatedCache =
                unixTime - currentCache.lastCachedTimestamp > 60 * 20 // 20 minutes

            if (outdatedCache || clearCache) {
                currentCache.linkCache.clear()
                currentCache.subtitleCache.clear()
                currentCache.saturated = false
            } else if (currentCache.linkCache.isNotEmpty()) {
                Log.d(TAG, "Resumed previous loading from ${unixTime - currentCache.lastCachedTimestamp}s ago")
            }

            // Replay already-cached entries to the caller and seed the dedup sets so
            // newly-arriving callbacks can't re-add the same items.
            currentCache.linkCache.forEach { link ->
                currentLinksUrls.add(link.url)
                if (sourceTypes.contains(link.type)) {
                    callback(link to null)
                }
            }

            currentCache.subtitleCache.forEach { sub ->
                currentSubsUrls.add(sub.url)
                val suffixCount = lastCountedSuffix.getOrDefault(sub.originalName, 0u) + 1u
                lastCountedSuffix[sub.originalName] = suffixCount
                subtitleCallback(sub)
            }

            // Short-circuit: all links are already cached, no network request needed.
            if (currentCache.saturated) {
                return true
            }
        }

        val result = APIRepository(
            getApiFromNameNull(current.apiName) ?: throw Exception("This provider does not exist")
        ).loadLinks(
            current.data,
            isCasting = isCasting,
            subtitleCallback = { file ->
                Log.d(TAG, "Loaded SubtitleFile: $file")
                val correctFile = PlayerSubtitleHelper.getSubtitleData(file)

                // Decode the name outside the lock (pure CPU work, no shared state).
                val nameDecoded = correctFile.originalName.html().toString().trim() // `%3Ch1%3Esub%20name…` → `sub name…`

                synchronized(currentCache) {
                    // Check + update the dedup set atomically with the cache write.
                    if (correctFile.url.isBlank() || !currentSubsUrls.add(correctFile.url)) {
                        return@loadLinks
                    }

                    val suffixCount = lastCountedSuffix.getOrDefault(nameDecoded, 0u) + 1u
                    lastCountedSuffix[nameDecoded] = suffixCount

                    val updatedFile = correctFile.copy(
                        originalName = nameDecoded,
                        nameSuffix   = "$suffixCount"
                    )

                    if (currentCache.subtitleCache.add(updatedFile)) {
                        subtitleCallback(updatedFile)
                        currentCache.lastCachedTimestamp = unixTime
                    }
                }
            },
            callback = { link ->
                Log.d(TAG, "Loaded ExtractorLink: $link")
                synchronized(currentCache) {
                    // Check + update the dedup set atomically with the cache write.
                    if (link.url.isBlank() || !currentLinksUrls.add(link.url)) {
                        return@loadLinks
                    }

                    if (currentCache.linkCache.add(link)) {
                        if (sourceTypes.contains(link.type)) {
                            callback(Pair(link, null))
                        }
                        currentCache.lastCachedTimestamp = unixTime
                    }
                }
            }
        )

        synchronized(currentCache) {
            currentCache.saturated = currentCache.linkCache.isNotEmpty()
            currentCache.lastCachedTimestamp = unixTime
        }

        return result
    }
}