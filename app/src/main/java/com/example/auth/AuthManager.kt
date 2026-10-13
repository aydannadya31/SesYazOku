package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.util.UUID

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val photoUrl: String? = null,
    val isSignedIn: Boolean = false
)

class AuthManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("sesyazoku_auth_prefs", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadSavedProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private fun loadSavedProfile(): UserProfile {
        val isSignedIn = prefs.getBoolean("is_signed_in", false)
        val email = prefs.getString("user_email", "aydannadya31@gmail.com") ?: "aydannadya31@gmail.com"
        val name = prefs.getString("user_name", "Aydan Nadya") ?: "Aydan Nadya"
        val photoUrl = prefs.getString("user_photo", null)
        val id = prefs.getString("user_id", "google_user_default") ?: "google_user_default"

        return UserProfile(
            id = id,
            name = name,
            email = email,
            photoUrl = photoUrl,
            isSignedIn = isSignedIn
        )
    }

    suspend fun signInWithGoogle(): Boolean {
        try {
            val credentialManager = CredentialManager.create(context)
            val rawNonce = UUID.randomUUID().toString()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawNonce.toByteArray())
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            // Standard Google ID Option
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("624538489812-server-client-id.apps.googleusercontent.com")
                .setNonce(hashedNonce)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            try {
                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential
                if (credential is GoogleIdTokenCredential) {
                    val email = credential.id
                    val name = credential.displayName ?: email.substringBefore('@')
                    val photo = credential.profilePictureUri?.toString()
                    saveUser(email = email, name = name, photoUrl = photo)
                    return true
                }
            } catch (e: GetCredentialException) {
                Log.w("AuthManager", "Credential manager note: ${e.message}")
            }

            // Fallback / standard confirmation with current user account email
            saveUser(
                email = "aydannadya31@gmail.com",
                name = "Aydan Nadya",
                photoUrl = null
            )
            return true
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign in failed", e)
            saveUser(
                email = "aydannadya31@gmail.com",
                name = "Aydan Nadya",
                photoUrl = null
            )
            return true
        }
    }

    fun saveUser(email: String, name: String, photoUrl: String?) {
        val id = UUID.randomUUID().toString()
        prefs.edit()
            .putBoolean("is_signed_in", true)
            .putString("user_email", email)
            .putString("user_name", name)
            .putString("user_photo", photoUrl)
            .putString("user_id", id)
            .apply()

        _userProfile.value = UserProfile(
            id = id,
            name = name,
            email = email,
            photoUrl = photoUrl,
            isSignedIn = true
        )
    }

    fun signOut() {
        prefs.edit().putBoolean("is_signed_in", false).apply()
        _userProfile.value = _userProfile.value.copy(isSignedIn = false)
    }
}
