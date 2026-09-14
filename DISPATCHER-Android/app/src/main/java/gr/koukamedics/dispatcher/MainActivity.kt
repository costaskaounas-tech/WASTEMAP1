package gr.koukamedics.dispatcher

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.view.View
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorPanel: View
    private var mainFrameFailed = false
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null
    private var filePickerPending = false
    private val popupViews = mutableSetOf<WebView>()

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = fileChooserCallback
            fileChooserCallback = null
            filePickerPending = false
            // Never expose a private/local file URI or return a selection after navigation.
            val selected = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
                ?.filter { uri ->
                    uri.scheme == "content" && !uri.authority.isNullOrBlank() &&
                        uri.authority != packageName &&
                        !uri.authority.orEmpty().startsWith("$packageName.") &&
                        checkUriPermission(
                            uri, android.os.Process.myPid(), android.os.Process.myUid(),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }?.toTypedArray()?.takeIf { it.isNotEmpty() }
            callback?.onReceiveValue(
                if (::webView.isInitialized && UrlPolicy.opensInsideApp(webView.url.orEmpty())) selected else null,
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { view, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or
                    WindowInsetsCompat.Type.ime(),
            )
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }

        webView = findViewById(R.id.web_view)
        progressBar = findViewById(R.id.loading_progress)
        errorPanel = findViewById(R.id.error_panel)

        configureWebView()

        findViewById<Button>(R.id.retry_button).setOnClickListener {
            errorPanel.visibility = View.GONE
            mainFrameFailed = false
            if (webView.url.isNullOrBlank()) {
                webView.loadUrl(getString(R.string.app_url))
            } else {
                webView.reload()
            }
        }

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(getString(R.string.app_url))
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        // Keep this callback enabled when the task is reopened from Recents.
                        if (!moveTaskToBack(true)) finish()
                    }
                }
            },
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setAcceptThirdPartyCookies(webView, true)
            }
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadsImagesAutomatically = true
            mediaPlaybackRequiresUserGesture = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            allowFileAccess = false
            allowContentAccess = true
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
            setSupportMultipleWindows(true)
            userAgentString = "$userAgentString DISPATCHER-Android/1.0"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
        }

        webView.webViewClient =
            object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    if (!request.isForMainFrame) {
                        return !UrlPolicy.isWebUrl(request.url.toString()) && request.url.toString() != "about:blank"
                    }
                    return routeNavigation(request.url, request.hasGesture())
                }

                @Deprecated("Used for compatibility with older WebView implementations")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                    routeNavigation(Uri.parse(url), false)

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    fileChooserCallback?.onReceiveValue(null)
                    fileChooserCallback = null
                    if (!UrlPolicy.opensInsideApp(url)) {
                        view.stopLoading()
                        showMainFrameError()
                        return
                    }
                    mainFrameFailed = false
                    errorPanel.visibility = View.GONE
                    progressBar.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView, url: String) {
                    progressBar.visibility = View.GONE
                    if (!mainFrameFailed) {
                        errorPanel.visibility = View.GONE
                    }
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    if (request.isForMainFrame) showMainFrameError()
                }

                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    errorResponse: WebResourceResponse,
                ) {
                    if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                        showMainFrameError()
                    }
                }
            }

        webView.webChromeClient =
            object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    progressBar.progress = newProgress
                    progressBar.visibility =
                        if (newProgress in 0..99) View.VISIBLE else View.GONE
                }

                override fun onShowFileChooser(
                    webView: WebView,
                    filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: FileChooserParams,
                ): Boolean {
                    if (!UrlPolicy.opensInsideApp(webView.url.orEmpty()) || filePickerPending ||
                        fileChooserParams.mode !in setOf(FileChooserParams.MODE_OPEN, FileChooserParams.MODE_OPEN_MULTIPLE)) {
                        filePathCallback.onReceiveValue(null)
                        return true
                    }
                    this@MainActivity.fileChooserCallback = filePathCallback
                    filePickerPending = true

                    return try {
                        // System document selection only; no camera or broad storage permission.
                        val picker = fileChooserParams.createIntent().apply {
                            action = Intent.ACTION_OPEN_DOCUMENT
                            addCategory(Intent.CATEGORY_OPENABLE)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
                        }
                        fileChooserLauncher.launch(picker)
                        true
                    } catch (_: ActivityNotFoundException) {
                        this@MainActivity.fileChooserCallback = null
                        filePickerPending = false
                        filePathCallback.onReceiveValue(null)
                        Toast.makeText(
                            this@MainActivity,
                            R.string.no_file_picker,
                            Toast.LENGTH_LONG,
                        ).show()
                        true
                    } catch (_: SecurityException) {
                        this@MainActivity.fileChooserCallback = null
                        filePickerPending = false
                        filePathCallback.onReceiveValue(null)
                        showNoHandlerMessage()
                        true
                    }
                }

                override fun onCreateWindow(
                    view: WebView,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message,
                ): Boolean {
                    if (!isUserGesture || !UrlPolicy.opensInsideApp(view.url.orEmpty())) return false

                    val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
                    val popup = WebView(this@MainActivity)
                    popupViews.add(popup)
                    popup.webViewClient =
                        object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean {
                                if (request.isForMainFrame) routePopup(request.url)
                                releasePopup(view)
                                return true
                            }

                            @Deprecated("Used for compatibility with older WebView implementations")
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                url: String,
                            ): Boolean {
                                routePopup(Uri.parse(url))
                                releasePopup(view)
                                return true
                            }
                        }
                    transport.webView = popup
                    resultMsg.sendToTarget()
                    webView.postDelayed({ releasePopup(popup) }, 10_000)
                    return true
                }
            }

        webView.setDownloadListener { url, _, _, _, _ ->
            openExternal(Uri.parse(url))
        }
    }

    private fun routeNavigation(uri: Uri, userGesture: Boolean): Boolean {
        if (UrlPolicy.opensInsideApp(uri.toString())) return false
        if (UrlPolicy.isWebUrl(uri.toString())) {
            openExternal(uri)
        } else if (!userGesture || !UrlPolicy.opensInsideApp(webView.url.orEmpty())) {
            return true
        } else if (uri.scheme.equals("intent", true)) {
            openIntentUri(uri)
        } else {
            openExternal(uri)
        }
        return true
    }

    private fun releasePopup(popup: WebView) {
        if (popupViews.remove(popup)) {
            popup.stopLoading()
            webView.post { popup.destroy() }
        }
    }

    private fun routePopup(uri: Uri) {
        if (UrlPolicy.opensInsideApp(uri.toString())) {
            webView.loadUrl(uri.toString())
        } else if (uri.scheme.equals("intent", ignoreCase = true)) {
            openIntentUri(uri)
        } else {
            openExternal(uri)
        }
    }

    private fun openIntentUri(uri: Uri) {
        val parsedIntent =
            runCatching {
                Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
            }.getOrNull()

        if (parsedIntent == null) {
            showNoHandlerMessage()
            return
        }

        // Build a fresh implicit intent. Never forward incoming actions, components,
        // selectors, extras, packages, ClipData or URI permission flags from a website.
        val destination = parsedIntent.data
        if (destination != null && launchExternal(destination)) return
        val fallback = parsedIntent.getStringExtra("browser_fallback_url")
        if (fallback != null && UrlPolicy.opensInsideApp(fallback)) {
            webView.loadUrl(fallback)
        } else if (fallback != null && UrlPolicy.isWebUrl(fallback)) {
            openExternal(Uri.parse(fallback))
        } else {
            showNoHandlerMessage()
        }
    }

    private fun openExternal(uri: Uri) {
        if (!launchExternal(uri)) showNoHandlerMessage()
    }

    private fun launchExternal(uri: Uri): Boolean {
        if (!UrlPolicy.canOpenExternal(uri.toString())) return false
        val action = when (uri.scheme?.lowercase(java.util.Locale.ROOT)) {
            "tel" -> Intent.ACTION_DIAL
            "mailto", "sms", "smsto" -> Intent.ACTION_SENDTO
            else -> Intent.ACTION_VIEW
        }
        val intent = Intent(action, uri).apply { addCategory(Intent.CATEGORY_BROWSABLE) }
        return try {
            startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun showNoHandlerMessage() {
        Toast.makeText(this, R.string.no_app_for_link, Toast.LENGTH_LONG).show()
    }

    private fun showMainFrameError() {
        mainFrameFailed = true
        progressBar.visibility = View.GONE
        errorPanel.visibility = View.VISIBLE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onPause() {
        CookieManager.getInstance().flush()
        webView.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onDestroy() {
        fileChooserCallback?.onReceiveValue(null)
        fileChooserCallback = null
        popupViews.forEach { it.destroy() }
        popupViews.clear()
        webView.stopLoading()
        webView.removeAllViews()
        webView.destroy()
        super.onDestroy()
    }
}
