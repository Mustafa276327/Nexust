package com.nexus.browser

import android.app.Activity
import android.app.DownloadManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/* =========================================================
   NEXUS BROWSER - Android (native WebView tabanlı)
   Faz 1 + Faz 2 + Faz 3 + Faz 4 (NEXUS köprüsü dahil)
   ========================================================= */

private const val APP_VERSION_CODE = 3

class MainActivity : Activity() {

    companion object {
        private const val NEXUS_KANAL = "nexus_bridge"
        private const val NEXUS_KANAL_YUKSEK = "nexus_bridge_yuksek"
    }


    // =========================================================
    // NEXUS KÖPRÜSÜ (Companion App ↔ NEXUS web panel)
    // - Periyodik cihaz bilgisi: /api/cihaz_bilgisi (X-Token ile)
    // - Bildirim polling: /api/bildirimler → gerçek Android bildirimi
    // =========================================================

    private fun nexusKanallariOlustur() {
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(NEXUS_KANAL, "NEXUS Bildirimleri", NotificationManager.IMPORTANCE_DEFAULT))
            nm.createNotificationChannel(NotificationChannel(NEXUS_KANAL_YUKSEK, "NEXUS Bildirimleri (Yüksek)", NotificationManager.IMPORTANCE_HIGH))
        } catch (e: Exception) { }
        // Android 13+ bildirim izni
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            try { requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001) } catch (e: Exception) { }
        }
    }

    private fun nexusAyar(): Pair<String, String> {
        val s = settingsManager.getSettingsObj()
        val url = s.optString("nexusUrl", "http://127.0.0.1:8080").trim().trimEnd('/')
        val token = s.optString("nexusToken", "").trim()
        return Pair(url, token)
    }

    private fun nexusPost(url: String, token: String, path: String, body: String): Int {
        val conn = URL(url + path).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("X-Token", token)
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 6000
        conn.readTimeout = 6000
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        conn.disconnect()
        return code
    }

    private fun nexusCihazBilgisiGonder(url: String, token: String) {
        try {
            var pilYuzde = -1
            var sarj = false
            try {
                val bi = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                if (bi != null) {
                    val level = bi.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = bi.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                    if (level >= 0 && scale > 0) pilYuzde = level * 100 / scale
                    val durum = bi.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    sarj = durum == BatteryManager.BATTERY_STATUS_CHARGING || durum == BatteryManager.BATTERY_STATUS_FULL
                }
            } catch (e: Exception) { }
            var bosMb = -1L
            try {
                val st = android.os.StatFs(Environment.getDataDirectory().path)
                bosMb = st.availableBytes / (1024 * 1024)
            } catch (e: Exception) { }
            val govde = JSONObject().apply {
                put("pil_yuzde", pilYuzde)
                put("sarj_oluyor", sarj)
                put("model", Build.MODEL ?: "?")
                put("android_surumu", Build.VERSION.RELEASE ?: "?")
                put("bos_depolama_mb", bosMb)
            }.toString()
            nexusPost(url, token, "/api/cihaz_bilgisi", govde)
        } catch (e: Exception) { /* sessizce yoksay — köprü asla çökmemeli */ }
    }

    private fun nexusBildirimGoster(id: Int, baslik: String, icerik: String, oncelik: String) {
        try {
            val nm = getSystemService(NotificationManager::class.java)
            val kanal = if (oncelik == "yuksek") NEXUS_KANAL_YUKSEK else NEXUS_KANAL
            val n = Notification.Builder(this, kanal)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(baslik.ifBlank { "NEXUS" })
                .setContentText(icerik)
                .setStyle(Notification.BigTextStyle().bigText(icerik))
                .setAutoCancel(true)
                .build()
            nm.notify(10000 + id, n)
        } catch (e: Exception) { }
    }

    private fun nexusBildirimleriCek(url: String, token: String) {
        val conn = URL("$url/api/bildirimler").openConnection() as HttpURLConnection
        conn.setRequestProperty("X-Token", token)
        conn.connectTimeout = 6000
        conn.readTimeout = 6000
        val code = conn.responseCode
        if (code != 200) { conn.disconnect(); return }
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        val arr = try { JSONObject(text).getJSONArray("bildirimler") } catch (e: Exception) { return }
        if (arr.length() == 0) return
        val ids = JSONArray()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            ids.put(o.optInt("id"))
            nexusBildirimGoster(
                o.optInt("id"),
                o.optString("baslik", "NEXUS"),
                o.optString("icerik", ""),
                o.optString("oncelik", "normal")
            )
        }
        // Gösterilenleri onayla — tekrar tekrar gösterilmesin
        nexusPost(url, token, "/api/bildirimler_onayla", JSONObject().apply { put("ids", ids) }.toString())
    }

    private fun nexusKopruDongusu() {
        var adim = 0
        while (true) {
            try {
                val s = settingsManager.getSettingsObj()
                if (s.optBoolean("nexusSync", true)) {
                    val (url, token) = nexusAyar()
                    if (token.isNotBlank()) {
                        if (adim % 6 == 0) nexusCihazBilgisiGonder(url, token)
                        nexusBildirimleriCek(url, token)
                    }
                }
            } catch (e: Exception) { /* köprü hataları uygulamayı etkilememeli */ }
            adim++
            try { Thread.sleep(30_000) } catch (e: InterruptedException) { return }
        }
    }


    private lateinit var topBar: WebView
    private lateinit var overlay: WebView
    private lateinit var contentContainer: FrameLayout

    private val prefs by lazy { getSharedPreferences("nexus_prefs", MODE_PRIVATE) }
    private val settingsManager by lazy { SettingsManager(prefs, this) }
    private val downloadController by lazy { DownloadController(this) }
    private val securityController by lazy { SecurityController(this) }
    private val webViewController by lazy { WebViewController(this) }
    private val tabManager by lazy { TabManager(this, contentContainer, topBar, overlay, settingsManager, downloadController, webViewController) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        topBar = findViewById(R.id.topBarWebView)
        overlay = findViewById(R.id.overlayWebView)
        contentContainer = findViewById(R.id.contentContainer)

        setupChromeWebView(topBar, "file:///android_asset/top/index.html")
        setupChromeWebView(overlay, "file:///android_asset/overlay/index.html")
        // İlk sekme top/js/renderer.js tarafından AndroidHost.createTab(false) ile açılır.

        tabManager.applyTheme()

        // NEXUS köprüsü: bildirim kanalları + arka plan döngüsü
        nexusKanallariOlustur()
        Thread { nexusKopruDongusu() }.apply { isDaemon = true }.start()
    }

    private fun setupChromeWebView(wv: WebView, url: String) {
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.addJavascriptInterface(AndroidHost(), "AndroidHost")
        wv.webViewClient = WebViewClient()
        wv.setBackgroundColor(Color.WHITE)
        wv.loadUrl(url)
    }

    // ---------- QR Kod Tarayıcı Sonucu ----------
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1002 && resultCode == Activity.RESULT_OK && data != null) {
            val contents = data.getStringExtra("SCAN_RESULT")
            val format = data.getStringExtra("SCAN_RESULT_FORMAT")
            if (contents?.isNotBlank() == true) {
                val tab = tabManager.activeTabId?.let { tabManager.tabs[it] }
                tab?.webView?.loadUrl(contents ?: "")
                Toast.makeText(this@MainActivity, "QR kod algılandı: $contents", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------- Geri tuşu ----------
    override fun onBackPressed() {
        if (overlay.visibility == View.VISIBLE) { overlay.visibility = View.GONE; return }
        val tab = tabManager.activeTabId?.let { tabManager.tabs[it] }
        if (tab != null && tab.webView.canGoBack()) { tab.webView.goBack(); return }
        moveTaskToBack(true)
    }

    // =========================================================
    // AndroidHost — JavaScript ←> Kotlin köprüsü
    // =========================================================

    private inner class AndroidHost {

        // ---- Sekmeler ----
        @JavascriptInterface
        fun createTab(isPrivate: Boolean): String {
            var id = ""
            runOnUiThread { id = tabManager.createTabInternal(isPrivate) }
            var waited = 0
            while (id == "" && waited < 200) { Thread.sleep(5); waited++ }
            return id
        }

        @JavascriptInterface
        fun closeTab(tabId: String) {
            runOnUiThread { tabManager.closeTab(tabId) }
        }

        @JavascriptInterface
        fun switchTab(tabId: String) {
            runOnUiThread { tabManager.switchTab(tabId) }
        }

        @JavascriptInterface
        fun navigate(tabId: String, url: String) { runOnUiThread { tabManager.navigate(tabId, url) } }

        @JavascriptInterface
        fun goBack(tabId: String) { runOnUiThread { tabManager.goBack(tabId) } }

        @JavascriptInterface
        fun goForward(tabId: String) { runOnUiThread { tabManager.goForward(tabId) } }

        @JavascriptInterface
        fun reload(tabId: String) { runOnUiThread { tabManager.reload(tabId) } }

        // ---- Paneller ----
        @JavascriptInterface
        fun showPanel(name: String) {
            runOnUiThread {
                overlay.evaluateJavascript("window.NexusOverlay && NexusOverlay.open(${jsStr(name)})", null)
                overlay.visibility = View.VISIBLE
            }
        }
        @JavascriptInterface
        fun closePanel() { runOnUiThread { overlay.visibility = View.GONE } }

        // ---- Geçmiş ----
        @JavascriptInterface
        fun getHistoryJson(): String = settingsManager.getHistoryArr().toString()
        @JavascriptInterface
        fun clearHistory() { settingsManager.clearHistory() }

        // ---- Yer imleri ----
        @JavascriptInterface
        fun getBookmarksJson(): String = settingsManager.getBookmarksArr().toString()
        @JavascriptInterface
        fun toggleBookmark(title: String, url: String): Boolean = settingsManager.toggleBookmark(title, url)
        @JavascriptInterface
        fun isBookmarked(url: String): Boolean = settingsManager.isBookmarked(url)

        // ---- Ayarlar ----
        @JavascriptInterface
        fun getSettingsJson(): String = settingsManager.getSettingsObj().toString()
        @JavascriptInterface
        fun setSetting(key: String, jsonValue: String) {
            val obj = settingsManager.getSettingsObj()
            obj.put(key, JSONTokener(jsonValue).nextValue())
            settingsManager.saveSettingsObj(obj)
            if (key == "perfMode" || key == "tabSleep") {
                runOnUiThread { tabManager.tabs.values.forEach { tabManager.applyPerfSettings(it.webView) } }
            }
            if (key == "accentColor") {
                val hex = obj.optString("accentColor", "#5b5bf0")
                val setCss = "document.documentElement.style.setProperty('--accent', ${jsStr(hex)})"
                runOnUiThread { topBar.evaluateJavascript(setCss, null); overlay.evaluateJavascript(setCss, null) }
            }
            if (key == "theme" || key == "darkMode") {
                runOnUiThread { tabManager.applyTheme() }
            }
        }

        // ---- Şifreler ----
        @JavascriptInterface
        fun getPasswordsJson(): String {
            val arr = settingsManager.getPasswordsArr()
            val out = JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val plain = securityController.decryptPassword(o.optString("secret"), o.optString("iv"))
                out.put(JSONObject().apply {
                    put("id", o.optString("id")); put("site", o.optString("site"))
                    put("username", o.optString("username")); put("password", plain)
                    put("category", o.optString("category", "Diğer"))
                })
            }
            return out.toString()
        }
        @JavascriptInterface
        fun savePassword(site: String, username: String, password: String, category: String) {
            val pair = securityController.encryptPassword(password)
            val arr = settingsManager.getPasswordsArr()
            arr.put(JSONObject().apply {
                put("id", "pw-" + UUID.randomUUID().toString().take(8))
                put("site", site); put("username", username); put("secret", pair.first); put("iv", pair.second)
                put("category", category)
            })
            settingsManager.savePasswordsArr(arr)
        }
        @JavascriptInterface
        fun deletePassword(id: String) { settingsManager.deletePassword(id) }

        // ---- Faz 4: Sızıntı kontrolü ----
        @JavascriptInterface
        fun checkBreachesAsync() {
            Thread {
                val arr = settingsManager.getPasswordsArr()
                val results = JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val plain = securityController.decryptPassword(o.optString("secret"), o.optString("iv"))
                    val breached = try { securityController.isPasswordBreached(plain) } catch (e: Exception) { null }
                    results.put(JSONObject().apply {
                        put("id", o.optString("id")); put("site", o.optString("site"))
                        put("breached", breached ?: JSONObject.NULL)
                    })
                }
                runOnUiThread { overlay.evaluateJavascript("window.NexusOverlay && NexusOverlay.onBreachResults(${jsStr(results.toString())})", null) }
            }.start()
        }

        // ---- Gizlilik panosu ----
        @JavascriptInterface
        fun getPrivacyCount(): Int = tabManager.getPrivacyCount()

        // ---- Faz 3: Akıllı arama önerileri (yerel, AI'sız) ----
        @JavascriptInterface
        fun getSmartSuggestionsJson(query: String): String {
            if (query.isBlank()) return "[]"
            val q = query.lowercase()
            val out = JSONArray()
            val hist = settingsManager.getHistoryArr()
            for (i in 0 until hist.length()) {
                if (out.length() >= 4) break
                val o = hist.getJSONObject(i)
                if (o.optString("title").lowercase().contains(q) || o.optString("url").lowercase().contains(q)) out.put(o)
            }
            val bm = settingsManager.getBookmarksArr()
            for (i in 0 until bm.length()) {
                if (out.length() >= 6) break
                val o = bm.getJSONObject(i)
                if (o.optString("title").lowercase().contains(q) || o.optString("url").lowercase().contains(q)) out.put(o)
            }
            return out.toString()
        }

        // ---- Faz 3: AI sekme gruplama ----
        @JavascriptInterface
        fun groupTabsAI(tabsJson: String) {
            val s = settingsManager.getSettingsObj()
            val apiKey = s.optString("groqApiKey", "")
            if (apiKey.isBlank()) {
                runOnUiThread { topBar.evaluateJavascript("window.NexusTop && NexusTop.onGroupingResult(${jsStr("NOKEY")})", null) }
                return
            }
            val model = s.optString("groqModel", "llama-3.3-70b-versatile")
            Thread {
                val sys = "Sekme başlıklarını/URL'lerini konularına göre grupla. SADECE geçerli JSON döndür, başka hiçbir açıklama ekleme: " +
                    "{\"gruplar\":[{\"isim\":\"kısa grup adı\",\"sekme_id\":[\"tab-1\",\"tab-2\"]}]}"
                val result = securityController.callGroq(apiKey, model, sys, tabsJson)
                runOnUiThread { overlay.evaluateJavascript("window.NexusOverlay && NexusOverlay.onGroupingResult(${jsStr(result)})", null) }
            }.start()
        }

        // ---- Faz 5: Google Custom Search ----
        @JavascriptInterface
        fun performSearch(tabId: String, query: String) {
            val s = settingsManager.getSettingsObj()
            val key = s.optString("customSearchKey", "")
            val cx = s.optString("customSearchCx", "")
            if (key.isBlank() || cx.isBlank()) {
                runOnUiThread {
                    tabManager.tabs[tabId]?.webView?.evaluateJavascript("window.NexusSearch && NexusSearch.onResults(${jsStr("{\"items\":[]})")), null)
                }
                return
            }
            Thread {
                val result = try {
                    val q = java.net.URLEncoder.encode(query, "UTF-8")
                    val conn = URL("https://www.googleapis.com/customsearch/v1?key=$key&cx=$cx&q=$q").openConnection() as HttpURLConnection
                    conn.connectTimeout = 10000; conn.readTimeout = 15000
                    val code = conn.responseCode
                    val text = (if (code in 200..299) conn.inputStream else conn.errorStream).bufferedReader().use { it.readText() }
                    conn.disconnect()
                    if (code !in 200..299) "ERROR:$text" else text
                } catch (e: Exception) { "ERROR:${e.message}" }
                runOnUiThread {
                    tabManager.tabs[tabId]?.webView?.evaluateJavascript("window.NexusSearch && NexusSearch.onResults(${jsStr(result)})", null)
                }
            }.start()
        }

        // ---- Faz 5: Canlı arama önerileri (Google Suggest, anahtarsız) ----
        @JavascriptInterface
        fun fetchLiveSuggestions(query: String) {
            if (query.isBlank()) return
            Thread {
                try {
                    val q = java.net.URLEncoder.encode(query, "UTF-8")
                    val conn = URL("https://suggestqueries.google.com/complete/search?client=firefox&q=$q").openConnection() as HttpURLConnection
                    conn.connectTimeout = 4000; conn.readTimeout = 4000
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()
                    val arr = JSONArray(text)
                    val suggestions = if (arr.length() > 1) arr.getJSONArray(1) else JSONArray()
                    runOnUiThread {
                        topBar.evaluateJavascript("window.NexusTop && NexusTop.onLiveSuggestions(${jsStr(query)}, ${jsStr(suggestions.toString())})", null)
                    }
                } catch (e: Exception) { /* sessizce yoksay */ }
            }.start()
        }

        // ---- Aktif sekme id'si ----
        @JavascriptInterface
        fun getActiveTabId(): String = tabManager.activeTabId ?: ""

        // ---- Faz 4: AI sayfa özetleme ----
        @JavascriptInterface
        fun aiSummarizePage(tabId: String) {
            runOnUiThread {
                val tab = tabManager.tabs[tabId] ?: return@runOnUiThread
                tab.webView.evaluateJavascript(
                    "(function(){try{return document.body ? document.body.innerText.slice(0,4000) : '';}catch(e){return '';}})()"
                ) { rawResult ->
                    val pageText = try { (JSONTokener(rawResult).nextValue() as? String) ?: "" } catch (e: Exception) { "" }
                    val s = settingsManager.getSettingsObj()
                    val apiKey = s.optString("groqApiKey", "")
                    if (apiKey.isBlank()) {
                        overlay.evaluateJavascript("window.NexusOverlay && NexusOverlay.onSummaryResult(${jsStr("Groq API anahtarı ayarlanmamış. Menü > Ayarlar'dan ekleyin.")})", null)
                        return@evaluateJavascript
                    }
                    val model = s.optString("groqModel", "llama-3.3-70b-versatile")
                    val isHttps = tab.url?.startsWith("https://") == true
                    val urlSnapshot = tab.url ?: ""
                    val titleSnapshot = tab.title
                    Thread {
                        val sys = "Sen bir web güvenliği ve gizlilik analistisin. Kullanıcıya Türkçe, kısa ve maddeler halinde: " +
                            "1) Sitenin güvenilirliği, 2) Gizlilik/veri toplama riski, 3) Dolandırıcılık/kimlik avı belirtisi olup olmadığı, " +
                            "4) Bilinen şikayet/kötüye kullanım örüntülerine dair GENEL bilgin varsa belirt, canlı arama yapamadığını da söyle."
                        val usr = "Site: $urlSnapshot\nHTTPS: $isHttps\nBaşlık: $titleSnapshot\nSayfa metni (kısaltılmış):\n$pageText"
                        val result = securityController.callGroq(apiKey, model, sys, usr)
                        runOnUiThread { overlay.evaluateJavascript("window.NexusOverlay && NexusOverlay.onSummaryResult(${jsStr(result)})", null) }
                    }.start()
                }
            }
        }

        // ---- Faz 4: Ekran görüntüsü ----
        @JavascriptInterface
        fun takeScreenshot(tabId: String) {
            runOnUiThread {
                val wv = tabManager.tabs[tabId]?.webView ?: return@runOnUiThread
                if (wv.width == 0 || wv.height == 0) return@runOnUiThread
                val bmp = Bitmap.createBitmap(wv.width, wv.height, Bitmap.Config.ARGB_8888)
                wv.draw(Canvas(bmp))
                Thread {
                    try {
                        val filename = "Nexus_" + System.currentTimeMillis() + ".png"
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NexusBrowser")
                        }
                        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        if (uri != null) {
                            contentResolver.openOutputStream(uri)?.use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }
                            runOnUiThread { Toast.makeText(this@MainActivity, "Ekran görüntüsü kaydedildi (Resimler/NexusBrowser)", Toast.LENGTH_SHORT).show() }
                        }
                    } catch (e: Exception) {
                        runOnUiThread { Toast.makeText(this@MainActivity, "Ekran görüntüsü kaydedilemedi: ${e.message}", Toast.LENGTH_SHORT).show() }
                    }
                }.start()
            }
        }

        // ---- Faz 4: PDF olarak kaydet ----
        @JavascriptInterface
        fun printPageAsPdf(tabId: String) {
            runOnUiThread {
                val tab = tabManager.tabs[tabId] ?: return@runOnUiThread
                val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
                val jobName = "Nexus_" + tab.title.replace(Regex("[^A-Za-z0-9]+"), "_").take(40)
                val adapter = tab.webView.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, adapter, PrintAttributes.Builder().build())
            }
        }

        // ---- Faz 4: Bölünmüş ekran ----
        @JavascriptInterface
        fun enterSplitView(secondTabId: String) {
            runOnUiThread {
                val activeId = tabManager.activeTabId ?: return@runOnUiThread
                val topTab = tabManager.tabs[activeId] ?: return@runOnUiThread
                val bottomTab = tabManager.tabs[secondTabId] ?: return@runOnUiThread
                tabManager.splitSecondId = secondTabId
                contentContainer.post {
                    val h = contentContainer.height / 2
                    if (h <= 0) return@post
                    tabManager.tabs.values.forEach { it.webView.visibility = View.GONE }
                    topTab.webView.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, h)
                    bottomTab.webView.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, h).apply { topMargin = h }
                    topTab.webView.visibility = View.VISIBLE
                    bottomTab.webView.visibility = View.VISIBLE
                }
            }
        }
        @JavascriptInterface
        fun exitSplitView() {
            runOnUiThread {
                tabManager.splitSecondId = null
                tabManager.tabs.values.forEach { it.webView.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT) }
                tabManager.activeTabId?.let { tabManager.showOnlyTab(it) }
            }
        }
        @JavascriptInterface
        fun getOpenTabsJson(): String = tabManager.getOpenTabsJson()

        // ---- Geliştirici Konsolu ----
        @JavascriptInterface
        fun getConsoleLogsJson(): String = webViewController.getConsoleLogsJson()
        @JavascriptInterface
        fun clearConsoleLogs() { webViewController.clearConsoleLogs() }

        // ---- Çerez Yönetimi ----
        @JavascriptInterface
        fun getCookiesForUrl(url: String): String = webViewController.getCookiesForUrl(url)
        @JavascriptInterface
        fun deleteCookieForUrl(url: String, cookieName: String) { webViewController.deleteCookieForUrl(url, cookieName) }
        @JavascriptInterface
        fun deleteAllCookies() { webViewController.deleteAllCookies() }

        // ---- User-Agent ----
        @JavascriptInterface
        fun getCurrentUserAgent(tabId: String): String {
            return tabManager.tabs[tabId]?.webView?.let { webViewController.getDefaultUserAgent(it) } ?: ""
        }
        @JavascriptInterface
        fun setDesktopUserAgent(tabId: String) {
            tabManager.tabs[tabId]?.webView?.let { webViewController.setDesktopUserAgent(it) }
        }
        @JavascriptInterface
        fun setMobileUserAgent(tabId: String) {
            tabManager.tabs[tabId]?.webView?.let { webViewController.setMobileUserAgent(it) }
        }

        // ---- Reader Mode ----
        @JavascriptInterface
        fun toggleReaderMode(tabId: String) { tabManager.toggleReaderMode(tabId) }
        @JavascriptInterface
        fun getReaderMode(tabId: String): Boolean = tabManager.getReaderMode(tabId)

        // ---- Son Kapatılan Sekmeler ----
        @JavascriptInterface
        fun getRecentlyClosedJson(): String = tabManager.getRecentlyClosedJson()
        @JavascriptInterface
        fun restoreRecentlyClosed(index: Int): String = tabManager.restoreRecentlyClosed(index) ?: ""

        // ---- QR Kod Tarayıcı ----
        @JavascriptInterface
        fun scanQrCode() {
            runOnUiThread {
                val intent = Intent("com.google.zxing.client.android.SCAN").apply {
                    putExtra("SCAN_MODE", "QR_CODE_MODE")
                    setPackage("com.google.zxing.client.android")
                }
                try {
                    startActivityForResult(intent, 1002)
                } catch (e: Exception) {
                    // ZXing yok, genel kamera/qr aramasına yönlendir
                    val fallback = Intent(Intent.ACTION_VIEW).apply {
                        setData(Uri.parse("https://www.google.com/search?q=qr+kod+tarayici&udm=14"))
                    }
                    try {
                        startActivity(fallback)
                    } catch (e2: Exception) {
                        Toast.makeText(this@MainActivity, "QR tarayıcı uygulaması bulunamadı", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // ---- İndirme Yöneticisi (DownloadController'a delegate) ----
        @JavascriptInterface
        fun getDownloadsJson(): String = downloadController.getDownloadsJson()
        @JavascriptInterface
        fun openDownload(downloadId: Long) { runOnUiThread { downloadController.openDownload(downloadId) } }
        @JavascriptInterface
        fun deleteDownload(downloadId: Long) { runOnUiThread { downloadController.deleteDownload(downloadId) } }
        @JavascriptInterface
        fun retryDownload(downloadId: Long) { runOnUiThread { downloadController.retryDownload(downloadId) } }

        // ---- Biyometrik Kilit (SecurityController'a delegate) ----
        @JavascriptInterface
        fun authenticateWithBiometrics() {
            runOnUiThread {
                securityController.authenticate { ok ->
                    overlay.evaluateJavascript("window.NexusOverlay && NexusOverlay.onBiometricResult($ok)", null)
                }
            }
        }

        // ---- Şifre Oluşturucu ----
        @JavascriptInterface
        fun generatePassword(length: Int, useUpper: Boolean, useLower: Boolean, useDigits: Boolean, useSymbols: Boolean): String {
            return tabManager.generatePassword(length, useUpper, useLower, useDigits, useSymbols)
        }

        // ---- Panoya Kopyala ----
        @JavascriptInterface
        fun copyToClipboard(text: String) { runOnUiThread { tabManager.copyToClipboard(text) } }

        // ---- NEXUS köprüsü: bağlantı testi + senkron ayarları ----
        @JavascriptInterface
        fun nexusTest() {
            val (url, token) = nexusAyar()
            if (token.isBlank()) {
                runOnUiThread { overlay.evaluateJavascript("window.NexusOverlay && NexusOverlay.onNexusTestResult(${jsStr("TOKEN_YOK")})", null) }
                return
            }
            Thread {
                val sonuc = try {
                    val conn = URL("$url/api/durum").openConnection() as HttpURLConnection
                    conn.setRequestProperty("X-Token", token)
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    val code = conn.responseCode
                    val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                        ?.bufferedReader()?.use { it.readText() } ?: ""
                    conn.disconnect()
                    if (code == 200) {
                        val botOk = try { JSONObject(body).optBoolean("bot_ok", false) } catch (e: Exception) { false }
                        "OK:$botOk"
                    } else "HATA:$code"
                } catch (e: Exception) { "ULASILAMADI:${e.javaClass.simpleName}" }
                runOnUiThread { overlay.evaluateJavascript("window.NexusOverlay && NexusOverlay.onNexusTestResult(${jsStr(sonuc)})", null) }
            }.start()
        }

        @JavascriptInterface
        fun nexusSetConfig(url: String, token: String, sync: Boolean) {
            val obj = settingsManager.getSettingsObj()
            obj.put("nexusUrl", url.trim().ifBlank { "http://127.0.0.1:8080" }.trimEnd('/'))
            obj.put("nexusToken", token.trim())
            obj.put("nexusSync", sync)
            settingsManager.saveSettingsObj(obj)
            runOnUiThread { Toast.makeText(this@MainActivity, "NEXUS köprüsü kaydedildi", Toast.LENGTH_SHORT).show() }
        }

        // ---- Faz 4: Otomatik güncelleme kontrolü ----
        @JavascriptInterface
        fun checkForUpdate() {
            val url = settingsManager.getSettingsObj().optString("updateCheckUrl", "")
            if (url.isBlank()) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Güncelleme adresi ayarlanmamış (Ayarlar'dan ekleyin)", Toast.LENGTH_SHORT).show() }
                return
            }
            Thread {
                try {
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.connectTimeout = 8000
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()
                    val obj = JSONObject(text)
                    val remoteVersion = obj.optInt("versionCode", 0)
                    val apkUrl = obj.optString("apkUrl", "")
                    runOnUiThread {
                        if (remoteVersion > APP_VERSION_CODE && apkUrl.isNotBlank()) {
                            Toast.makeText(this@MainActivity, "Yeni sürüm bulundu, indiriliyor. Bildirime dokunarak kurun.", Toast.LENGTH_LONG).show()
                            val request = DownloadManager.Request(Uri.parse(apkUrl))
                                .setDestinationInExternalFilesDir(this@MainActivity, Environment.DIRECTORY_DOWNLOADS, "nexus-update.apk")
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
                        } else {
                            Toast.makeText(this@MainActivity, "Güncel sürümü kullanıyorsunuz", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread { Toast.makeText(this@MainActivity, "Güncelleme kontrolü başarısız: ${e.message}", Toast.LENGTH_SHORT).show() }
                }
            }.start()
        }
    }

    private fun jsStr(s: String): String = JSONObject.quote(s)
}
