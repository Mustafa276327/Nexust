package com.nexus.browser

import org.json.JSONArray
import org.json.JSONObject
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Güvenlik ile ilgili tüm iş mantığını tek sınıf altında toplar:
 * - Şifre kasası (Android Keystore + AES-GCM)
 * - Have I Been Pwned sızıntı kontrolü
 * - Groq AI çağrıları (sayfa özeti, sekme gruplama)
 * - Biyometrik kimlik doğrulama (placeholder - FragmentActivity gerektirir)
 */
class SecurityController(private val context: android.content.Context) {

    // ---------- Şifre kasası ----------
    private object PasswordVault {
        private const val ALIAS = "nexus_pw_key"

        private fun getOrCreateKey(): SecretKey {
            val ks = KeyStore.getInstance("AndroidKeyStore")
            ks.load(null)
            (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
            val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            kg.init(
                KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            return kg.generateKey()
        }

        fun encrypt(plain: String): Pair<String, String> {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val ct = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            return Pair(
                android.util.Base64.encodeToString(ct, android.util.Base64.NO_WRAP),
                android.util.Base64.encodeToString(cipher.iv, android.util.Base64.NO_WRAP)
            )
        }

        fun decrypt(cipherTextB64: String, ivB64: String): String {
            return try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val iv = android.util.Base64.decode(ivB64, android.util.Base64.NO_WRAP)
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
                String(cipher.doFinal(android.util.Base64.decode(cipherTextB64, android.util.Base64.NO_WRAP)), Charsets.UTF_8)
            } catch (e: Exception) { "" }
        }
    }

    fun encryptPassword(plain: String): Pair<String, String> = PasswordVault.encrypt(plain)
    fun decryptPassword(cipherTextB64: String, ivB64: String): String = PasswordVault.decrypt(cipherTextB64, ivB64)

    // ---------- HIBP Sızıntı Kontrolü ----------
    fun isPasswordBreached(password: String): Boolean {
        val sha1 = MessageDigest.getInstance("SHA-1").digest(password.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02X".format(it) }
        val prefix = sha1.substring(0, 5)
        val suffix = sha1.substring(5)
        val conn = java.net.URL("https://api.pwnedpasswords.com/range/$prefix").openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 8000; conn.readTimeout = 8000
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        return text.lineSequence().any { it.startsWith(suffix, ignoreCase = true) }
    }

    // ---------- Groq AI Çağrısı ----------
    fun callGroq(apiKey: String, model: String, systemPrompt: String, userPrompt: String): String {
        return try {
            val conn = java.net.URL("https://api.groq.com/openai/v1/chat/completions").openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 25000
            val body = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                    put(JSONObject().apply { put("role", "user"); put("content", userPrompt) })
                })
                put("temperature", 0.3)
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream.bufferedReader().use { it.readText() }
            conn.disconnect()
            if (code !in 200..299) return "HATA ($code): $text"
            JSONObject(text).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        } catch (e: Exception) { "İstek başarısız: ${e.message}" }
    }

    // ---------- Biyometrik Kimlik Doğrulama ----------
    // Not: BiometricPrompt, androidx.fragment.app.FragmentActivity gerektirir.
    // Proje android.useAndroidX=false olduğu kadar, burası basit bir wrapper olarak bırakıldı.
    // İleride MainActivity FragmentActivity'ye çevrildiğinde gerçek BiometricPrompt entegrasyonu eklenebilir.
    fun authenticate(callback: (Boolean) -> Unit) {
        // Geçici: her zaman başarılı say (UI tarafı bekleme ekranını kaldırır)
        // Gerçek implementasyon için MainActivity'yi FragmentActivity'ye dönüştürmek gerekir.
        callback(true)
    }
}
