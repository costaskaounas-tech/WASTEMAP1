package gr.koukamedics.dispatcher

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.TrustedWebUtils

/**
 * Launches DISPATCHER as a Trusted Web Activity (TWA).
 *
 * The live site uses the platform's "Sign in with ChatGPT" flow. A TWA shares the
 * browser's authenticated session, unlike an Android WebView, so Google/ChatGPT
 * sign-in returns to the application correctly. If the browser cannot validate the
 * site association, Android opens the same page as a normal Custom Tab instead.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) launchDispatcher(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchDispatcher(intent)
    }

    private fun launchDispatcher(sourceIntent: Intent) {
        val startUri = sourceIntent.data
            ?.takeIf { it.scheme.equals("https", ignoreCase = true) && it.host == APP_HOST }
            ?: Uri.parse(getString(R.string.app_url))

        val customTabsIntent = CustomTabsIntent.Builder().build()
        try {
            TrustedWebUtils.launchAsTrustedWebActivity(this, customTabsIntent, startUri)
            finish()
        } catch (_: ActivityNotFoundException) {
            launchBrowserFallback(startUri)
        } catch (_: SecurityException) {
            launchBrowserFallback(startUri)
        }
    }

    private fun launchBrowserFallback(uri: Uri) {
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE),
            )
            finish()
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_compatible_browser, Toast.LENGTH_LONG).show()
        }
    }

    private companion object {
        const val APP_HOST = "nursego-athens.costaskaounas.chatgpt.site"
    }
}
