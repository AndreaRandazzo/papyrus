package io.github.andrearandazzo.papyrus.lock

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Asks the phone to confirm it's the owner, the same way the phone unlocks: fingerprint, face, or
 * PIN / pattern / password. Uses Android's own prompt, so Papyrus never sees any of it.
 */
object DeviceAuth {
    /** False when the phone has no screen lock: there is nothing to confirm with. */
    fun isAvailable(context: Context): Boolean =
        context.getSystemService(KeyguardManager::class.java).isDeviceSecure
}

/**
 * Returns a function that shows the phone's unlock prompt and calls [onSuccess] if it succeeds.
 * Android 10+ shows the biometric prompt with PIN as an alternative; older versions show the
 * phone's own PIN / pattern screen.
 */
@Composable
fun rememberDeviceAuth(title: String, onSuccess: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val success by rememberUpdatedState(onSuccess)
    val keyguard = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) success()
    }
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val builder = BiometricPrompt.Builder(context).setTitle(title)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            } else {
                @Suppress("DEPRECATION")
                builder.setDeviceCredentialAllowed(true)
            }
            builder.build().authenticate(
                CancellationSignal(),
                context.mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = success()
                },
            )
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(KeyguardManager::class.java)
                .createConfirmDeviceCredentialIntent(title, null)
                ?.let(keyguard::launch)
        }
    }
}
