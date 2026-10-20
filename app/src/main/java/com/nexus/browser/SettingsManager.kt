package com.nexus.browser

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File

/**
 * Tüm kalıcı ayar verilerini (SharedPreferences üzerinden) yönetir.
 * MainActivity'den ayrı bir sınıf olarak tutulur ki MainActivity.kt kısalıyor.
 */
class SettingsManager(private val prefs: SharedPreferences, private val context: Context) {

    // ---------- Ayarlar ----------
    fun getSettingsObj(): JSONObject {
        val raw = prefs.getString("settings", null)
        return if (raw != null) JSONObject(raw) else JSONObject().apply {
            put("adBlock", true); put("trackerBlock", true); put("httpsOnly", false)
            put("perfMode", "dengeli"); put("tabSleep", false)
            put("imageLimitMB", 0); put("cacheLimitMB", 0)
            put("accentColor", "#5b5bf0")
            put("groqApiKey", ""); put("groqModel", "llama-3.3-70b-versatile")
            put("updateCheckUrl", "")
            put("customSearchKey", ""); put("customSearchCx", "")
            put("theme", "light"); put("darkMode", false); put("searchEngine", "google")
            put("lang", "tr")
            put("nexusUrl", "http://127.0.0.1:8080"); put("nexusToken", ""); put("nexusSync", true)
        }
    }

    fun saveSettingsObj(obj: JSONObject) {
        prefs.edit().putString("settings", obj.toString()).commit()
    }

    // ---------- Geçmiş ----------
    fun getHistoryArr(): JSONArray {
        val raw = prefs.getString("history", null) ?: return JSONArray()
        return try { JSONArray(raw) } catch (e: Exception) { JSONArray() }
    }

    fun addHistoryEntry(title: String, url: String) {
        val arr = getHistoryArr()
        val entry = JSONObject().apply { put("title", title); put("url", url); put("time", System.currentTimeMillis()) }
        val newArr = JSONArray()
        newArr.put(entry)
        for (i in 0 until minOf(arr.length(), 400)) newArr.put(arr.get(i))
        prefs.edit().putString("history", newArr.toString()).commit()
    }

    fun clearHistory() {
        prefs.edit().remove("history").commit()
    }

    // ---------- Yer imleri ----------
    fun getBookmarksArr(): JSONArray {
        val raw = prefs.getString("bookmarks", null) ?: return JSONArray()
        return try { JSONArray(raw) } catch (e: Exception) { JSONArray() }
    }

    fun saveBookmarksArr(arr: JSONArray) {
        prefs.edit().putString("bookmarks", arr.toString()).commit()
    }

    fun toggleBookmark(title: String, url: String): Boolean {
        val arr = getBookmarksArr()
        var idx = -1
        for (i in 0 until arr.length()) if (arr.getJSONObject(i).optString("url") == url) { idx = i; break }
        return if (idx >= 0) {
            val n = JSONArray(); for (i in 0 until arr.length()) if (i != idx) n.put(arr.get(i))
            saveBookmarksArr(n); false
        } else {
            arr.put(JSONObject().apply { put("title", title); put("url", url) }); saveBookmarksArr(arr); true
        }
    }

    fun isBookmarked(url: String): Boolean {
        val arr = getBookmarksArr()
        for (i in 0 until arr.length()) if (arr.getJSONObject(i).optString("url") == url) return true
        return false
    }

    // ---------- Şifreler ----------
    fun getPasswordsArr(): JSONArray {
        val raw = prefs.getString("passwords", null) ?: return JSONArray()
        return try { JSONArray(raw) } catch (e: Exception) { JSONArray() }
    }

    fun savePasswordsArr(arr: JSONArray) {
        prefs.edit().putString("passwords", arr.toString()).commit()
    }

    fun deletePassword(id: String) {
        val arr = getPasswordsArr()
        val n = JSONArray(); for (i in 0 until arr.length()) if (arr.getJSONObject(i).optString("id") != id) n.put(arr.get(i))
        savePasswordsArr(n)
    }

    // ---------- Önbellek ----------
    fun getDirSize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { f -> size += if (f.isDirectory) getDirSize(f) else f.length() }
        return size
    }

    fun trimCacheIfNeeded() {
        val limitMB = getSettingsObj().optInt("cacheLimitMB", 0)
        if (limitMB <= 0) return
        try {
            val cacheDir = context.cacheDir
            if (cacheDir != null && getDirSize(cacheDir) > limitMB.toLong() * 1024 * 1024) {
                // clearCache WebView'de yapılmalı, bu yüzden sadece bilgiyi döndürür; çağrıyıcı WebView'i temizler
            }
        } catch (e: Exception) { }
    }
}
