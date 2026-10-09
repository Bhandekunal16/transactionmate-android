package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.data.preferences.AppPreferences
import com.example.data.repository.TransactionMateRepository
import com.example.data.security.AuthResult
import com.example.data.security.BiometricAuthManager
import com.example.data.security.BiometricCapability
import com.example.navigation.MainAppScaffold
import com.example.ui.auth.LockScreen
import com.example.ui.theme.TransactionMateTheme
import com.example.viewmodel.TransactionMateViewModel
import com.example.viewmodel.TransactionMateViewModelFactory

class MainActivity : FragmentActivity() {

    private val preferences by lazy { AppPreferences(applicationContext) }
    private val repository by lazy { TransactionMateRepository(preferences) }
    private val biometricAuthManager by lazy { BiometricAuthManager(this) }
    private val viewModel: TransactionMateViewModel by viewModels {
        TransactionMateViewModelFactory(repository, preferences)
    }

    private var backgroundTimestamp: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val themeColor by viewModel.themeColor.collectAsState()

            TransactionMateTheme(
                themeMode = themeMode,
                themeColor = themeColor
            ) {
                val isLocked by viewModel.isAppLocked.collectAsState()
                val biometricEnabled by viewModel.biometricLockEnabled.collectAsState()
                val biometricCapability by viewModel.biometricCapability.collectAsState()
                val authErrorMessage by viewModel.authErrorMessage.collectAsState()
                val isAuthenticating by viewModel.isAuthenticating.collectAsState()

                // Check capability on launch
                LaunchedEffect(Unit) {
                    val capability = biometricAuthManager.checkCapability()
                    viewModel.updateBiometricCapability(capability)
                    if (biometricEnabled && isLocked) {
                        triggerBiometricPrompt()
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (biometricEnabled && isLocked) {
                        LockScreen(
                            biometricCapability = biometricCapability,
                            errorMessage = authErrorMessage,
                            isAuthenticating = isAuthenticating,
                            onTriggerAuth = { triggerBiometricPrompt() },
                            onOpenSettings = { biometricAuthManager.openEnrollmentSettings(this@MainActivity) }
                        )
                    } else {
                        MainAppScaffold(viewModel = viewModel)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val capability = biometricAuthManager.checkCapability()
        viewModel.updateBiometricCapability(capability)

        if (viewModel.biometricLockEnabled.value && backgroundTimestamp > 0L) {
            val elapsedSeconds = (System.currentTimeMillis() - backgroundTimestamp) / 1000
            if (elapsedSeconds >= AppPreferences.DEFAULT_LOCK_TIMEOUT_SECONDS) {
                viewModel.setAppLocked(true)
                triggerBiometricPrompt()
            }
        }
        backgroundTimestamp = 0L
    }

    override fun onStop() {
        super.onStop()
        backgroundTimestamp = System.currentTimeMillis()
    }

    private fun triggerBiometricPrompt() {
        val capability = biometricAuthManager.checkCapability()
        viewModel.updateBiometricCapability(capability)

        if (capability is BiometricCapability.NoneEnrolled && !biometricAuthManager.isDeviceSecure()) {
            viewModel.setAuthError("No biometric or device screen lock enrolled. Please configure in Android Settings.")
            return
        }

        viewModel.setIsAuthenticating(true)
        viewModel.setAuthError(null)

        biometricAuthManager.authenticate(this) { result ->
            viewModel.setIsAuthenticating(false)
            when (result) {
                is AuthResult.Success -> {
                    viewModel.setAppLocked(false)
                    viewModel.setAuthError(null)
                }
                is AuthResult.Failure -> {
                    if (result.isLockout) {
                        viewModel.setAuthError("Too many failed attempts. Device authentication is temporarily locked.")
                    } else if (!result.isCanceled) {
                        viewModel.setAuthError(result.message)
                    }
                }
            }
        }
    }
}
