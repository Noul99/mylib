package com.lymors.lycommons.data.auth.phone

import android.app.Activity
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.lymors.lycommons.utils.MyResult
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class AuthRepositoryWithPhoneImpl @Inject constructor(private val auth: FirebaseAuth) :
    AuthRepositoryWithPhone {

    lateinit var callBack :PhoneAuthProvider.OnVerificationStateChangedCallbacks

    override suspend fun signInUser(
        phone: String,
        context: Activity,
        phoneAuthCredential: (PhoneAuthCredential) -> Unit,
        authVerificationFailed: (String) -> Unit,
        codeSend: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit
    ): PhoneAuthProvider.OnVerificationStateChangedCallbacks {
        Log.i("TAG", "repo -> signInUser")
        callBack = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                phoneAuthCredential(p0)
                Log.i("TAG", "repo -> onVerificationCompleted")
            }

            override fun onVerificationFailed(e: FirebaseException) {
                authVerificationFailed(e.message!!)

                Log.i("TAG", "repo -> authVerificationFailed : ${e.message}")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.i("TAG", "repo -> onCodeSent ")
                codeSend(verificationId, token )
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(context)
            .setCallbacks(callBack).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        return callBack
    }

    override suspend fun loginUser(credential: PhoneAuthCredential): MyResult<String> {
        return try {
            Log.i("TAG", "repo -> loginUser")
            auth.signInWithCredential(credential).await()
            MyResult.Success("Login successful")
        } catch (e: Exception) {
            MyResult.Success("${e.message}")
        }
    }

    override suspend fun resendOtp(
        token: PhoneAuthProvider.ForceResendingToken,
        callBack: PhoneAuthProvider.OnVerificationStateChangedCallbacks,
        context: Activity,
        phoneNumber: String
    ): MyResult<String> {
        Log.i("TAG", "repo -> resendOtp")
        return try {
            PhoneAuthProvider.getInstance()
                .verifyPhoneNumber(phoneNumber, 60, TimeUnit.SECONDS, context, callBack, token)
            MyResult.Success("Code send.")
        } catch (e: Exception) {
            MyResult.Error("${e.message}")
        }
    }


}