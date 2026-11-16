package com.example.ailearning.data.local.encryption

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.inject.Inject

class EncryptionManager @Inject constructor(
    private val context: Context
) {
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "secure_api_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val dbEncryptionKey by lazy {
        generateDbKey()
    }

    fun storeApiKey(keyId: String, plainTextKey: String) {
        encryptedPrefs.edit().putString(keyId, plainTextKey).apply()
    }

    fun getApiKey(keyId: String): String? {
        return encryptedPrefs.getString(keyId, null)
    }

    fun removeApiKey(keyId: String) {
        encryptedPrefs.edit().remove(keyId).apply()
    }

    fun getDatabaseKey(): ByteArray = dbEncryptionKey

    private fun generateDbKey(): ByteArray {
        val keyAlias = "ai_learning_db_key"
        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGen = javax.crypto.KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
            )
            val spec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).apply {
                setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                setKeySize(256)
            }.build()
            keyGen.init(spec)
            keyGen.generateKey()
        }
        
        val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedKey = cipher.doFinal("AI_LEARNING_DB_KEY_2024".toByteArray())
        
        return iv + encryptedKey
    }
}
