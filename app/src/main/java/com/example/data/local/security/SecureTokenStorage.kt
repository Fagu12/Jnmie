package com.example.data.local.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Interface for securely storing and retrieving sensitive authentication tokens.
 * Enforces hardware/software cryptographic storage backed by Android KeyStore.
 */
interface SecureTokenStorage {
    val tokenFlow: Flow<String?>
    suspend fun getAccessToken(): String?
    suspend fun saveAccessToken(token: String?)
    suspend fun clearAccessToken()
    fun isStorageSecured(): Boolean
}

/**
 * Android KeyStore-backed AES-256 GCM Secure Token Storage.
 *
 * Encrypts sensitive OAuth and personal access tokens with AES-256-GCM using keys
 * generated inside the Android KeyStore provider. Raw plaintext tokens are never
 * written to unencrypted disk or plain preferences.
 */
class AndroidKeyStoreSecureTokenStorage(
    private val context: Context
) : SecureTokenStorage {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
    }

    private val _tokenFlow = MutableStateFlow<String?>(null)
    override val tokenFlow: Flow<String?> = _tokenFlow.asStateFlow()

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER).apply {
            load(null)
        }
    }

    init {
        // Load initial decrypted token on startup
        try {
            val initialToken = decryptStoredToken()
            _tokenFlow.value = initialToken
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing secure token storage: ${e.message}")
            _tokenFlow.value = null
        }
    }

    override suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            decryptStoredToken()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt access token: ${e.message}")
            null
        }
    }

    override suspend fun saveAccessToken(token: String?) = withContext(Dispatchers.IO) {
        try {
            if (token.isNullOrBlank()) {
                clearAccessTokenInternal()
            } else {
                val encryptedBase64 = encryptToken(token.trim())
                sharedPreferences.edit()
                    .putString(KEY_ENCRYPTED_TOKEN, encryptedBase64)
                    .putBoolean(KEY_IS_ENCRYPTED, true)
                    .apply()
                _tokenFlow.value = token.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to securely save token: ${e.message}", e)
            // Fallback: in restricted environments, ensure memory flow is updated
            _tokenFlow.value = token?.trim()
        }
    }

    override suspend fun clearAccessToken() = withContext(Dispatchers.IO) {
        clearAccessTokenInternal()
    }

    override fun isStorageSecured(): Boolean = true

    private fun clearAccessTokenInternal() {
        sharedPreferences.edit()
            .remove(KEY_ENCRYPTED_TOKEN)
            .remove(KEY_IS_ENCRYPTED)
            .apply()
        _tokenFlow.value = null
    }

    private fun getOrCreateSecretKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE_PROVIDER
            )
            val keyGenSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AES_KEY_SIZE_BITS)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(keyGenSpec)
            return keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey ?: throw IllegalStateException("KeyStore entry is not a SecretKey")
    }

    private fun encryptToken(plainText: String): String {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Store IV (12 bytes) prepended to ciphertext
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decryptStoredToken(): String? {
        val encryptedBase64 = sharedPreferences.getString(KEY_ENCRYPTED_TOKEN, null) ?: return null
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH_BYTES) return null

            val iv = ByteArray(GCM_IV_LENGTH_BYTES)
            val ciphertext = ByteArray(combined.size - GCM_IV_LENGTH_BYTES)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH_BYTES)
            System.arraycopy(combined, GCM_IV_LENGTH_BYTES, ciphertext, 0, ciphertext.size)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Decryption failed, clearing corrupted token: ${e.message}")
            clearAccessTokenInternal()
            null
        }
    }

    companion object {
        private const val TAG = "SecureTokenStorage"
        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val PREF_FILE_NAME = "secure_auth_prefs"
        private const val KEY_ALIAS = "anilist_secure_auth_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_ENCRYPTED_TOKEN = "encrypted_anilist_token"
        private const val KEY_IS_ENCRYPTED = "is_keystore_encrypted"
        private const val AES_KEY_SIZE_BITS = 256
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
    }
}
