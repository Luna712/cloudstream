package com.lagradost.cloudstream3.ui.download

import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.isMovieType
import com.lagradost.cloudstream3.mvvm.launchSafe
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.DOWNLOAD_EPISODE_CACHE
import com.lagradost.cloudstream3.utils.DOWNLOAD_HEADER_CACHE
import com.lagradost.cloudstream3.utils.DataStore.getFolderName
import com.lagradost.cloudstream3.utils.DataStore.getKey
import com.lagradost.cloudstream3.utils.DataStore.getKeys
import com.lagradost.cloudstream3.utils.VideoDownloadHelper
import com.lagradost.cloudstream3.utils.VideoDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadViewModel : ViewModel() {
    private val _noDownloadsText = MutableLiveData<String>().apply { value = "" }
    val noDownloadsText: LiveData<String> = _noDownloadsText

    private val _headerCards = MutableLiveData<List<VisualDownloadHeaderCached>>().apply { value = emptyList() }
    val headerCards: LiveData<List<VisualDownloadHeaderCached>> = _headerCards

    private val _usedBytes = MutableLiveData<Long>()
    val usedBytes: LiveData<Long> = _usedBytes

    private val _availableBytes = MutableLiveData<Long>()
    val availableBytes: LiveData<Long> = _availableBytes

    private val _downloadBytes = MutableLiveData<Long>()
    val downloadBytes: LiveData<Long> = _downloadBytes

    fun updateList(context: Context) = viewModelScope.launchSafe {
        val children = withContext(Dispatchers.IO) {
            context.getKeys(DOWNLOAD_EPISODE_CACHE).mapNotNull {
                context.getKey<VideoDownloadHelper.DownloadEpisodeCached>(it)
            }.distinctBy { it.id }
        }

        val (totalBytesUsedByChild, currentBytesUsedByChild, totalDownloads) = withContext(Dispatchers.IO) {
            val totalBytesUsedByChild = mutableMapOf<Int, Long>()
            val currentBytesUsedByChild = mutableMapOf<Int, Long>()
            val totalDownloads = mutableMapOf<Int, Int>()

            for (c in children) {
                val childFile = VideoDownloadManager.getDownloadFileInfoAndUpdateSettings(context, c.id) ?: continue
                if (childFile.fileLength <= 1) continue

                val len = childFile.totalBytes
                val flen = childFile.fileLength

                totalBytesUsedByChild[c.parentId] = (totalBytesUsedByChild[c.parentId] ?: 0) + len
                currentBytesUsedByChild[c.parentId] = (currentBytesUsedByChild[c.parentId] ?: 0) + flen
                totalDownloads[c.parentId] = (totalDownloads[c.parentId] ?: 0) + 1
            }

            Triple(totalBytesUsedByChild, currentBytesUsedByChild, totalDownloads)
        }

        val cachedHeaders = withContext(Dispatchers.IO) {
            totalDownloads.keys.mapNotNull { key ->
                context.getKey<VideoDownloadHelper.DownloadHeaderCached>(DOWNLOAD_HEADER_CACHE, key.toString())
            }
        }

        val visualHeaders = withContext(Dispatchers.Default) {
            cachedHeaders.mapNotNull { header ->
                val downloads = totalDownloads[header.id] ?: 0
                val bytes = totalBytesUsedByChild[header.id] ?: 0
                val currentBytes = currentBytesUsedByChild[header.id] ?: 0

                if (bytes <= 0 || downloads <= 0) return@mapNotNull null

                val movieEpisode = if (header.type.isMovieType()) {
                    context.getKey<VideoDownloadHelper.DownloadEpisodeCached>(
                        DOWNLOAD_EPISODE_CACHE,
                        getFolderName(header.id.toString(), header.id.toString())
                    )
                } else null

                VisualDownloadHeaderCached(
                    0,
                    downloads,
                    bytes,
                    currentBytes,
                    header,
                    movieEpisode
                )
            }.sortedBy { (it.child?.episode ?: 0) + (it.child?.season?.times(10000) ?: 0) }
        }

        try {
            val statFs = StatFs(Environment.getExternalStorageDirectory().path)
            val availableBytes = statFs.availableBytes
            val totalBytes = statFs.blockSizeLong * statFs.blockCountLong
            val downloadedBytes = visualHeaders.sumOf { it.totalBytes }

            _usedBytes.postValue(totalBytes - availableBytes - downloadedBytes)
            _availableBytes.postValue(availableBytes)
            _downloadBytes.postValue(downloadedBytes)
        } catch (t : Throwable) {
            _downloadBytes.postValue(0)
            logError(t)
        }

        _headerCards.postValue(visualHeaders)
    }
}