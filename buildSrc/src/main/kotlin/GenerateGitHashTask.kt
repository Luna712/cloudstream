import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputDirectory

abstract class GenerateGitHashTask : DefaultTask() {

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:InputDirectory
    abstract val gitDir: RegularFileProperty

    @TaskAction
    fun run() {
        val headFile = gitDir.get().asFile.resolve("HEAD")
        val hash = if (headFile.exists()) {
            val text = headFile.readText().trim()
            if (text.startsWith("ref:")) {
                val ref = text.removePrefix("ref:").trim()
                val commitFile = gitDir.get().asFile.resolve(ref)
                if (commitFile.exists()) commitFile.readText().trim() else ""
            } else text
        } else ""

        outputFile.get().asFile.parentFile.mkdirs()
        outputFile.get().asFile.writeText(hash.take(7))
    }
}
