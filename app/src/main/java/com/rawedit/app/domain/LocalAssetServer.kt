package com.rawedit.app.domain

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.util.Locale

class LocalAssetServer(
    private val context: Context,
    @Volatile var currentHtmlContent: String = ""
) {
    companion object {
        const val HOST_RAW_LOCAL = "raw.local"
        const val HOST_ASSETS_LOCAL = "assets.local"
        const val BASE_URL = "https://raw.local/index.html"

        private val FONT_ALIASES = mapOf(
            "inter" to "sans-serif",
            "jetbrains mono" to "monospace",
            "montserrat" to "sans-serif-black",
            "anton" to "sans-serif-condensed",
            "bebas neue" to "sans-serif-condensed",
            "playfair display" to "serif",
            "space grotesk" to "sans-serif-medium",
            "poppins" to "sans-serif",
            "oswald" to "sans-serif-condensed",
            "open sans" to "sans-serif"
        )
    }

    private val registeredLocalFiles = HashMap<String, File>()

    fun registerUserFile(aliasName: String, file: File) {
        synchronized(registeredLocalFiles) {
            registeredLocalFiles[aliasName.lowercase(Locale.US)] = file
        }
    }

    fun intercept(request: WebResourceRequest): WebResourceResponse {
        val url = request.url ?: return forbiddenResponse("Null URL")
        val scheme = (url.scheme ?: "").lowercase(Locale.US)
        val host = (url.host ?: "").lowercase(Locale.US)
        val path = (url.path ?: "/").removePrefix("/")

        if (scheme == "data" || scheme == "blob") {
            return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
        }

        if (scheme != "https" || (host != HOST_RAW_LOCAL && host != HOST_ASSETS_LOCAL)) {
            return forbiddenResponse("Blocked non-local request: $url")
        }

        // 1. Composition HTML
        if (host == HOST_RAW_LOCAL && (path.isEmpty() || path == "index.html")) {
            val bytes = currentHtmlContent.toByteArray(Charsets.UTF_8)
            return WebResourceResponse(
                "text/html",
                "UTF-8",
                200,
                "OK",
                mapOf("Cache-Control" to "no-store", "Access-Control-Allow-Origin" to "*"),
                ByteArrayInputStream(bytes)
            )
        }

        // 2. Virtual CSS font alias helper if requested as /fonts/<name>.css
        if (path.startsWith("fonts/")) {
            val fontKey = path.removePrefix("fonts/").removeSuffix(".css").replace('-', ' ').lowercase(Locale.US)
            val mappedSystemFont = FONT_ALIASES[fontKey] ?: "sans-serif"
            val css = "@font-face { font-family: '$fontKey'; src: local('$mappedSystemFont'); }"
            return WebResourceResponse(
                "text/css",
                "UTF-8",
                200,
                "OK",
                mapOf("Access-Control-Allow-Origin" to "*"),
                ByteArrayInputStream(css.toByteArray(Charsets.UTF_8))
            )
        }

        // 3. Registered local user media/image
        val localFile = synchronized(registeredLocalFiles) {
            registeredLocalFiles[path.lowercase(Locale.US)]
        }
        if (localFile != null && localFile.exists()) {
            val mime = guessMimeType(path)
            return WebResourceResponse(
                mime,
                "UTF-8",
                200,
                "OK",
                mapOf("Access-Control-Allow-Origin" to "*"),
                FileInputStream(localFile)
            )
        }

        // 4. Bundled style-pack or sample assets
        return runCatching {
            val assetStream = context.assets.open(path)
            WebResourceResponse(
                guessMimeType(path),
                "UTF-8",
                200,
                "OK",
                mapOf("Access-Control-Allow-Origin" to "*"),
                assetStream
            )
        }.getOrElse {
            forbiddenResponse("Asset not found: $path")
        }
    }

    private fun guessMimeType(path: String): String {
        val ext = path.substringAfterLast('.', "").lowercase(Locale.US)
        return when (ext) {
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "json" -> "application/json"
            "svg" -> "image/svg+xml"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            else -> "application/octet-stream"
        }
    }

    private fun forbiddenResponse(reason: String): WebResourceResponse {
        val body = "403 FORBIDDEN (OFFLINE SANDBOX): $reason".toByteArray(Charsets.UTF_8)
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            403,
            "Forbidden",
            mapOf("Access-Control-Allow-Origin" to "*"),
            ByteArrayInputStream(body)
        )
    }
}
