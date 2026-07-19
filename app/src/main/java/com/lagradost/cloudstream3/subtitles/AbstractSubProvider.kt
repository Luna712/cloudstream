package com.lagradost.cloudstream3.subtitles

import androidx.core.net.toUri
import com.lagradost.cloudstream3.MainActivity.Companion.deleteFileOnExit
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.ui.player.SubtitleOrigin
import kotlinx.io.Source
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import java.io.File
import java.util.zip.ZipInputStream

/**
 * A builder for subtitle files.
 * @see addUrl
 * @see addFile
 */
class SubtitleResource {
    fun downloadFile(source: Source): File {
        val file = File.createTempFile("temp-subtitle", ".tmp").apply {
            deleteFileOnExit(this)
        }
        file.outputStream().asSink().buffered().use { sink ->
            sink.transferFrom(source)
        }
        source.close()

        return file
    }

    private fun unzip(file: File): List<Pair<String, File>> {
        val entries = mutableListOf<Pair<String, File>>()

        ZipInputStream(file.inputStream()).use { zipInputStream ->
            var zipEntry = zipInputStream.nextEntry

            while (zipEntry != null) {
                val tempFile = File.createTempFile("unzipped-subtitle", ".tmp").apply {
                    deleteFileOnExit(this)
                }
                entries.add(zipEntry.name to tempFile)

                tempFile.outputStream().asSink().buffered().use { sink ->
                    sink.transferFrom(zipInputStream.asSource())
                }

                zipEntry = zipInputStream.nextEntry
            }
        }
        return entries
    }

    data class SingleSubtitleResource(
        val name: String?,
        val url: String,
        val origin: SubtitleOrigin
    )

    private var resources: MutableList<SingleSubtitleResource> = mutableListOf()

    fun getSubtitles(): List<SingleSubtitleResource> {
        return resources.toList()
    }

    fun addUrl(url: String?, name: String? = null) {
        if (url == null) return
        this.resources.add(
            SingleSubtitleResource(name, url, SubtitleOrigin.URL)
        )
    }

    fun addFile(file: File, name: String? = null) {
        this.resources.add(
            SingleSubtitleResource(name, file.toUri().toString(), SubtitleOrigin.DOWNLOADED_FILE)
        )
        deleteFileOnExit(file)
    }

    suspend fun addZipUrl(
        url: String,
        nameGenerator: (String, File) -> String? = { _, _ -> null }
    ) {
        val source = app.get(url).body().source()
        val zip = downloadFile(source)
        val realFiles = unzip(zip)
        zip.deleteRecursively()
        realFiles.forEach { (name, subtitleFile) ->
            addFile(subtitleFile, nameGenerator(name, subtitleFile))
        }
    }
}
