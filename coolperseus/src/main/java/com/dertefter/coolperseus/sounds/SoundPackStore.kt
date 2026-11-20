package com.dertefter.coolperseus.sounds

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import java.io.File
import java.util.Locale


object SoundPackStore {

    const val AUTHORITY = "com.dertefter.coolperseus.sounds"

    const val ROOT_SEGMENT = "sound_packs"

    private const val SCHEME_CONTENT = "content"

    fun rootDir(context: Context): File =
        File(context.filesDir, ROOT_SEGMENT).apply {
            if (!exists()) mkdirs()
        }

    fun packDir(context: Context, packId: String): File =
        File(rootDir(context), packId).apply {
            if (!exists()) mkdirs()
        }

    fun contentUri(context: Context, file: File): String {
        val segments = relativeSegments(rootDir(context), file)
        return Uri.Builder()
            .scheme(SCHEME_CONTENT)
            .authority(AUTHORITY)
            .apply {
                appendPath(ROOT_SEGMENT)
                segments.forEach { appendPath(it) }
            }
            .build()
            .toString()
    }

    fun isContentUri(raw: String): Boolean =
        raw.startsWith("$SCHEME_CONTENT://$AUTHORITY/")

    fun resolve(context: Context, uri: Uri?): File? {
        if (uri == null || (uri.scheme != SCHEME_CONTENT || uri.authority != AUTHORITY)) return null

        val segments = uri.pathSegments
        if (segments.isEmpty() || segments[0] != ROOT_SEGMENT || segments.size < 3) return null

        val root = rootDir(context)
        var target = root
        for (segment in segments.drop(1)) {
            if (segment == "." || segment == ".." || segment.isEmpty()) return null
            target = File(target, segment)
        }

        return runCatching {
            val canonicalRoot = root.canonicalPath
            val canonicalTarget = target.canonicalPath
            if (canonicalTarget == canonicalRoot ||
                canonicalTarget.startsWith(canonicalRoot + File.separator)
            ) {
                target.takeIf { it.isFile }
            } else {
                null
            }
        }.getOrNull()
    }

    fun deletePack(context: Context, packId: String) {
        runCatching { File(rootDir(context), packId).deleteRecursively() }
    }

    fun safeFileName(raw: String): String {
        val name = raw.substringAfterLast('/').substringAfterLast('\\')
        val cleaned = buildString(name.length) {
            for (ch in name) {
                append(if (ch.isLetterOrDigit() || ch == '.' || ch == '-' || ch == '_') ch else '_')
            }
        }.trim('.', '_')

        return cleaned.ifEmpty { "file" }
    }

    fun uniqueFile(dir: File, fileName: String): File {
        val first = File(dir, fileName)
        if (!first.exists()) return first

        val base = fileName.substringBeforeLast('.', fileName)
        val extension = fileName.substringAfterLast('.', "")
        var index = 1
        while (true) {
            val candidateName = if (extension.isEmpty()) "$base-$index" else "$base-$index.$extension"
            val candidate = File(dir, candidateName)
            if (!candidate.exists()) return candidate
            index++
        }
    }

    fun localFile(context: Context, raw: String?): File? = when {
        raw.isNullOrEmpty() -> null
        isContentUri(raw) -> resolve(context, raw.toUri())
        else -> File(raw).takeIf { it.isFile }
    }

    private fun relativeSegments(root: File, file: File): List<String> {
        val rootPath = splitPath(root.absolutePath)
        val filePath = splitPath(file.absolutePath)
        var common = 0
        while (common < rootPath.size && common < filePath.size && rootPath[common] == filePath[common]) {
            common++
        }
        return filePath.drop(common)
    }

    private fun splitPath(path: String): List<String> =
        path.split(File.separatorChar).filter { it.isNotEmpty() }

    fun mimeOf(fileName: String): String? = when (fileName.lowercase(Locale.ROOT).substringAfterLast('.', "")) {
        "ogg" -> "audio/ogg"
        "mp3" -> "audio/mpeg"
        "m4a", "mp4", "aac" -> "audio/mp4a-latm"
        "wav" -> "audio/x-wav"
        "flac" -> "audio/flac"
        "opus" -> "audio/opus"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "bmp" -> "image/bmp"
        else -> null
    }
}
