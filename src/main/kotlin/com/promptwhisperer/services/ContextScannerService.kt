package com.promptwhisperer.services

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.nio.charset.StandardCharsets

/**
 * SafeProjectContext contains metadata and small file contents that are explicitly allowed.
 * Avoids secrets and large files.
 */
data class SafeProjectContext(
    val projectName: String?,
    val safeFiles: List<String>,
    // path -> content (only for small files)
    val smallFileContents: Map<String, String>,
    val excluded: List<String>,
) {
    fun humanReadableSummary(): String {
        val sb = StringBuilder()
        sb.append("Project: ${projectName ?: "unknown"}\n\n")
        sb.append("Included files:\n")
        safeFiles.forEach { sb.append("- $it\n") }
        if (smallFileContents.isNotEmpty()) {
            sb.append("\nSmall file previews:\n")
            smallFileContents.forEach { (k, v) ->
                sb.append("--- $k ---\n")
                sb.append(v.lines().take(20).joinToString("\n") + "\n")
            }
        }
        if (excluded.isNotEmpty()) {
            sb.append("\nExcluded files (for safety):\n")
            excluded.forEach { sb.append("- $it\n") }
        }
        return sb.toString()
    }
}

/**
 * Scans project files safely. Reads filenames, small whitelisted files and returns a SafeProjectContext.
 * This implementation uses IntelliJ VFS for read-only access and avoids reading blocked patterns.
 */
interface ContextScannerService {
    fun scanProject(project: Project): SafeProjectContext
}

class ContextScannerServiceImpl : ContextScannerService {
    // Max size for small file reading: 16 KB
    private val maxSmallFileBytes = 16 * 1024

    override fun scanProject(project: Project): SafeProjectContext {
        val base = project.baseDir
        val collected = mutableListOf<String>()
        val smallContents = mutableMapOf<String, String>()
        val excluded = mutableListOf<String>()

        // gather top-level file tree (shallow) and certain named files
        scanVirtualFile(base, project, collected, smallContents, excluded, depth = 3)
        val projectName = project.name
        return SafeProjectContext(projectName, collected.sorted(), smallContents, excluded.sorted())
    }

    private fun scanVirtualFile(
        vf: VirtualFile,
        project: Project,
        collected: MutableList<String>,
        smallContents: MutableMap<String, String>,
        excluded: MutableList<String>,
        depth: Int,
    ) {
        if (depth < 0) return
        if (vf.isDirectory) {
            vf.children.forEach { child ->
                scanVirtualFile(child, project, collected, smallContents, excluded, depth - 1)
            }
        } else {
            val rel =
                if (project.basePath != null && vf.path.startsWith(project.basePath!!)) {
                    vf.path.substring(project.basePath!!.length).trimStart('/')
                } else {
                    vf.path
                }
            collected.add(rel)
            val name = vf.name.lowercase()
            val whitelist =
                setOf("readme.md", "build.gradle", "build.gradle.kts", "pom.xml", "package.json", "pyproject.toml", "requirements.txt", "go.mod", "cargo.toml", "dockerfile", "docker-compose.yml")
            if (whitelist.any { name.contains(it) }) {
                try {
                    val length = vf.length
                    if (length in 1..maxSmallFileBytes) {
                        val bytes = vf.contentsToByteArray()
                        val content = String(bytes, StandardCharsets.UTF_8)
                        smallContents[rel] = content.take(maxSmallFileBytes)
                    }
                } catch (e: Exception) {
                    excluded.add(rel)
                }
            }
        }
    }
}
