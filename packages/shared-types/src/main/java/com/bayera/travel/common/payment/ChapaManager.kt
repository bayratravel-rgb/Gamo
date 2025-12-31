package com.bayera.travel.common.payment

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ChapaManager {
    // ENSURE THIS MATCHES YOUR BROWSER URL EXACTLY
    private const val BACKEND_URL = "https://bayra-travel.onrender.com/api/pay"
    
    fun initializePayment(
        email: String,
        amount: Double,
        firstName: String,
        lastName: String,
        txRef: String,
        callback: (String?) -> Unit
    ) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // Give it time to wake up
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        
        val json = JSONObject()
        json.put("amount", amount)
        json.put("email", email)
        json.put("firstName", firstName)
        json.put("lastName", lastName)
        json.put("txRef", txRef)

        val body = json.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(BACKEND_URL)
            .post(body)
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val resBody = response.body?.string() ?: ""
                
                if (response.isSuccessful) {
                    val resJson = JSONObject(resBody)
                    val checkoutUrl = resJson.optString("checkoutUrl")
                    if (checkoutUrl.isNotEmpty()) {
                        callback(checkoutUrl)
                    } else {
                        Log.e("Chapa", "No checkoutUrl in response: $resBody")
                        callback(null)
                    }
                } else {
                    Log.e("Chapa", "Server Error ${response.code}: $resBody")
                    callback(null)
                }
            } catch (e: Exception) {
                Log.e("Chapa", "Network Error: ${e.message}")
                e.printStackTrace()
                callback(null)
            }
        }.start()
    }
}
