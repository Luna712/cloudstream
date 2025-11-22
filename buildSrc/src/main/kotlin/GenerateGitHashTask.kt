import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class GenerateGitHashTask : DefaultTask() {

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val head = file("${project.rootDir}/.git/HEAD")
        val hash = if (head.exists()) {
            val text = head.readText().trim()
            if (text.startsWith("ref:")) {
                val ref = text.removePrefix("ref:").trim()
                val commitFile = file("${project.rootDir}/.git/$ref")
                if (commitFile.exists()) commitFile.readText().trim() else ""
            } else text
        } else ""

        val content = """
            package com.lagradost.cloudstream3
            object GitInfo {
                const val HASH = "${hash.take(7)}"
            }
        """.trimIndent()

        outputFile.get().asFile.parentFile.mkdirs()
        outputFile.get().asFile.writeText(content)
    }
}
