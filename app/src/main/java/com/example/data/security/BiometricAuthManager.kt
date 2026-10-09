package com.example.data.security

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

sealed class BiometricCapability {
    data object Available : BiometricCapability()
    data object NoneEnrolled : BiometricCapability()
    data object NoHardware : BiometricCapability()
    data object HardwareUnavailable : BiometricCapability()
    data object SecurityUpdateRequired : BiometricCapability()
    data class Error(val message: String) : BiometricCapability()
}

sealed class AuthResult {
    data object Success : AuthResult()
    data class Failure(val message: String, val isCanceled: Boolean = false, val isLockout: Boolean = false) : AuthResult()
}

class BiometricAuthManager(private val context: Context) {

    private val biometricManager = BiometricManager.from(context)
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    /**
     * Determine preferred authenticators based on API level and capabilities.
     * On Android 11+ (API 30+), BIOMETRIC_STRONG or DEVICE_CREDENTIAL is standard.
     */
    val authenticators: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        } else {
            // Prior to API 30, DEVICE_CREDENTIAL with BIOMETRIC_STRONG may have quirks on some OEM devices;
            // check if keyguard is secured.
            if (isDeviceSecure()) {
                BIOMETRIC_STRONG or DEVICE_CREDENTIAL
            } else {
                BIOMETRIC_STRONG
            }
        }

    fun isDeviceSecure(): Boolean {
        return keyguardManager?.isDeviceSecure == true
    }

    fun checkCapability(): BiometricCapability {
        return when (val status = biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.Available
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // If biometric is not enrolled but device credential (PIN/pattern) is set up
                if (isDeviceSecure()) {
                    BiometricCapability.Available
                } else {
                    BiometricCapability.NoneEnrolled
                }
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                if (isDeviceSecure()) BiometricCapability.Available else BiometricCapability.NoHardware
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                if (isDeviceSecure()) BiometricCapability.Available else BiometricCapability.HardwareUnavailable
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricCapability.SecurityUpdateRequired
            else -> BiometricCapability.Error("Authentication status code: $status")
        }
    }

    /**
     * Launch Android system enrollment settings so the user can configure a PIN, pattern, or biometrics.
     */
    fun openEnrollmentSettings(activity: FragmentActivity) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        try {
            activity.startActivity(intent)
        } catch (_: Exception) {
            activity.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    /**
     * Show the Android system BiometricPrompt with fingerprint/face priority and device credential fallback.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock TransactionMate",
        subtitle: String = "Verify your identity to access financial records",
        description: String = "Confirm your fingerprint, face, or device PIN / pattern",
        onResult: (AuthResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val promptCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(AuthResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                val isCanceled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                val isLockout = errorCode == BiometricPrompt.ERROR_LOCKOUT ||
                        errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT

                onResult(
                    AuthResult.Failure(
                        message = errString.toString(),
                        isCanceled = isCanceled,
                        isLockout = isLockout
                    )
                )
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(
                    AuthResult.Failure(
                        message = "Biometric not recognized. Please try again.",
                        isCanceled = false,
                        isLockout = false
                    )
                )
            }
        }

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)

        // When DEVICE_CREDENTIAL is included, setNegativeButtonText MUST NOT be used
        val authModes = authenticators
        promptInfoBuilder.setAllowedAuthenticators(authModes)
        if ((authModes and DEVICE_CREDENTIAL) == 0) {
            promptInfoBuilder.setNegativeButtonText("Cancel")
        }

        val biometricPrompt = BiometricPrompt(activity, executor, promptCallback)
        try {
            biometricPrompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            onResult(AuthResult.Failure("Failed to initialize authentication: ${e.localizedMessage}"))
        }
    }
}
