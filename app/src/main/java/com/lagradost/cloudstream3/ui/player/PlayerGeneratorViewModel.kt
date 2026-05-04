package com.lagradost.cloudstream3.ui.player

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.mvvm.launchSafe
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.mvvm.safe
import com.lagradost.cloudstream3.mvvm.safeApiCall
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.Coroutines.ioSafe
import com.lagradost.cloudstream3.utils.DataStore.mapper as jacksonMapper
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.videoskip.SkipAPI
import com.lagradost.cloudstream3.utils.videoskip.VideoSkipStamp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PlayerGeneratorViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    companion object {
        const val TAG = "PlayerGeneratorViewModel"

        // Keys shared with GeneratorPlayer.newInstance() so the arguments Bundle seeds
        // SavedStateHandle on first creation, and SavedStateHandle auto-restores them after
        // process death without any manual Bundle read/write in the Fragment.
        const val KEY_GEN_TYPE     = "generatorType"
        const val KEY_GEN_EPISODES = "generatorEpisodes"
        const val KEY_GEN_INDEX    = "generatorIndex"
        const val KEY_GEN_PAGE     = "generatorPage"
        const val KEY_GEN_PAGE_CLS = "generatorPageClass"
        const val KEY_GEN_LINKS    = "generatorLinks"
        const val KEY_GEN_EXTRACT  = "generatorExtract"
        const val KEY_GEN_REFERER  = "generatorReferer"
        const val KEY_GEN_URIS     = "generatorUris"

        const val GEN_TYPE_REPO     = "repo"
        const val GEN_TYPE_LINK     = "link"
        const val GEN_TYPE_DOWNLOAD = "download"

        // Keys for the currently-selected source and subtitle. Written here whenever the
        // Fragment selects a source; auto-restored on process death via SavedStateHandle.
        const val KEY_SEL_LINK_JSON   = "selectedLinkJson"
        const val KEY_SEL_LINK_CLS    = "selectedLinkClass"
        const val KEY_SEL_LINK_IS_URI = "selectedLinkIsUri"
        const val KEY_SEL_URI_JSON    = "selectedUriJson"
        const val KEY_SEL_SUB_JSON    = "selectedSubJson"
    }

    // Generator is restored from SavedStateHandle on process death, set via attachGenerator().
    private var generator: IGenerator? = null

    init {
        // On process death Android restores the Fragment's savedInstanceState into
        // SavedStateHandle automatically. The arguments bundle is also merged in on first
        // creation. Either way, the generator keys are available here without any Fragment
        // intervention.
        generator = generatorFromHandle()
    }

    private fun generatorFromHandle(): IGenerator? = try {
        when (savedStateHandle.get<String>(KEY_GEN_TYPE)) {
            GEN_TYPE_REPO -> {
                val episodesJson = savedStateHandle.get<String>(KEY_GEN_EPISODES) ?: return null
                val episodes: List<ResultEpisode> = AppUtils.parseJson(episodesJson)
                val index = savedStateHandle.get<Int>(KEY_GEN_INDEX) ?: 0
                val page: LoadResponse? = run {
                    val json      = savedStateHandle.get<String>(KEY_GEN_PAGE) ?: return@run null
                    val className = savedStateHandle.get<String>(KEY_GEN_PAGE_CLS) ?: return@run null
                    try {
                        jacksonMapper.readValue(json, Class.forName(className)) as? LoadResponse
                    } catch (t: Throwable) {
                        Log.w(TAG, "Could not deserialize LoadResponse page ($className)", t)
                        null
                    }
                }
                RepoLinkGenerator(episodes, index, page)
            }
            GEN_TYPE_LINK -> {
                val linksJson = savedStateHandle.get<String>(KEY_GEN_LINKS) ?: return null
                val links: List<BasicLink> = AppUtils.parseJson(linksJson)
                val extract = savedStateHandle.get<Boolean>(KEY_GEN_EXTRACT) ?: true
                val referer = savedStateHandle.get<String>(KEY_GEN_REFERER)
                LinkGenerator(links, extract, referer)
            }
            GEN_TYPE_DOWNLOAD -> {
                val urisJson = savedStateHandle.get<String>(KEY_GEN_URIS) ?: return null
                val uris: List<ExtractorUri> = AppUtils.parseJson(urisJson)
                val index = savedStateHandle.get<Int>(KEY_GEN_INDEX) ?: 0
                DownloadFileGenerator(uris, index)
            }
            else -> null
        }
    } catch (t: Throwable) {
        Log.e(TAG, "Failed to restore generator from SavedStateHandle", t)
        null
    }

    /** Called by the Fragment when it first navigates to the player with a new generator.
     *  Persists all generator state into SavedStateHandle so it survives process death. */
    fun attachGenerator(newGenerator: IGenerator?) {
        if (generator != null || newGenerator == null) return
        generator = newGenerator
        // Persist to SavedStateHandle so process-death restore works without Fragment involvement.
        try {
            when (newGenerator) {
                is RepoLinkGenerator -> {
                    savedStateHandle[KEY_GEN_TYPE]     = GEN_TYPE_REPO
                    savedStateHandle[KEY_GEN_EPISODES] = newGenerator.videos.toJson()
                    savedStateHandle[KEY_GEN_INDEX]    = newGenerator.videoIndex
                    newGenerator.page?.let { page ->
                        savedStateHandle[KEY_GEN_PAGE]     = page.toJson()
                        savedStateHandle[KEY_GEN_PAGE_CLS] = page::class.java.name
                    }
                }
                is LinkGenerator -> {
                    savedStateHandle[KEY_GEN_TYPE]    = GEN_TYPE_LINK
                    savedStateHandle[KEY_GEN_LINKS]   = newGenerator.links.toJson()
                    savedStateHandle[KEY_GEN_EXTRACT] = newGenerator.extract
                    newGenerator.refererUrl?.let { savedStateHandle[KEY_GEN_REFERER] = it }
                }
                is DownloadFileGenerator -> {
                    savedStateHandle[KEY_GEN_TYPE]  = GEN_TYPE_DOWNLOAD
                    savedStateHandle[KEY_GEN_URIS]  = newGenerator.videos.toJson()
                    savedStateHandle[KEY_GEN_INDEX] = newGenerator.videoIndex
                }
                // MinimalLinkGenerator: comes from external intents that re-fire on restore.
                // No persistent state needed.
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to persist generator to SavedStateHandle", t)
        }
    }

    fun getGenerator(): IGenerator? = generator

    /**
     * Selected link / subtitle, persisted so process death restores the exact source.
     */

    /** The selected link as restored from SavedStateHandle. Consumed once by [startPlayer]. */
    val restoredSelectedLink: Pair<ExtractorLink?, ExtractorUri?>? by lazy { selectedLinkFromHandle() }

    /** The selected subtitle as restored from SavedStateHandle. Consumed once by [startPlayer]. */
    val restoredSelectedSubtitle: SubtitleData? by lazy { tryParseJson<SubtitleData>(savedStateHandle[KEY_SEL_SUB_JSON]) }

    private fun selectedLinkFromHandle(): Pair<ExtractorLink?, ExtractorUri?>? = try {
        val isUri = savedStateHandle.get<Boolean>(KEY_SEL_LINK_IS_URI) ?: return null
        if (isUri) {
            val json = savedStateHandle.get<String>(KEY_SEL_URI_JSON) ?: return null
            tryParseJson<ExtractorUri>(json)?.let { null to it }
        } else {
            val json      = savedStateHandle.get<String>(KEY_SEL_LINK_JSON) ?: return null
            val className = savedStateHandle.get<String>(KEY_SEL_LINK_CLS)  ?: return null
            val extLink   = jacksonMapper.readValue(json, Class.forName(className)) as? ExtractorLink
            extLink?.let { it to null }
        }
    } catch (t: Throwable) {
        Log.e(TAG, "Failed to restore selected link from SavedStateHandle", t)
        null
    }

    /** Called by the Fragment whenever the user selects a source. Persists across process death. */
    fun saveSelectedState(
        link: Pair<ExtractorLink?, ExtractorUri?>?,
        subtitle: SubtitleData?
    ) {
        try {
            if (link != null) {
                val extLink = link.first
                val extUri  = link.second
                if (extLink != null) {
                    savedStateHandle[KEY_SEL_LINK_IS_URI] = false
                    savedStateHandle[KEY_SEL_LINK_JSON]   = extLink.toJson()
                    savedStateHandle[KEY_SEL_LINK_CLS]    = extLink::class.java.name
                } else if (extUri != null) {
                    savedStateHandle[KEY_SEL_LINK_IS_URI] = true
                    savedStateHandle[KEY_SEL_URI_JSON]    = extUri.toJson()
                }
            }
            if (subtitle != null) {
                savedStateHandle[KEY_SEL_SUB_JSON] = subtitle.toJson()
            } else {
                savedStateHandle.remove<String>(KEY_SEL_SUB_JSON)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to persist selected state to SavedStateHandle", t)
        }
    }

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
