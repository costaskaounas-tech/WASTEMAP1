package gr.koukamedics.dispatcher

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.provider.Browser
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

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorPanel: View
    private var mainFrameFailed = false
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val callback = fileChooserCallback
            fileChooserCallback = null
            callback?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data),
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
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
                    if (!request.isForMainFrame) return false
                    return routeNavigation(request.url)
                }

                @Deprecated("Used for compatibility with older WebView implementations")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                    routeNavigation(Uri.parse(url))

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
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
                    this@MainActivity.fileChooserCallback?.onReceiveValue(null)
                    this@MainActivity.fileChooserCallback = filePathCallback

                    return try {
                        fileChooserLauncher.launch(fileChooserParams.createIntent())
                        true
                    } catch (_: ActivityNotFoundException) {
                        this@MainActivity.fileChooserCallback = null
                        filePathCallback.onReceiveValue(null)
                        Toast.makeText(
                            this@MainActivity,
                            R.string.no_file_picker,
                            Toast.LENGTH_LONG,
                        ).show()
                        true
                    }
                }

                override fun onCreateWindow(
                    view: WebView,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message,
                ): Boolean {
                    if (!isUserGesture) return false

                    val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
                    val popup = WebView(this@MainActivity)
                    popup.webViewClient =
                        object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean {
                                routePopup(request.url)
                                view.destroy()
                                return true
                            }

                            @Deprecated("Used for compatibility with older WebView implementations")
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                url: String,
                            ): Boolean {
                                routePopup(Uri.parse(url))
                                view.destroy()
                                return true
                            }
                        }
                    transport.webView = popup
                    resultMsg.sendToTarget()
                    return true
                }
            }

        webView.setDownloadListener { url, _, _, _, _ ->
            openExternal(Uri.parse(url))
        }
    }

    private fun routeNavigation(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()

        if (scheme in setOf("about", "blob", "data", "javascript")) return false
        if (UrlPolicy.opensInsideApp(uri.toString())) return false

        if (scheme == "intent") {
            openIntentUri(uri)
        } else {
            openExternal(uri)
        }
        return true
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
                Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    component = null
                    selector = null
                }
            }.getOrNull()

        if (parsedIntent == null) {
            showNoHandlerMessage()
            return
        }

        try {
            startActivity(parsedIntent)
        } catch (_: ActivityNotFoundException) {
            val fallback = parsedIntent.getStringExtra("browser_fallback_url")
            if (fallback.isNullOrBlank()) {
                showNoHandlerMessage()
            } else if (UrlPolicy.opensInsideApp(fallback)) {
                webView.loadUrl(fallback)
            } else {
                openExternal(Uri.parse(fallback))
            }
        }
    }

    private fun openExternal(uri: Uri) {
        val intent =
            Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                putExtra(Browser.EXTRA_APPLICATION_ID, packageName)
            }

        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            showNoHandlerMessage()
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
        webView.stopLoading()
        webView.removeAllViews()
        webView.destroy()
        super.onDestroy()
    }
}
