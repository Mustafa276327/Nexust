package com.nexus.browser

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.util.LinkedList

/**
 * WebView ile ilgili tüm iş mantığını tek sınıf altında toplar:
 * - Geliştirici konsolu (console.log, JS hataları)
 * - Çerez yönetimi (görüntüleme, silme)
 * - User-Agent değiştirme
 */
class WebViewController(private val context: android.content.Context) {

    companion object {
        private const val MAX_CONSOLE_LOGS = 200
    }

    // ---------- Geliştirici Konsolu ----------
    private val consoleLogs = LinkedList<String>()

    fun addConsoleLog(message: String, level: String = "log") {
        val entry = "[$level] $message"
        consoleLogs.addFirst(entry)
        if (consoleLogs.size > MAX_CONSOLE_LOGS) consoleLogs.removeLast()
    }

    fun getConsoleLogsJson(): String {
        val out = JSONArray()
        consoleLogs.forEach { out.put(it) }
        return out.toString()
    }

    fun clearConsoleLogs() {
        consoleLogs.clear()
    }

    // ---------- Çerez Yönetimi ----------
    @SuppressLint("NewApi")
    fun getCookiesForUrl(url: String): String {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(url)
        if (cookies.isNullOrEmpty()) return "[]"
        val out = JSONArray()
        cookies.split(";").forEach { cookie ->
            val trimmed = cookie.trim()
            val eqIdx = trimmed.indexOf('=')
            if (eqIdx > 0) {
                val name = trimmed.substring(0, eqIdx).trim()
                val value = trimmed.substring(eqIdx + 1).trim()
                out.put(JSONObject().apply {
                    put("name", name)
                    put("value", value)
                })
            }
        }
        return out.toString()
    }

    @SuppressLint("NewApi")
    fun deleteCookieForUrl(url: String, cookieName: String) {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(url)
        if (cookies.isNullOrEmpty()) return
        cookies.split(";").forEach { cookie ->
            val trimmed = cookie.trim()
            val eqIdx = trimmed.indexOf('=')
            if (eqIdx > 0) {
                val name = trimmed.substring(0, eqIdx).trim()
                if (name == cookieName) {
                    // Çerezi süresi dolmuş olarak ayarla (silme etkisi)
                    cookieManager.setCookie(url, "$cookieName=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/")
                }
            }
        }
        cookieManager.flush()
        Toast.makeText(context, "Çerez silindi: $cookieName", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("NewApi")
    fun deleteAllCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        Toast.makeText(context, "Tüm çerezler silindi", Toast.LENGTH_SHORT).show()
    }

    // ---------- User-Agent ----------
    fun getDefaultUserAgent(wv: WebView): String {
        return wv.settings.userAgentString
    }

    fun setDesktopUserAgent(wv: WebView) {
        wv.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        Toast.makeText(context, "Masaüstü modu etkinleştirildi", Toast.LENGTH_SHORT).show()
    }

    fun setMobileUserAgent(wv: WebView) {
        wv.settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        Toast.makeText(context, "Mobil modu etkinleştirildi", Toast.LENGTH_SHORT).show()
    }

    // ---------- Reader Mode ----------
    fun injectReaderMode(wv: WebView) {
        val css = """
            header, nav, footer, .ads, .ad, .advertisement, .sidebar, .menu, .popup, .modal {
                display: none !important;
            }
            article, main, .content, .post-content, .entry-content {
                font-size: 18px !important;
                line-height: 1.7 !important;
                max-width: 720px !important;
                margin: 0 auto !important;
            }
            body { background: var(--theme-bg, #FFFFFF) !important; color: var(--theme-text, #202133) !important; }
            a { color: var(--accent, #5b5bf0) !important; }
        """.trimIndent()
        wv.evaluateJavascript("""
            (function(){
                var s = document.createElement('style');
                s.type = 'text/css';
                s.setAttribute('data-reader-mode', 'true');
                s.innerHTML = \`${css.replace("`", "\\`")}\`;
                document.head.appendChild(s);
            })();
        """, null)
    }
}
