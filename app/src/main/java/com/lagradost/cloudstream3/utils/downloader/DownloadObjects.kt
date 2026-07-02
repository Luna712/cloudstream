package com.lagradost.cloudstream3.utils.downloader

import android.net.Uri
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.SkipSerializationTest
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.services.DownloadQueueService
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.serializers.UriSerializer
import com.lagradost.cloudstream3.utils.serializers.WriteOnlySerializer
import com.lagradost.safefile.SafeFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.IOException
import java.io.OutputStream
import java.util.Objects

object DownloadObjects {
    /** An item can either be something to resume or something new to start */
    @Serializable
    data class DownloadQueueWrapper(
        @SerialName("resumePackage") val resumePackage: DownloadResumePackage?,
        @SerialName("downloadItem") val downloadItem: DownloadQueueItem?,
    ) {
        init {
            assert(resumePackage != null || downloadItem != null) {
                "ResumeID and downloadItem cannot both be null at the same time!"
            }
        }

        /** Loop through the current download instances to see if it is currently downloading. Also includes link loading. */
        fun isCurrentlyDownloading(): Boolean {
            return DownloadQueueService.downloadInstances.value.any { it.downloadQueueWrapper.id == this.id }
        }

        @SerialName("id")
        val id = resumePackage?.item?.ep?.id ?: downloadItem!!.episode.id

        @SerialName("parentId")
        val parentId = resumePackage?.item?.ep?.parentId ?: downloadItem!!.episode.parentId
    }

    /** General data about the episode and show to start a download from. */
    @Serializable
    data class DownloadQueueItem(
        @SerialName("episode") val episode: ResultEpisode,
        @SerialName("isMovie") val isMovie: Boolean,
        @SerialName("resultName") val resultName: String,
        @SerialName("resultType") val resultType: TvType,
        @SerialName("resultPoster") val resultPoster: String?,
        @SerialName("apiName") val apiName: String,
        @SerialName("resultId") val resultId: Int,
        @SerialName("resultUrl") val resultUrl: String,
        @SerialName("links") val links: List<ExtractorLink>? = null,
        @SerialName("subs") val subs: List<SubtitleData>? = null,
    ) {
        fun toWrapper(): DownloadQueueWrapper {
            return DownloadQueueWrapper(null, this)
        }
    }

    @Serializable
    abstract class DownloadCached(
        @Transient open val id: Int = 0,
    )

    @OptIn(ExperimentalSerializationApi::class) // KeepGeneratedSerializer is an experimental annotation for now
    @KeepGeneratedSerializer
    @Serializable(with = DownloadEpisodeCached.Serializer::class)
    data class DownloadEpisodeCached(
        @SerialName("name") val name: String?,
        @SerialName("poster") val poster: String?,
        @SerialName("episode") val episode: Int,
        @SerialName("season") val season: Int?,
        @SerialName("parentId") val parentId: Int,
        @SerialName("score") var score: Score? = null,
        @SerialName("description") val description: String?,
        @SerialName("cacheTime") val cacheTime: Long,
        @SerialName("id") override val id: Int,
    ) : DownloadCached(id) {
        object Serializer : WriteOnlySerializer<DownloadEpisodeCached>(
            DownloadEpisodeCached.generatedSerializer(),
            setOf("rating"),
        )

        @SerialName("rating")
        @Deprecated(
            "`rating` is the old scoring system, use score instead",
            replaceWith = ReplaceWith("score"),
            level = DeprecationLevel.ERROR,
        )
        var rating: Int? = null
            set(value) {
                if (value != null) {
                    @Suppress("DEPRECATION_ERROR")
                    score = Score.fromOld(value)
                }
            }
    }

    /** What to display to the user for a downloaded show/movie. Includes info such as name, poster and url */
    @Serializable
    data class DownloadHeaderCached(
        @SerialName("apiName") val apiName: String,
        @SerialName("url") val url: String,
        @SerialName("type") val type: TvType,
        @SerialName("name") val name: String,
        @SerialName("poster") val poster: String?,
        @SerialName("cacheTime") val cacheTime: Long,
        @SerialName("id") override val id: Int,
    ) : DownloadCached(id)

