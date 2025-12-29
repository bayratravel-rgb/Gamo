package com.bayera.travel.common.payment

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object ChapaManager {
    // 🔒 SECURE: Pointing to your live backend
    private const val BACKEND_URL = "https://bayra-travel.onrender.com"
    
    fun initializePayment(
        email: String,
        amount: Double,
        firstName: String,
        lastName: String,
        txRef: String,
        callback: (String?) -> Unit
    ) {
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()
        
        val json = JSONObject()
        json.put("amount", amount.toString())
        json.put("email", email)
        json.put("firstName", firstName)
        json.put("lastName", lastName)
        json.put("txRef", txRef)

        val body = json.toString().toRequestBody(mediaType)
        
        // Calling your Ktor Server Endpoint
        val request = Request.Builder()
            .url("$BACKEND_URL/api/pay")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val resBody = response.body?.string()
                
                if (response.isSuccessful && resBody != null) {
                    val resJson = JSONObject(resBody)
                    // The server returns { "checkoutUrl": "..." }
                    val checkoutUrl = resJson.optString("checkoutUrl")
                    if (checkoutUrl.isNotEmpty()) {
                        callback(checkoutUrl)
                    } else {
                        callback(null)
                    }
                } else {
                    System.out.println("Backend Error: $resBody")
                    callback(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }.start()
    }
}
