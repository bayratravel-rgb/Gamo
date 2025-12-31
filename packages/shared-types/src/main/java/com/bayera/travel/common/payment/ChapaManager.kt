package com.bayera.travel.common.payment

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ChapaManager {
    private const val BACKEND_URL = "https://bayra-travel.onrender.com/api/pay"
    
    // Config: Allow 60 seconds timeout for Render Cold Start
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    fun initializePayment(
        email: String,
        amount: Double,
        firstName: String,
        lastName: String,
        txRef: String,
        callback: (String?) -> Unit
    ) {
        val mediaType = "application/json".toMediaType()
        val json = JSONObject()
        json.put("amount", amount.toString())
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
                val resBody = response.body?.string()
                
                if (response.isSuccessful && resBody != null) {
                    val resJson = JSONObject(resBody)
                    // Check if key exists before accessing
                    if (resJson.has("checkoutUrl")) {
                        val checkoutUrl = resJson.getString("checkoutUrl")
                        callback(checkoutUrl)
                    } else {
                        println("JSON missing checkoutUrl: $resBody")
                        callback(null)
                    }
                } else {
                    println("Backend Error Code: ${response.code} Body: $resBody")
                    callback(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }.start()
    }
}
