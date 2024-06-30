package com.lymors.lycommons.data.auth.googleauth

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.lymors.lycommons.utils.MyResult
import javax.inject.Inject

class AuthRepositoryWithGoogleImpl @Inject constructor(private val auth: FirebaseAuth) : AuthRepositoryWithGoogle {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private var onSignInResult: ((task: Task<AuthResult>?, account: GoogleSignInAccount?, exception: Exception?) -> Unit)? = null

    override fun signInWithGoogle(activity: AppCompatActivity, serverClientId: String, callback: (task: Task<AuthResult>?, account: GoogleSignInAccount?, exception: Exception?) -> Unit) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(serverClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(activity, gso)
        onSignInResult = callback

        signInLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(activity, account)
                } catch (e: ApiException) {
                    onSignInResult?.invoke(null, null, e)
                }
            } else {
                onSignInResult?.invoke(null, null, Exception("Sign in canceled"))
            }
        }

        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    override fun getGoogleAccount(
        activity: AppCompatActivity,
        serverClientId: String,
        accountCallback: (account: GoogleSignInAccount?) -> Unit
    ) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(serverClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(activity, gso)

        signInLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    accountCallback.invoke(account)
                } catch (e: ApiException) {
                    accountCallback.invoke(null)
                }
            } else {
                accountCallback.invoke(null)
            }
        }
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }


    // Sign out method
    override fun signOut(activity: AppCompatActivity ,  serverClientId: String , onSignOutResult:(MyResult<String>) ->Unit) {
        auth.signOut() // Firebase sign out
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(serverClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(activity, gso)

        googleSignInClient.signOut().addOnCompleteListener(activity) {
            if (it.isSuccessful) {
                onSignOutResult.invoke(MyResult.Success("Signed out successfully"))
            } else {
                onSignOutResult.invoke(MyResult.Error("Failed to sign out due to ${it.exception?.message}"))
            }
        }
    }

    private fun firebaseAuthWithGoogle(activity: AppCompatActivity, account: GoogleSignInAccount?) {
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    onSignInResult?.invoke(task, account, null)
                } else {
                    onSignInResult?.invoke(task, account, task.exception)
                }
            }
    }


}
