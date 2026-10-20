package com.nexus.browser

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import android.content.Intent

/**
 * İndirme yönetimi için sorumlu sınıf:
 * - Dosya indirme başlatma (DownloadManager)
 * - İndirme listesini sorgulama (JSON)
 * - İndirilen dosyayı açma
 * - İndirmeyi silme
 * - İndirmeyi tekrar deneme
 */
class DownloadController(private val context: Context) {

    fun downloadFile(url: String, fileName: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription("Nexus Browser — İndiriliyor…")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            Toast.makeText(context, "İndirme başlatıldı: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "İndirme başarısız: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun getDownloadsJson(): String {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterByStatus(
            DownloadManager.STATUS_FAILED or DownloadManager.STATUS_PAUSED or
            DownloadManager.STATUS_PENDING or DownloadManager.STATUS_RUNNING or
            DownloadManager.STATUS_SUCCESSFUL
        )
        val cursor: Cursor? = dm.query(query)
        val out = JSONArray()
        if (cursor != null) {
            try {
                val idxId = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
                val idxTitle = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                val idxUri = cursor.getColumnIndex(DownloadManager.COLUMN_URI)
                val idxStatus = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val idxTotal = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val idxBytes = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val idxMime = cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)
                while (cursor.moveToNext()) {
                    val id = if (idxId >= 0) cursor.getLong(idxId) else 0L
                    val title = if (idxTitle >= 0) cursor.getString(idxTitle) else "İndirme"
                    val uri = if (idxUri >= 0) cursor.getString(idxUri) else ""
                    val status = if (idxStatus >= 0) cursor.getInt(idxStatus) else 0
                    val total = if (idxTotal >= 0) cursor.getLong(idxTotal) else 0L
                    val bytes = if (idxBytes >= 0) cursor.getLong(idxBytes) else 0L
                    val mime = if (idxMime >= 0) cursor.getString(idxMime) else ""
                    val statusLabel = when (status) {
                        DownloadManager.STATUS_FAILED -> "Hata"
                        DownloadManager.STATUS_PAUSED -> "Durduruldu"
                        DownloadManager.STATUS_PENDING -> "Bekliyor"
                        DownloadManager.STATUS_RUNNING -> "İndiriliyor"
                        DownloadManager.STATUS_SUCCESSFUL -> "Tamamlandı"
                        else -> "Bilinmiyor"
                    }
                    val pct = if (total > 0) (bytes * 100 / total).toInt() else 0
                    out.put(JSONObject().apply {
                        put("id", id)
                        put("title", title)
                        put("uri", uri)
                        put("status", statusLabel)
                        put("statusCode", status)
                        put("total", total)
                        put("bytes", bytes)
                        put("percent", pct)
                        put("mime", mime)
                    })
                }
            } finally {
                cursor.close()
            }
        }
        return out.toString()
    }

    fun openDownload(downloadId: Long) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = dm.getUriForDownloadedFile(downloadId)
        if (uri == null) {
            Toast.makeText(context, "Dosya bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(uri.toString()))
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Dosya açılacak uygulama bulunamadı", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteDownload(downloadId: Long) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.remove(downloadId)
        Toast.makeText(context, "İndirme silindi", Toast.LENGTH_SHORT).show()
    }

    fun retryDownload(downloadId: Long) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = dm.query(query)
        var url = ""
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    val idxUri = cursor.getColumnIndex(DownloadManager.COLUMN_URI)
                    if (idxUri >= 0) url = cursor.getString(idxUri)
                }
            } finally {
                cursor.close()
            }
        }
        if (url.isNotEmpty()) {
            val fileName = url.substringAfterLast("/").takeIf { it.isNotEmpty() } ?: "nexus_download"
            downloadFile(url, fileName)
        } else {
            Toast.makeText(context, "URL alınamadı, deneme başarısız", Toast.LENGTH_SHORT).show()
        }
    }
}
