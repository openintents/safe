/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package org.openintents.safe.fingerprint

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProperties.*
import android.support.annotation.RequiresApi
import android.util.Base64
import android.util.Log
import android.view.View
import com.example.android.fingerprintdialog.DEFAULT_KEY_NAME
import com.example.android.fingerprintdialog.FingerprintAuthenticationDialogFragment
import com.example.android.fingerprintdialog.Stage
import kotlinx.android.synthetic.main.front_door.*
import org.openintents.safe.AskPassword
import org.openintents.safe.DBHelper
import org.openintents.safe.PreferenceActivity
import org.openintents.safe.PreferenceActivity.PREFKEY_FINGERPRINT_ENCRYPTED_MASTER
import org.openintents.safe.R
import org.openintents.safe.password.Master
import org.openintents.safe.service.AutoLockService
import java.io.File
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec


const val EXTRA_SETUP_FINGERPRINT = "use_master"
const val EXTRA_FIRST_TIME = "first_time"

@RequiresApi(Build.VERSION_CODES.M)
class AskFingerprint : AskPassword(),
        FingerprintAuthenticationDialogFragment.Callback {

    private lateinit var keyStore: KeyStore
    private lateinit var keyGenerator: KeyGenerator
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        use_fingerprint_description.visibility = View.VISIBLE
        use_fingerprint_button.visibility = View.VISIBLE
        remote.visibility = View.GONE

        setupKeyStoreAndKeyGenerator()

        val cipher = setupCipher()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        setUpButton(cipher)
    }

    /**
     * Enables or disables purchase buttons and sets the appropriate click listeners.
     *
     * @param cipher the default cipher, used for the purchase button
     */
    private fun setUpButton(cipher: Cipher) {
        val useFingerprintButton = use_fingerprint_button
        val keyguardManager = getSystemService(KeyguardManager::class.java)

        if (!keyguardManager.isKeyguardSecure) {
            // Show a message that the user hasn't set up a fingerprint or lock screen.
            showToast(getString(R.string.setup_lock_screen))
            useFingerprintButton.isEnabled = false
            return
        }

        val fingerprintManager = getSystemService(FingerprintManager::class.java)
        if (!fingerprintManager.hasEnrolledFingerprints()) {
            useFingerprintButton.isEnabled = false
            // This happens when no fingerprints are registered.
            showToast(getString(R.string.register_fingerprint))
            return
        }

        createKey(DEFAULT_KEY_NAME)

        if (intent.getBooleanExtra(EXTRA_FIRST_TIME, false)) {
            use_fingerprint_description.visibility = View.VISIBLE
        } else {
            use_fingerprint_description.visibility = View.GONE
        }

        useFingerprintButton.run {
            isEnabled = true
            setOnClickListener(PurchaseButtonClickListener(cipher))
            if (!intent.getBooleanExtra(EXTRA_FIRST_TIME, false)
                    && !intent.getBooleanExtra(EXTRA_WAIT_FOR_USER, false)) {
                performClick()
            }
        }
    }

    /**
     * Sets up KeyStore and KeyGenerator
     */
    private fun setupKeyStoreAndKeyGenerator() {
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        } catch (e: KeyStoreException) {
            throw RuntimeException("Failed to get an instance of KeyStore", e)
        }

        try {
            keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException,
                is NoSuchProviderException ->
                    throw RuntimeException("Failed to get an instance of KeyGenerator", e)
                else -> throw e
            }
        }
    }

    /**
     * Sets up the cipher
     */
    private fun setupCipher(): Cipher {
        val cipher: Cipher
        try {
            val cipherString = "$KEY_ALGORITHM_AES/$BLOCK_MODE_CBC/$ENCRYPTION_PADDING_PKCS7"
            cipher = Cipher.getInstance(cipherString)
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException,
                is NoSuchPaddingException ->
                    throw RuntimeException("Failed to get an instance of Cipher", e)
                else -> throw e
            }
        }
        return cipher
    }

    private val IV_FILE: String = "iv_file" // TODO do not backup

    /**
     * Initialize the [Cipher] instance with the created key in the [createKey] method.
     *
     * @param keyName the key name to init the cipher
     * @return `true` if initialization succeeded, `false` if the lock screen has been disabled or
     * reset after key generation, or if a fingerprint was enrolled after key generation.
     */
    private fun initCipher(cipher: Cipher, opmode: Int, newIV: Boolean): Boolean {
        try {
            keyStore.load(null)

            if (opmode == Cipher.ENCRYPT_MODE) {
                if (newIV) {
                    cipher.init(opmode, keyStore.getKey(DEFAULT_KEY_NAME, null) as SecretKey)
                    writeIV(cipher)
                } else {
                    cipher.init(opmode, keyStore.getKey(DEFAULT_KEY_NAME, null) as SecretKey)
                }
            } else {
                val ivParams = IvParameterSpec(readIV())
                cipher.init(opmode, keyStore.getKey(DEFAULT_KEY_NAME, null) as SecretKey, ivParams)
            }
            return true
        } catch (e: Exception) {
            when (e) {
                is KeyPermanentlyInvalidatedException -> return false
                is KeyStoreException,
                is CertificateException,
                is UnrecoverableKeyException,
                is IOException,
                is NoSuchAlgorithmException,
                is InvalidKeyException -> throw RuntimeException("Failed to init Cipher", e)
                else -> throw e
            }
        }
    }

    private fun readIV(): ByteArray {
        val file = File(getFilesDir(), IV_FILE)
        val fileSize = file.length().toInt()

        val iv = ByteArray(fileSize)

        val fis = openFileInput(IV_FILE)
        fis.read(iv, 0, fileSize)
        fis.close()

        return iv
    }

    private fun writeIV(cipher: Cipher) {
        val ivParams = cipher.getParameters().getParameterSpec(IvParameterSpec::class.java)
        val iv = ivParams.getIV()

        val fos = openFileOutput(IV_FILE, Context.MODE_PRIVATE)
        fos.write(iv)
        fos.close()
    }

    /**
     * Proceed with the purchase operation
     *
     * @param withFingerprint `true` if the purchase was made by using a fingerprint
     * @param crypto the Crypto object
     */
    override fun onFingerprintRecognized(crypto: FingerprintManager.CryptoObject?) {

        // verify using cryptography and then show
        // the confirmation message.
        if (crypto != null) {
            if (intent.getBooleanExtra(EXTRA_FIRST_TIME, false)) {
                PreferenceActivity.setFirstTimeFingerprint(this, false)
            }

            PreferenceActivity.setUseFingerprint(this, true)

            if (intent.getBooleanExtra(EXTRA_SETUP_FINGERPRINT, false)) {
                encrypt(crypto.cipher, Master.getMasterKey())
                setResult(Activity.RESULT_OK)
                finish()
            } else {

                val master = decrypt(crypto.cipher)

                val dbHelper = DBHelper(this);
                Master.setSalt(dbHelper.fetchSalt())
                dbHelper.close()

                Master.setMasterKey(master)

                val myIntent = Intent(applicationContext, AutoLockService::class.java)
                startService(myIntent)
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    override fun onGoToBackup() {
        PreferenceActivity.setUseFingerprint(this, false)
        setResult(RESULT_FINGERPRINT_ALTERNATIVE_REQUESTED)
        finish()
    }

    override fun onCancel() {
        if (intent.getBooleanExtra(EXTRA_FIRST_TIME, false)) {
            PreferenceActivity.setFirstTimeFingerprint(this, false)
        }
        finish()
    }

    private fun encrypt(cipher: Cipher, password: String) {
        try {
            initCipher(cipher, Cipher.ENCRYPT_MODE, true)
            val bytes = cipher.doFinal(password.toByteArray())
            val encrypted = Base64.encodeToString(bytes, Base64.NO_WRAP)
            Log.d("Master encrypted", "encypt: " + encrypted + " " + Base64.encode(cipher.parameters.getEncoded(null), Base64.NO_WRAP))
            sharedPreferences.edit().putString(PREFKEY_FINGERPRINT_ENCRYPTED_MASTER, encrypted).apply()
        } catch (exception: IllegalBlockSizeException) {
            throw RuntimeException("Failed to encrypt password", exception)
        } catch (exception: BadPaddingException) {
            throw RuntimeException("Failed to encrypt password", exception)
        }

    }

    private fun decrypt(cipher: Cipher): String {
        try {
            initCipher(cipher, Cipher.DECRYPT_MODE, false)
            val encoded = sharedPreferences.getString(PREFKEY_FINGERPRINT_ENCRYPTED_MASTER, null)
            Log.d("Master encrypted", "decrypt: " + encoded + " " + Base64.encode(cipher.parameters.getEncoded(null), Base64.NO_WRAP))
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            return String(cipher.doFinal(bytes))
        } catch (exception: IllegalBlockSizeException) {
            throw RuntimeException("Failed to decrypt password", exception)
        } catch (exception: BadPaddingException) {
            throw RuntimeException("Failed to decrypt password", exception)
        }

    }

    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with a fingerprint.
     *
     * @param keyName the name of the key to be created
     * @param invalidatedByBiometricEnrollment if `false` is passed, the created key will not be
     * invalidated even if a new fingerprint is enrolled. The default value is `true` - the key will
     * be invalidated if a new fingerprint is enrolled.
     */
    override fun createKey(keyName: String, invalidatedByBiometricEnrollment: Boolean) {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of enrolled
        // fingerprints has changed.
        try {
            keyStore.load(null)

            if (keyStore.containsAlias(keyName)) {
                return
            }

            val keyProperties = KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            val builder = KeyGenParameterSpec.Builder(keyName, keyProperties)
                    .setBlockModes(BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(false)
                    .setEncryptionPaddings(ENCRYPTION_PADDING_PKCS7)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment)
            }

            keyGenerator.run {
                init(builder.build())
                generateKey()
            }
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException,
                is InvalidAlgorithmParameterException,
                is CertificateException,
                is IOException -> throw RuntimeException(e)
                else -> throw e
            }
        }
    }

    private inner class PurchaseButtonClickListener internal constructor(
            internal var cipher: Cipher
    ) : View.OnClickListener {

        override fun onClick(view: View) {
            val fragment = FingerprintAuthenticationDialogFragment()
            fragment.setCryptoObject(FingerprintManager.CryptoObject(cipher))
            fragment.setCallback(this@AskFingerprint)

            val fingerprintSetup = isFingerprintSetup();
            // Set up the crypto object for later, which will be authenticated by fingerprint usage.
            if (initCipher(cipher, Cipher.ENCRYPT_MODE, fingerprintSetup)) {

                // Show the fingerprint dialog. The user has the option to use the fingerprint with
                // crypto, or can fall back to using a server-side verified password.
                if (fingerprintSetup) {
                    fragment.setStage(Stage.SETUP_FINGERPRINT)
                } else {
                    fragment.setStage(Stage.FINGERPRINT)
                }
            } else {
                // This happens if the lock screen has been disabled nor a fingerprint was
                // enrolled. Thus, show the dialog to authenticate with their password first and ask
                // the user if they want to authenticate with a fingerprint in the future.
                fragment.setStage(Stage.NEW_FINGERPRINT_ENROLLED)
            }
            fragment.show(fragmentManager, DIALOG_FRAGMENT_TAG)
        }
    }

    private fun isFingerprintSetup() = intent.getBooleanExtra(EXTRA_SETUP_FINGERPRINT, false)

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val DIALOG_FRAGMENT_TAG = "myFragment"
    }
}
