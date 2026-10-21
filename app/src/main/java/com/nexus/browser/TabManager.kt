package com.nexus.browser

import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.security.SecureRandom
import java.util.LinkedList
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.ConsoleMessage
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

private val TRACKER_DOMAINS = listOf(
    "doubleclick.net", "googlesyndication.com", "googleadservices.com", "google-analytics.com",
    "googletagmanager.com", "googletagservices.com", "adservice.google.com",
    "connect.facebook.net", "ads.facebook.com", "scorecardresearch.com", "outbrain.com",
    "taboola.com", "criteo.com", "criteo.net", "adnxs.com", "amazon-adsystem.com",
    "moatads.com", "quantserve.com", "hotjar.com", "mixpanel.com", "segment.io",
    "segment.com", "appsflyer.com", "branch.io", "mc.yandex.ru", "adsrvr.org",
    "pubmatic.com", "rubiconproject.com", "openx.net", "casalemedia.com",
    "bidswitch.net", "smartadserver.com", "adform.net"
)

private val IMAGE_EXT_REGEX = Regex("\\.(png|jpe?g|gif|webp|bmp)(\\?|$)", RegexOption.IGNORE_CASE)

private const val FINGERPRINT_JS = """
(function(){
  try {
    if (window.__nexusFpHooked) return; window.__nexusFpHooked = true;
    var notify = function(t){ try { if (window.NexusFp) NexusFp.report(t); } catch(e){} };
    if (window.HTMLCanvasElement) {
      var o1 = HTMLCanvasElement.prototype.toDataURL;
      HTMLCanvasElement.prototype.toDataURL = function(){ notify('canvas'); return o1.apply(this, arguments); };
    }
    if (window.CanvasRenderingContext2D) {
      var o2 = CanvasRenderingContext2D.prototype.getImageData;
      CanvasRenderingContext2D.prototype.getImageData = function(){ notify('canvas'); return o2.apply(this, arguments); };
    }
    var AC = window.AudioContext || window.webkitAudioContext;
    if (AC) {
      var o3 = AC.prototype.createAnalyser;
      AC.prototype.createAnalyser = function(){ notify('audio'); return o3.apply(this, arguments); };
    }
  } catch(e) {}
})();
"""

class TabInfo(val id: String, val isPrivate: Boolean, val webView: WebView) {
    var title: String = "Yeni sekme"
    var url: String? = null
    var readerMode: Boolean = false
}

/**
 * Tüm sekme (tab) yönetimini tek sınıf altında toplar.
 * MainActivity'den WebView oluşturma, gösterme/gizleme, geçiş ve dolaşım işlemlerini alır.
 */
