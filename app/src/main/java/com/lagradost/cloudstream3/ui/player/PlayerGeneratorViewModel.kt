package com.lagradost.cloudstream3.ui.player

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.mvvm.launchSafe
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.mvvm.safe
import com.lagradost.cloudstream3.mvvm.safeApiCall
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.videoskip.SkipAPI
import com.lagradost.cloudstream3.utils.videoskip.VideoSkipStamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PlayerGeneratorViewModel : ViewModel() {
    companion object {
        const val TAG = "PlayViewGen"
    }

    private var generator: IGenerator? = null

    private val _currentLinks = MutableLiveData<Set<Pair<ExtractorLink?, ExtractorUri?>>>(setOf())
    val currentLinks: LiveData<Set<Pair<ExtractorLink?, ExtractorUri?>>> = _currentLinks

    private val _currentSubs = MutableLiveData<Set<SubtitleData>>(setOf())
    val currentSubs: LiveData<Set<SubtitleData>> = _currentSubs

    private val _loadingLinks = MutableLiveData<Resource<Boolean?>>()
    val loadingLinks: LiveData<Resource<Boolean?>> = _loadingLinks

    private val _currentStamps = MutableLiveData<List<VideoSkipStamp>>(emptyList())
    val currentStamps: LiveData<List<VideoSkipStamp>> = _currentStamps

    private val _currentSubtitleYear = MutableLiveData<Int?>(null)
    val currentSubtitleYear: LiveData<Int?> = _currentSubtitleYear

    /**
     * Save the Episode ID to prevent starting multiple link loading Jobs when preloading links.
     */
    private var currentLoadingEpisodeId: Int? = null

    var forceClearCache = false

    fun setSubtitleYear(year: Int?) {
        _currentSubtitleYear.postValue(year)
    }

    fun getId(): Int? {
        return generator?.getCurrentId()
    }

    fun loadLinks(episode: Int) {
        generator?.goto(episode)
        loadLinks()
    }

    fun loadLinksPrev() {
        Log.i(TAG, "loadLinksPrev")
        if (generator?.hasPrev() == true) {
            generator?.prev()
            loadLinks()
        }
    }

    fun loadLinksNext() {
        Log.i(TAG, "loadLinksNext")
        if (generator?.hasNext() == true) {
            generator?.next()
            loadLinks()
        }
    }

    fun hasNextEpisode(): Boolean? {
        return generator?.hasNext()
    }

    fun hasPrevEpisode(): Boolean? {
        return generator?.hasPrev()
    }

    fun preLoadNextLinks() {
        val id = getId()
        // Do not preload if already loading
        if (id == currentLoadingEpisodeId) return

        Log.i(TAG, "preLoadNextLinks")
        currentJob?.cancel()
        currentLoadingEpisodeId = id

        currentJob = viewModelScope.launch {
            try {
                if (generator?.hasCache == true && generator?.hasNext() == true) {
                    safeApiCall {
                        generator?.generateLinks(
                            sourceTypes = LOADTYPE_INAPP,
                            clearCache = false,
                            callback = {},
                            subtitleCallback = {},
                            offset = 1
                        )
                    }
                }
            } catch (t: Throwable) {
                logError(t)
            } finally {
                if (currentLoadingEpisodeId == id) {
                    currentLoadingEpisodeId = null
                }
            }
        }
    }

    fun getLoadResponse(): LoadResponse? {
        return safe { (generator as? RepoLinkGenerator?)?.page }
    }

    fun getMeta(): Any? {
        return safe { generator?.getCurrent() }
    }

    fun getAllMeta(): List<Any>? {
        return safe { generator?.getAll() }
    }

    fun getNextMeta(): Any? {
        return safe {
            if (generator?.hasNext() == false) return@safe null
            generator?.getCurrent(offset = 1)
        }
    }

    fun loadThisEpisode(index:Int) {
        generator?.goto(index)
        loadLinks()
    }

    fun getCurrentIndex():Int?{
        val repoGen = generator as? RepoLinkGenerator ?: return null
        return repoGen.videoIndex
    }

    fun attachGenerator(newGenerator: IGenerator?) {
        if (generator == null) {
            generator = newGenerator
        }
    }

    fun getGenerator(): IGenerator? = generator

    private var extraSubtitles : MutableSet<SubtitleData> = mutableSetOf()

    /**
     * If duplicate nothing will happen
     * */
    fun addSubtitles(file: Set<SubtitleData>) = synchronized(extraSubtitles) {
        extraSubtitles += file
        val current = _currentSubs.value ?: emptySet()
        val next = extraSubtitles + current

        // if it is of a different size then we have added distinct items
        if (next.size != current.size) {
            // Posting will refresh subtitles which will in turn
            // make the subs to english if previously unselected
            _currentSubs.postValue(next)
        }
    }

    private var currentJob: Job? = null
    private var currentStampJob: Job? = null

    fun loadStamps(duration: Long) {
        //currentStampJob?.cancel()
        currentStampJob = ioSafe {
            val meta = generator?.getCurrent()
            val page = (generator as? RepoLinkGenerator?)?.page
            if (page != null && meta is ResultEpisode) {
                _currentStamps.postValue(listOf())
                _currentStamps.postValue(
                    SkipAPI.videoStamps(
                        page,
                        meta,
                        duration,
                        hasNextEpisode() ?: false
                    )
                )
            }
        }
    }

    fun loadLinks(sourceTypes: Set<ExtractorLinkType> = LOADTYPE_INAPP) {
        Log.i(TAG, "loadLinks")
        currentJob?.cancel()

        currentJob = viewModelScope.launchSafe {
            // Clear any manually-added subtitles from the previous episode.
            synchronized(extraSubtitles) { extraSubtitles.clear() }

            // Dedicated lock objects so each accumulator has its own monitor.
            // Using the set itself as its own lock (synchronized(currentLinks)) is legal
            // but fragile — a dedicated Any() makes the intent explicit and prevents
            // accidental re-use of the wrong lock (which was the bug for currentSubs,
            // which was previously guarded by synchronized(extraSubtitles) instead).
            val linksLock = Any()
            val subsLock  = Any()
            val currentLinks = mutableSetOf<Pair<ExtractorLink?, ExtractorUri?>>()
            val currentSubs  = mutableSetOf<SubtitleData>()

            // Signal "loading" and wipe the previous episode's data.
            _currentSubs.postValue(emptySet())
            _currentLinks.postValue(emptySet())
            _loadingLinks.postValue(Resource.Loading())

            val loadingState = safeApiCall {
                generator?.generateLinks(
                    sourceTypes = sourceTypes,
                    clearCache = forceClearCache,
                    callback = { pair ->
                        synchronized(linksLock) {
                            if (currentLinks.add(pair)) {
                                safe { _currentLinks.postValue(currentLinks.toSet()) }
                            }
                        }
                    },
                    subtitleCallback = { sub ->
                        synchronized(subsLock) {
                            if (currentSubs.add(sub)) {
                                val extra = synchronized(extraSubtitles) { extraSubtitles.toSet() }
                                safe { _currentSubs.postValue(currentSubs + extra) }
                            }
                        }
                    }
                )
            }

            // Always post the final authoritative snapshot after generateLinks returns.
            // This is necessary for synchronous generators (e.g. DownloadFileGenerator)
            // whose callbacks fire on the same coroutine thread: launchSafe swallows
            // CancellationException but the coroutine will have already populated
            // currentLinks/currentSubs before any cancellation point is reached,
            // so this postValue is always safe and never lost.
            _loadingLinks.postValue(loadingState)
            synchronized(linksLock) { _currentLinks.postValue(currentLinks.toSet()) }
            synchronized(subsLock) {
                val extra = synchronized(extraSubtitles) { extraSubtitles.toSet() }
                _currentSubs.postValue(currentSubs + extra)
            }
        }
    }
}