    @Serializable
    data class DownloadResumePackage(
        @SerialName("item") val item: DownloadItem,
        /** Tills which link should get resumed */
        @SerialName("linkIndex") val linkIndex: Int?,
    ) {
        fun toWrapper(): DownloadQueueWrapper {
            return DownloadQueueWrapper(this, null)
        }
    }

    @Serializable
    data class DownloadItem(
        @SerialName("source") val source: String?,
        @SerialName("folder") val folder: String?,
        @SerialName("ep") val ep: DownloadEpisodeMetadata,
        @SerialName("links") val links: List<ExtractorLink>,
    )

    /** Metadata for a specific episode and how to display it. */
    @Serializable
    data class DownloadEpisodeMetadata(
        @SerialName("id") val id: Int,
        @SerialName("parentId") val parentId: Int,
        @SerialName("mainName") val mainName: String,
        @SerialName("sourceApiName") val sourceApiName: String?,
        @SerialName("poster") val poster: String?,
        @SerialName("name") val name: String?,
        @SerialName("season") val season: Int?,
        @SerialName("episode") val episode: Int?,
        @SerialName("type") val type: TvType?,
    )

    @Serializable
    data class DownloadedFileInfo(
        @SerialName("totalBytes") val totalBytes: Long,
        @SerialName("relativePath") val relativePath: String,
        @SerialName("displayName") val displayName: String,
        @SerialName("extraInfo") val extraInfo: String? = null,
        @SerialName("basePath") val basePath: String? = null, // null is for legacy downloads. See getBasePath()
        // Hash of the link associated with this DownloadFile, used so not override old data in the DownloadedFileInfo
        @SerialName("linkHash") val linkHash : Int? = null,
    )

    @Serializable
    @SkipSerializationTest // Uri has issues with Jackson
    data class DownloadedFileInfoResult(
        @SerialName("fileLength") val fileLength: Long,
        @SerialName("totalBytes") val totalBytes: Long,
        @SerialName("path")
        @Serializable(with = UriSerializer::class)
        val path: Uri,
    )

    @Serializable
    data class ResumeWatching(
        @SerialName("parentId") val parentId: Int,
        @SerialName("episodeId") val episodeId: Int?,
        @SerialName("episode") val episode: Int?,
        @SerialName("season") val season: Int?,
        @SerialName("updateTime") val updateTime: Long,
        @SerialName("isFromDownload") val isFromDownload: Boolean,
    )

    data class DownloadStatus(
        /** if you should retry with the same args and hope for a better result */
        val retrySame: Boolean,
        /** if you should try the next mirror */
        val tryNext: Boolean,
        /** if the result is what the user intended */
        val success: Boolean,
    )

    data class CreateNotificationMetadata(
        val type: VideoDownloadManager.DownloadType,
        val bytesDownloaded: Long,
        val bytesTotal: Long,
        val hlsProgress: Long? = null,
        val hlsTotal: Long? = null,
        val bytesPerSecond: Long,
    )

    data class StreamData(
        private val fileLength: Long,
        val file: SafeFile,
        // val fileStream: OutputStream,
    ) {
        @Throws(IOException::class)
        fun open(): OutputStream {
            return file.openOutputStreamOrThrow(resume)
        }

        @Throws(IOException::class)
        fun openNew(): OutputStream {
            return file.openOutputStreamOrThrow(false)
        }

        fun delete(): Boolean {
            return file.delete() == true
        }

        val resume: Boolean get() = fileLength > 0L
        val startAt: Long get() = if (resume) fileLength else 0L
        val exists: Boolean get() = file.exists() == true
    }

    /**
     * Bytes have the size end-start where the byte range is [start,end)
     * note that ByteArray is a pointer and therefore can't be stored
     * without cloning it.
     */
    data class LazyStreamDownloadResponse(
        val bytes: ByteArray,
        val startByte: Long,
        val endByte: Long,
    ) {
        val size get() = endByte - startByte

        override fun toString(): String {
            return "$startByte->$endByte"
        }

        override fun equals(other: Any?): Boolean {
            if (other !is LazyStreamDownloadResponse) return false
            return other.startByte == startByte && other.endByte == endByte
        }

        override fun hashCode(): Int {
            return Objects.hash(startByte, endByte)
        }
    }
}
