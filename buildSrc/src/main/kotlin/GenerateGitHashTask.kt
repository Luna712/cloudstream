import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class GenerateGitHashTask : DefaultTask() {

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val head = project.layout.projectDirectory.file(".git/HEAD").asFile
        val hash = if (head.exists()) {
            val text = head.readText().trim()
            if (text.startsWith("ref:")) {
                val ref = text.removePrefix("ref:").trim()
                val commitFile = project.layout.projectDirectory.file(".git/$ref").asFile
                if (commitFile.exists()) commitFile.readText().trim() else ""
            } else text
        } else ""

        outputFile.get().asFile.writeText(hash.take(7))
    }
}
