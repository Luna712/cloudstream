import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenerateGitHashTask : DefaultTask() {

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val head = File(project.rootDir, ".git/HEAD")
        val hash = if (head.exists()) {
            val text = head.readText().trim()
            if (text.startsWith("ref:")) {
                val ref = text.removePrefix("ref:").trim()
                val commitFile = File(project.rootDir, ".git/$ref")
                if (commitFile.exists()) commitFile.readText().trim() else ""
            } else text
        } else ""

        val content = """
            package com.lagradost.cloudstream3
            object GitInfo {
                const val HASH = "${hash.take(7)}"
            }
        """.trimIndent()

        // Ensure parent directories exist
        outputFile.get().asFile.parentFile.mkdirs()
        outputFile.get().asFile.writeText(content)
    }
}