class TabManager(
    private val context: Context,
    private val contentContainer: FrameLayout,
    private val topBar: WebView,
    private val overlay: WebView,
    private val settingsManager: SettingsManager,
    private val downloadController: DownloadController,
    private val webViewController: WebViewController
) {
    val tabs = LinkedHashMap<String, TabInfo>()
    var activeTabId: String? = null
    private var tabCounter = 0
    var splitSecondId: String? = null
    private val recentlyClosed = LinkedList<Map<String, String>>()

    // ---------- Sekme oluşturma ----------
    fun createTabInternal(isPrivate: Boolean): String {
        val id = "tab-" + (++tabCounter)
        val wv = WebView(context)
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.setSupportZoom(true)
        wv.settings.builtInZoomControls = true
        wv.settings.displayZoomControls = false
        if (isPrivate) {
            wv.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            wv.settings.saveFormData = false
        }
        applyPerfSettings(wv)
        wv.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        wv.visibility = View.GONE

        val tab = TabInfo(id, isPrivate, wv)
        tabs[id] = tab
        wv.webViewClient = buildContentWebViewClient(tab)
        wv.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                webViewController.addConsoleLog(message.message(), message.messageLevel().toString())
                return true
            }
        }
        contentContainer.addView(wv)
        wv.loadUrl("file:///android_asset/home/index.html")

        showOnlyTab(id)
        return id
    }

    // ---------- Gösterim ----------
    fun showOnlyTab(id: String) {
        activeTabId = id
        val sleepOn = settingsManager.getSettingsObj().optBoolean("tabSleep", false)
        tabs.values.forEach {
            val visible = it.id == id
            it.webView.visibility = if (visible) View.VISIBLE else View.GONE
            if (sleepOn) { if (visible) it.webView.onResume() else it.webView.onPause() } else it.webView.onResume()
            if (visible && it.readerMode) {
                webViewController.injectReaderMode(it.webView)
            }
        }
        // Tema her sekme geçişinde yeniden uygulanır (kararlılık için)
        applyTheme()
    }

    fun toggleReaderMode(tabId: String) {
        val tab = tabs[tabId] ?: return
        tab.readerMode = !tab.readerMode
        if (tab.readerMode) {
            webViewController.injectReaderMode(tab.webView)
        } else {
            tab.webView.evaluateJavascript("""
                (function(){
                    var s = document.querySelector('style[data-reader-mode]');
                    if (s) s.remove();
                })();
            """, null)
        }
    }

    fun getReaderMode(tabId: String): Boolean = tabs[tabId]?.readerMode ?: false

    fun closeTab(tabId: String) {
        val tab = tabs.remove(tabId) ?: return
        // Son kapatılanlara kaydet (maksimum 10)
        recentlyClosed.addFirst(mapOf(
            "id" to tabId,
            "title" to tab.title,
            "url" to (tab.url ?: ""),
            "private" to tab.isPrivate.toString()
        ))
        if (recentlyClosed.size > 10) recentlyClosed.removeLast()
        contentContainer.removeView(tab.webView)
        tab.webView.destroy()
        if (tabs.isEmpty()) {
            createTabInternal(false)
        } else if (activeTabId == tabId) {
            val lastId = tabs.keys.last()
            showOnlyTab(lastId)
            pushNavStateToTop(tabs[lastId]!!, tabs[lastId]!!.webView)
        }
    }

    fun getRecentlyClosedJson(): String {
        val out = JSONArray()
        recentlyClosed.forEach { entry ->
            out.put(JSONObject(entry))
        }
        return out.toString()
    }

    fun restoreRecentlyClosed(index: Int): String? {
        if (index < 0 || index >= recentlyClosed.size) return null
        val entry = recentlyClosed[index]
        val isPrivate = entry["private"] == "true"
        val newId = createTabInternal(isPrivate)
        val tab = tabs[newId]!!
        tab.title = entry["title"] ?: "Yeni sekme"
        val url = entry["url"] ?: ""
        if (url.isNotEmpty()) {
            tab.webView.loadUrl(url)
        }
        recentlyClosed.removeAt(index)
        return newId
    }

    fun switchTab(tabId: String) {
        if (tabs.containsKey(tabId)) {
            showOnlyTab(tabId)
            tabs[tabId]?.let { pushNavStateToTop(it, it.webView) }
        }
    }

    fun navigate(tabId: String, url: String) {
        tabs[tabId]?.webView?.loadUrl(url)
    }

    fun goBack(tabId: String) {
        tabs[tabId]?.webView?.let { if (it.canGoBack()) it.goBack() }
    }

    fun goForward(tabId: String) {
        tabs[tabId]?.webView?.let { if (it.canGoForward()) it.goForward() }
    }

    fun reload(tabId: String) {
        tabs[tabId]?.webView?.reload()
    }

    // ---------- Navigasyon durumunu üst şerite ilet ----------
    fun pushNavStateToTop(tab: TabInfo, view: WebView) {
        val displayUrl = if (tab.url?.startsWith("file:///android_asset/home") == true) "" else (tab.url ?: "")
        val js = "window.NexusTop && NexusTop.onNavigated(" +
            "${JSONObject.quote(tab.id)}, ${JSONObject.quote(displayUrl)}, ${JSONObject.quote(tab.title ?: "")}, ${view.canGoBack()}, ${view.canGoForward()})"
        topBar.evaluateJavascript(js, null)
    }

    // ---------- Faz 3: Performans modları ----------
    fun applyPerfSettings(wv: WebView) {
        val s = settingsManager.getSettingsObj()
        when (s.optString("perfMode", "dengeli")) {
            "veri" -> wv.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            "tasarruf" -> wv.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            else -> wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
        }
    }

    // ---------- Karanlık / Aydınlık / Sistem Oto mod ----------
    fun applyTheme() {
        val s = settingsManager.getSettingsObj()
        val theme = s.optString("theme", "light")
        val dark = when (theme) {
            "dark" -> true
            "system" -> isSystemDark()
            else -> false
        }
        val bg = if (dark) "#1e1e2e" else "#FFFFFF"
        val txt = if (dark) "#e6e6ea" else "#202133"
        val js = "document.documentElement.style.setProperty('--theme-dark', '${if (dark) "true" else "false"}'); " +
            "document.documentElement.style.setProperty('--theme-bg', '$bg'); " +
            "document.documentElement.style.setProperty('--theme-text', '$txt'); " +
            "document.documentElement.setAttribute('data-theme', '${if (dark) "true" else "false"}')"
        topBar.evaluateJavascript(js, null)
        overlay.evaluateJavascript(js, null)
        tabs.values.forEach { it.webView.evaluateJavascript(js, null) }
    }

    private fun isSystemDark(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        } else false
    }

    // ---------- Dışarıya açık ----------
    fun getOpenTabsJson(): String {
        val out = JSONArray()
        tabs.values.forEach { out.put(JSONObject().apply { put("id", it.id); put("title", it.title) }) }
        return out.toString()
    }

    private var privacyBlockedCount = 0
    fun getPrivacyCount(): Int = privacyBlockedCount

    // ---------- Faz 3: Önbellek sınırı ----------
    fun trimCacheIfNeeded() {
        val limitMB = settingsManager.getSettingsObj().optInt("cacheLimitMB", 0)
        if (limitMB <= 0) return
        try {
            if (settingsManager.getDirSize(context.cacheDir) > limitMB.toLong() * 1024 * 1024) {
                tabs.values.forEach { it.webView.clearCache(false) }
            }
        } catch (e: Exception) { }
    }

    // ---------- Dosya indirme yöneticisi (DownloadController'a delegate) ----------
    fun downloadFile(url: String, fileName: String) {
        downloadController.downloadFile(url, fileName)
    }

    // ---------- Şifre Oluşturucu ----------
    fun generatePassword(length: Int, useUpper: Boolean, useLower: Boolean, useDigits: Boolean, useSymbols: Boolean): String {
        val chars = buildString {
            if (useUpper) append("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
            if (useLower) append("abcdefghijklmnopqrstuvwxyz")
            if (useDigits) append("0123456789")
            if (useSymbols) append("!@#$%^&*()-_=+[]{}|;:,.<>?/~`")
        }
        if (chars.isEmpty()) return "abc123"
        val secureRandom = SecureRandom()
        val pw = buildString {
            for (i in 0 until length.coerceIn(4, 64)) {
                append(chars[secureRandom.nextInt(chars.length)])
            }
        }
        return pw
    }

    // ---------- Clipboard ----------
    fun copyToClipboard(text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Nexus Browser", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Panoya kopyalandı", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Kopyalama başarısız: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private val CONTENT_RESOLVER by lazy { context.contentResolver }

    // ---------- WebViewClient (geçmiş,PDF indirme, engelleme, parmak izi) ----------
    fun buildContentWebViewClient(tab: TabInfo): WebViewClient = object : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val url = request.url.toString()
            val lower = url.lowercase()
            val httpsOnly = settingsManager.getSettingsObj().optBoolean("httpsOnly", false)
            if (httpsOnly && url.startsWith("http://")) {
                view.loadUrl(url.replaceFirst("http://", "https://"))
                return true
            }
            val downloadExts = listOf("zip", "apk", "exe", "dmg", "iso", "tar", "gz", "rar", "7z", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "json", "xml", "mp3", "mp4", "avi", "mkv", "mov", "wmv", "flac", "wav", "ogg", "webp", "ico", "svg")
            if (downloadExts.any { ext -> lower.endsWith(".$ext") || lower.endsWith(".$ext?") || lower.endsWith(".$ext#") }) {
                val fileName = url.substringAfterLast("/").takeIf { it.isNotEmpty() && it.length > 1 } ?: "nexus_download"
                downloadFile(url, fileName)
                return true
            }
            if (url.lowercase().endsWith(".pdf")) {
                try {
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(url), "application/pdf")
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    return true
                } catch (e: Exception) {
                    Toast.makeText(context, "PDF açacak bir uygulama bulunamadı", Toast.LENGTH_SHORT).show()
                }
            }
            return false
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            try {
                if (!request.isForMainFrame) {
                    val s = settingsManager.getSettingsObj()
                    val blocking = s.optBoolean("adBlock", true) || s.optBoolean("trackerBlock", true)
                    val host = request.url.host ?: ""
                    if (blocking && TRACKER_DOMAINS.any { host == it || host.endsWith(".$it") }) {
                        privacyBlockedCount++
                        topBar.evaluateJavascript("window.NexusTop && NexusTop.onPrivacyUpdate($privacyBlockedCount)", null)
                        return WebResourceResponse("text/plain", "utf-8", null)
                    }
                    val imgLimitMB = s.optInt("imageLimitMB", 0)
                    if (imgLimitMB > 0) {
                        val path = request.url.path ?: ""
                        val looksImage = IMAGE_EXT_REGEX.containsMatchIn(path) ||
                            (request.requestHeaders["Accept"]?.contains("image") == true)
                        if (looksImage) {
                            try {
                                val conn = java.net.URL(request.url.toString()).openConnection() as java.net.HttpURLConnection
                                conn.requestMethod = "HEAD"
                                conn.connectTimeout = 3000
                                conn.connect()
                                val len = conn.contentLengthLong
                                conn.disconnect()
                                if (len > imgLimitMB.toLong() * 1024 * 1024) {
                                    return WebResourceResponse("image/png", "utf-8", null)
                                }
                            } catch (e: Exception) { /* HEAD desteklenmiyorsa sınırlama atlanır */ }
                        }
                    }
                }
            } catch (e: Exception) { }
            return super.shouldInterceptRequest(view, request)
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            view.evaluateJavascript(FINGERPRINT_JS, null)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            tab.url = url
            tab.title = view.title ?: url ?: "Yeni sekme"
            if (!tab.isPrivate && url != null && !url.startsWith("file:///android_asset/home")) {
                settingsManager.addHistoryEntry(tab.title, url)
            }
            pushNavStateToTop(tab, view)
            trimCacheIfNeeded()

            // ---- Yerel arama sayfası tespiti: DuckDuckGo'ya yönlendir ----
            if (url != null && url.startsWith("file:///android_asset/search/")) {
                val q = android.net.Uri.parse(url).getQueryParameter("q")
                if (q.isNullOrBlank()) return
                val encodedQ = java.net.URLEncoder.encode(q, "UTF-8")
                view.post { view.loadUrl("https://html.duckduckgo.com/html/?q=$encodedQ") }
                return
            }

            // ---- DuckDuckGo tüm branding'ini gizle (sadece Nexus görünsün) ----
            if (url != null && url.contains("duckduckgo.com")) {
                view.post {
                    view.evaluateJavascript("""
                        (function(){
                            var s = document.createElement('style');
                            s.innerHTML = '#logo_homepage_link, .logo, .result__logo, [class*="logo"], .header__logo, #logo, .duckduckgo-logo, .header__logo__wordmark, .header__logo__tagline, #logo_wrapper, .header__logo, .logo-link, header a[href*="duckduckgo"], #logo_homepage, #logo_homepage_link, .result--logo, .logo__homepage, .web-result__logo, .logo--homepage, .header__logo__image, .logo__image, #logo_img { display: none !important; visibility: hidden !important; opacity: 0 !important; width: 0 !important; height: 0 !important; }';
                            s.setAttribute('data-nexus-hide-ddg', 'true');
                            document.head.appendChild(s);
                        })();
                    """, null)
                }
            }
        }
    }
}
