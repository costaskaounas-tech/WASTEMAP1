package gr.koukamedics.dispatcher

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.trusted.TrustedWebActivityIntentBuilder

/**
 * Launches DISPATCHER as a Trusted Web Activity (TWA).
 *
 * The live site uses the platform's "Sign in with ChatGPT" flow. A TWA uses the
 * browser's authenticated session, unlike an Android WebView, so Google/ChatGPT
 * sign-in returns to the site correctly. If a TWA provider is unavailable,
 * Android opens the same page in the installed browser.
 */
class MainActivity : ComponentActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var serviceConnection: CustomTabsServiceConnection? = null
    private var launchUri: Uri? = null
    private var launchStarted = false

    private val browserFallback = Runnable {
        if (!launchStarted) {
            val uri = launchUri ?: return@Runnable
            launchStarted = true
            disconnectCustomTabs()
            launchBrowserFallback(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchDispatcher(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!launchStarted) launchDispatcher(intent)
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(browserFallback)
        disconnectCustomTabs()
        super.onDestroy()
    }

    private fun launchDispatcher(sourceIntent: Intent) {
        if (launchStarted || serviceConnection != null) return

        val uri = sourceIntent.data
            ?.takeIf { it.scheme.equals("https", ignoreCase = true) && it.host == APP_HOST }
            ?: Uri.parse(getString(R.string.app_url))
        launchUri = uri

        val providerPackage = CustomTabsClient.getPackageName(this, null)
        if (providerPackage == null) {
            launchStarted = true
            launchBrowserFallback(uri)
            return
        }

        val connection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(
                name: ComponentName,
                client: CustomTabsClient,
            ) {
                if (launchStarted) return

                client.warmup(0L)
                val session = client.newSession(null)
                if (session == null) {
                    browserFallback.run()
                    return
                }

                launchStarted = true
                mainHandler.removeCallbacks(browserFallback)
                try {
                    TrustedWebActivityIntentBuilder(uri)
                        .build(session)
                        .launchTrustedWebActivity(this@MainActivity)
                    finish()
                } catch (_: ActivityNotFoundException) {
                    launchBrowserFallback(uri)
                } catch (_: SecurityException) {
                    launchBrowserFallback(uri)
                } catch (_: IllegalArgumentException) {
                    launchBrowserFallback(uri)
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                if (!launchStarted) browserFallback.run()
            }
        }

        serviceConnection = connection
        if (CustomTabsClient.bindCustomTabsService(this, providerPackage, connection)) {
            mainHandler.postDelayed(browserFallback, CUSTOM_TABS_TIMEOUT_MS)
        } else {
            launchStarted = true
            disconnectCustomTabs()
            launchBrowserFallback(uri)
        }
    }

    private fun disconnectCustomTabs() {
        serviceConnection?.let { connection ->
            runCatching { unbindService(connection) }
        }
        serviceConnection = null
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
        const val CUSTOM_TABS_TIMEOUT_MS = 3_000L
    }
}
