package com.bayera.travel.common.payment

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ChapaManager {
    private const val BASE_URL = "https://bayra-travel.onrender.com/api"
    
    fun initializePayment(email: String, amount: Double, firstName: String, lastName: String, txRef: String, callback: (String?, String?) -> Unit) {
        val client = OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).build()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val json = JSONObject()
        json.put("amount", amount)
        json.put("email", email)
        json.put("firstName", firstName)
        json.put("lastName", lastName)
        json.put("txRef", txRef)

        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder().url("$BASE_URL/pay").post(body).build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val resBody = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val resJson = JSONObject(resBody)
                    val url = resJson.optString("checkoutUrl")
                    if (url.isNotEmpty()) callback(url, null) else callback(null, "No URL")
                } else callback(null, "Server Error: $resBody")
            } catch (e: Exception) { callback(null, e.message) }
        }.start()
    }

    // --- NEW: VERIFY FUNCTION ---
    fun verifyPayment(txRef: String, callback: (Boolean) -> Unit) {
        val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val json = JSONObject()
        json.put("txRef", txRef)
        
        val body = json.toString().toRequestBody(mediaType)
        val request = Request.Builder().url("$BASE_URL/verify").post(body).build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    callback(true) // Verified!
                } else {
                    callback(false)
                }
            } catch (e: Exception) {
                callback(false)
            }
        }.start()
    }
}
