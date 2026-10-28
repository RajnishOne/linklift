package com.rjnsdev.linklift.app

import android.net.Uri
import android.util.Patterns
import android.webkit.URLUtil
import java.net.URI
import java.util.Locale
import java.util.regex.Pattern

private val YT_DLP_EXACT_HOSTS = setOf(
    "youtu.be",
    "instagr.am",
)

private val YT_DLP_HOST_MARKERS = listOf(
    "youtube.com",
    "youtube-nocookie.com",
    "instagram.com",
)

private val FALLBACK_URL_PATTERN = Pattern.compile(
    """https?://[^\s<>"']+|www\.[^\s<>"']+""",
    Pattern.CASE_INSENSITIVE
)

private fun getUriHost(url: String): String {
    val jvmHost = runCatching { URI(url).host }.getOrNull()
    if (!jvmHost.isNullOrBlank()) return jvmHost
    return runCatching { Uri.parse(url).host }.getOrNull().orEmpty()
}

private fun getUriPath(url: String): String {
    val jvmPath = runCatching { URI(url).path }.getOrNull()
    if (!jvmPath.isNullOrBlank()) return jvmPath
    return runCatching { Uri.parse(url).path }.getOrNull().orEmpty()
}

private fun getUriQueryParam(url: String, key: String): String? {
    val jvmParam = runCatching {
        val query = URI(url).rawQuery.orEmpty()
        query.split("&")
            .map { it.split("=", limit = 2) }
            .firstOrNull { it.size == 2 && it[0] == key }
            ?.get(1)
    }.getOrNull()
    if (!jvmParam.isNullOrBlank()) return jvmParam
    return runCatching { Uri.parse(url).getQueryParameter(key) }.getOrNull()
}

private fun isHttpOrHttps(url: String): Boolean {
    val runAndroid = runCatching { URLUtil.isNetworkUrl(url) }.getOrNull()
    if (runAndroid != null) return runAndroid
    val lowered = url.lowercase(Locale.ROOT)
    return lowered.startsWith("http://") || lowered.startsWith("https://")
}

/** Hosts routed through yt-dlp (`generic_media_resolver`) instead of HTML scraping. */
internal fun isYtDlpSupportedHost(url: String): Boolean {
    val host = getUriHost(url).lowercase(Locale.ROOT).removePrefix("www.")
    if (host.isBlank()) return false
    if (host in YT_DLP_EXACT_HOSTS) return true
    return YT_DLP_HOST_MARKERS.any { marker ->
        host == marker || host.endsWith(".$marker") || host.contains(marker)
    }
}

internal fun extractFirstUrl(text: String): String? = extractAllUrls(text).firstOrNull()

internal fun extractAllUrls(text: String): List<String> {
    if (text.isBlank()) return emptyList()
    val pattern = runCatching { Patterns.WEB_URL }.getOrNull() ?: FALLBACK_URL_PATTERN
    val matcher = pattern.matcher(text)
    val seen = LinkedHashSet<String>()
    while (matcher.find()) {
        val candidate = matcher.group()
            ?.trim()
            ?.trimEnd('.', ',', ';', ':', ')', ']', '}', '!', '?', '"', '\'')
            ?: continue
        val normalized = if (isHttpOrHttps(candidate)) {
            candidate
        } else if (candidate.startsWith("//")) {
            "https:$candidate"
        } else if (candidate.contains(".") && !candidate.startsWith("http")) {
            "https://$candidate".takeIf { isHttpOrHttps(it) }
        } else {
            null
        } ?: continue
        seen += normalized
    }
    return seen.toList()
}

internal fun looksLikePlaylistUrl(rawUrl: String): Boolean {
    val host = getUriHost(rawUrl).lowercase(Locale.ROOT).removePrefix("www.")
    val path = getUriPath(rawUrl).lowercase(Locale.ROOT)
    val listParam = getUriQueryParam(rawUrl, "list")

    return when {
        host.endsWith("youtube.com") || host == "music.youtube.com" -> {
            path.startsWith("/playlist") ||
                path.startsWith("/channel/") ||
                path.startsWith("/c/") ||
                path.startsWith("/user/") ||
                path.startsWith("/@") ||
                (!listParam.isNullOrBlank() && (path.startsWith("/playlist") || path.startsWith("/watch") && listParam.startsWith("PL")))
        }
        host == "youtu.be" -> !listParam.isNullOrBlank()
        else -> false
    }
}

internal fun shouldRefreshUrlBeforeDownload(url: String): Boolean {
    return isInstagramUrl(url)
}

internal fun shouldDownloadWithMergeService(format: MediaFormat, sourceUrl: String): Boolean {
    if (format.requiresMerge) return true
    if (isYouTubeUrl(sourceUrl) || format.mediaUrl.contains("googlevideo.com")) return true
    val host = getUriHost(sourceUrl).lowercase(Locale.ROOT)
    return host.contains("youtube.com") || host == "youtu.be" || host.contains("youtu.be")
}

internal fun isYouTubeUrl(url: String): Boolean {
    val host = getUriHost(url).lowercase(Locale.ROOT).removePrefix("www.")
    return host == "youtube.com" || host.endsWith(".youtube.com") ||
           host == "youtube-nocookie.com" || host.endsWith(".youtube-nocookie.com") ||
           host == "youtu.be" || host.endsWith(".youtu.be")
}

internal fun isInstagramUrl(url: String): Boolean {
    val host = getUriHost(url).lowercase(Locale.ROOT).removePrefix("www.")
    return host == "instagram.com" || host.endsWith(".instagram.com") ||
           host == "instagr.am" || host.endsWith(".instagr.am")
}

internal fun isSupportedMediaUrl(url: String): Boolean {
    return isYouTubeUrl(url) || isInstagramUrl(url)
}

internal fun extractYouTubeVideoId(url: String): String? {
    val host = getUriHost(url).lowercase(Locale.ROOT).removePrefix("www.")
    if (host == "youtu.be" || host.endsWith(".youtu.be")) {
        return getUriPath(url).trimStart('/').takeIf { it.isNotBlank() }
    }
    if (host.contains("youtube.com")) {
        val vParam = getUriQueryParam(url, "v")
        if (!vParam.isNullOrBlank()) return vParam
        val path = getUriPath(url)
        if (path.startsWith("/shorts/")) {
            return path.removePrefix("/shorts/").substringBefore('/').substringBefore('?')
        }
        if (path.startsWith("/embed/")) {
            return path.removePrefix("/embed/").substringBefore('/').substringBefore('?')
        }
    }
    return null
}

internal fun extractYouTubeThumbnailUrl(url: String): String? {
    val id = extractYouTubeVideoId(url) ?: return null
    return "https://i.ytimg.com/vi/$id/hqdefault.jpg"
}
