package gr.koukamedics.dispatcher

import java.net.URI

internal object UrlPolicy {
    const val APP_HOST = "nursego-athens.costaskaounas.chatgpt.site"

    fun opensInsideApp(url: String): Boolean =
        runCatching {
            val uri = URI(url)
            uri.scheme.equals("https", ignoreCase = true) &&
                uri.host.equals(APP_HOST, ignoreCase = true) &&
                uri.rawUserInfo == null &&
                (uri.port == -1 || uri.port == 443)
        }.getOrDefault(false)

    fun isWebUrl(url: String): Boolean =
        runCatching {
            val uri = URI(url)
            (uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) &&
                !uri.host.isNullOrBlank() && uri.rawUserInfo == null &&
                (uri.port == -1 || uri.port in 1..65535)
        }.getOrDefault(false)

    fun canOpenExternal(url: String): Boolean =
        isWebUrl(url) || runCatching {
            val uri = URI(url)
            uri.scheme?.lowercase(java.util.Locale.ROOT) in setOf(
                "tel", "mailto", "sms", "smsto", "geo", "google.navigation", "viber", "market",
            ) && !uri.rawSchemeSpecificPart.isNullOrBlank()
        }.getOrDefault(false)
}
