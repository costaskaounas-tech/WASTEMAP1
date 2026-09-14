package gr.koukamedics.dispatcher

import java.net.URI

internal object UrlPolicy {
    const val APP_HOST = "nursego-athens.costaskaounas.chatgpt.site"

    fun opensInsideApp(url: String): Boolean =
        runCatching {
            val uri = URI(url)
            uri.scheme.equals("https", ignoreCase = true) &&
                uri.host.equals(APP_HOST, ignoreCase = true)
        }.getOrDefault(false)
}
