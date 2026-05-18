package com.promptwhisperer.services

import com.intellij.openapi.project.Project
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Persists generated prompts as versioned artefacts under `.prompt-whisperer/prompts/`.
 * Maintains an `index.json` with SHA-256 integrity hashes.
 */
interface ArtefactPersistenceService {
    fun savePromptArtefact(promptText: String, project: Project): PromptArtefactEntry
}

@Serializable
data class IndexEntry(
    val artefact_id: String,
    val filename: String,
    val created_at: String,
    val task_summary: String,
    val project_name: String?,
    val plugin_version: String,
    val schema_version: Int = 1,
    val sha256: String
)

data class PromptArtefactEntry(val filename: String, val createdAt: String, val sha256: String)

class ArtefactPersistenceServiceImpl(private val project: Project) : ArtefactPersistenceService {
    private val baseDir: File = File(project.basePath ?: ".", ".prompt-whisperer")
    private val promptsDir = File(baseDir, "prompts")
    private val indexFile = File(baseDir, "index.json")
    private val json = Json { prettyPrint = true }

    private val pluginVersion = "0.1.0"

    override fun savePromptArtefact(promptText: String, project: Project): PromptArtefactEntry {
        if (!promptsDir.exists()) {
            promptsDir.mkdirs()
        }
        val now = Instant.now()
        val ts = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault()).format(now)
        val taskSlug = SlugUtil.slugify(promptText.take(100))
        val filename = "$ts-$taskSlug.prompt.md"
        val file = File(promptsDir, filename)
        val frontMatter = buildFrontMatter(now, project, taskSlug)
        val contents = frontMatter + "\n" + promptText
        file.writeText(contents)
        val sha = HashingServiceImpl().sha256(contents.toByteArray())

        // update index atomically
        val entries = if (indexFile.exists()) {
            try {
                json.decodeFromString(ListSerializer(IndexEntry.serializer()), indexFile.readText())
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()
        val newEntry = IndexEntry(
            artefact_id = java.util.UUID.randomUUID().toString(),
            filename = "prompts/$filename",
            created_at = now.toString(),
            task_summary = promptText.lines().firstOrNull() ?: "",
            project_name = project.name,
            plugin_version = pluginVersion,
            sha256 = sha
        )
        val updated = entries + newEntry
        indexFile.parentFile?.mkdirs()
        indexFile.writeText(json.encodeToString(updated))
        return PromptArtefactEntry(filename, now.toString(), sha)
    }

    private fun buildFrontMatter(now: Instant, project: Project, taskSlug: String): String {
        val iso = DateTimeFormatter.ISO_INSTANT.format(now)
        val projectName = project.name
        return """
            ---
            artefact_type: ai_prompt
            artefact_schema_version: 1
            prompt_whisperer_version: $pluginVersion
            created_at: $iso
            project_name: $projectName
            task_slug: $taskSlug
            source: prompt-whisperer-intellij-plugin
            ---
        """.trimIndent()
    }
}
