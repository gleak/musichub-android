package com.mediaplayer.android.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM envelope around the persisted Google ID token, with the
 * data-encryption key sealed inside the Android Keystore (hardware-backed
 * where the device supports it). The ciphertext sitting in DataStore is
 * useless to anyone who exfiltrates `auth.preferences_pb` without also
 * holding the Keystore master — which can't be exported off the device.
 *
 * Wire format: `Base64(IV ‖ ciphertext+tag)`. Twelve-byte IV is the GCM
 * standard; the 16-byte authentication tag rides inside `ciphertext+tag`
 * because that's how JCE's GCM cipher outputs it on `doFinal`.
 *
 * Failure mode: any decryption error (corrupted blob, the keystore alias
 * vanished after a backup-restore to a different device, ciphertext
 * tampered with) maps to `null` from [decrypt] — the caller treats that
 * as "no stored token" and falls back to the regular sign-in flow.
 */
internal object TokenCipher {

    private const val ALIAS = "mp_auth_token_v1"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LEN = 12
    private const val GCM_TAG_BITS = 128

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val packed = ByteArray(iv.size + cipherText.size).also {
            System.arraycopy(iv, 0, it, 0, iv.size)
            System.arraycopy(cipherText, 0, it, iv.size, cipherText.size)
        }
        return Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    fun decrypt(blob: String): String? = runCatching {
        val packed = Base64.decode(blob, Base64.NO_WRAP)
        if (packed.size <= IV_LEN) return null
        val iv = packed.copyOfRange(0, IV_LEN)
        val cipherText = packed.copyOfRange(IV_LEN, packed.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }.getOrNull()

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}
