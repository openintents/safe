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

package com.example.android.fingerprintdialog

import android.app.DialogFragment
import android.content.Context
import android.content.SharedPreferences
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.RequiresApi
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import org.openintents.safe.R
import javax.crypto.Cipher

/**
 * A dialog which uses fingerprint APIs to authenticate the user, and falls back to password
 * authentication if fingerprint is not available.
 */
@RequiresApi(Build.VERSION_CODES.M)
class FingerprintAuthenticationDialogFragment : DialogFragment(),
        FingerprintUiHelper.Callback {

    private lateinit var cancelButton: Button
    private lateinit var fingerprintContainer: View
    private lateinit var fingerprintEnrolledTextView: TextView
    private lateinit var passwordDescriptionTextView: TextView
    private lateinit var secondDialogButton: Button
    private lateinit var useFingerprintFutureCheckBox: CheckBox

    private lateinit var callback: Callback
    private lateinit var cryptoObject: FingerprintManager.CryptoObject
    private lateinit var fingerprintUiHelper: FingerprintUiHelper
    private lateinit var inputMethodManager: InputMethodManager
    private lateinit var sharedPreferences: SharedPreferences

    private var stage = Stage.SETUP_FINGERPRINT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        retainInstance = true
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View? {
        dialog.setTitle(getString(R.string.sign_in))
        return inflater.inflate(R.layout.fingerprint_dialog_container, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cancelButton = view.findViewById(R.id.cancel_button)
        fingerprintContainer = view.findViewById(R.id.fingerprint_container)
        fingerprintEnrolledTextView = view.findViewById(R.id.new_fingerprint_enrolled_description)
        passwordDescriptionTextView = view.findViewById(R.id.password_description)
        secondDialogButton = view.findViewById(R.id.second_dialog_button)
        useFingerprintFutureCheckBox = view.findViewById(R.id.use_fingerprint_in_future_check)

        cancelButton.setOnClickListener {
            dismiss()
            callback.onCancel()
        }
        secondDialogButton.setOnClickListener {
            gotToAskPassword()
        }

        fingerprintUiHelper = FingerprintUiHelper(
                activity.getSystemService(FingerprintManager::class.java),
                view.findViewById(R.id.fingerprint_icon),
                view.findViewById(R.id.fingerprint_status),
                this
        )
        updateStage()

        // If fingerprint authentication is not available, switch immediately to the backup
        // (password) screen.
        if (!fingerprintUiHelper.isFingerprintAuthAvailable) {
            gotToAskPassword()
        }
    }

    override fun onResume() {
        super.onResume()
        if (stage == Stage.FINGERPRINT || stage == Stage.SETUP_FINGERPRINT) {
            fingerprintUiHelper.startListening(cryptoObject)
        }
    }

    override fun onPause() {
        super.onPause()
        fingerprintUiHelper.stopListening()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        inputMethodManager = context.getSystemService(InputMethodManager::class.java)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    fun setCryptoObject(cryptoObject: FingerprintManager.CryptoObject) {
        this.cryptoObject = cryptoObject
    }

    fun setStage(stage: Stage) {
        this.stage = stage
    }

    private fun gotToAskPassword() {
        fingerprintUiHelper.stopListening()
        dismiss()
        callback.onGoToBackup()
    }

    private fun updateStage() {
        when (stage) {
            Stage.SETUP_FINGERPRINT -> {
                dialog.setTitle(R.string.setup_fingerprint)
                cancelButton.setText(R.string.cancel)
                secondDialogButton.visibility = View.GONE
                fingerprintContainer.visibility = View.VISIBLE
            }
            Stage.FINGERPRINT -> {
                cancelButton.setText(R.string.cancel)
                secondDialogButton.visibility = View.VISIBLE
                secondDialogButton.setText(R.string.use_password)
                fingerprintContainer.visibility = View.VISIBLE
            }
            Stage.NEW_FINGERPRINT_ENROLLED -> {
                cancelButton.setText(R.string.cancel)
                secondDialogButton.visibility = View.VISIBLE
                secondDialogButton.setText(R.string.ok)
                fingerprintContainer.visibility = View.GONE
                passwordDescriptionTextView.visibility = View.GONE
                fingerprintEnrolledTextView.visibility = View.VISIBLE
                useFingerprintFutureCheckBox.visibility = View.VISIBLE
            }
        }
    }

    override fun onAuthenticated() {
        dismiss()
        callback.onFingerprintRecognized(crypto = cryptoObject)
    }

    override fun onError() {
        dismiss()
        callback.onGoToBackup()
    }

    interface Callback {
        fun onFingerprintRecognized(crypto: FingerprintManager.CryptoObject? = null)
        fun createKey(keyName: String, invalidatedByBiometricEnrollment: Boolean = true)
        fun onGoToBackup()
        fun onCancel()
    }
}
