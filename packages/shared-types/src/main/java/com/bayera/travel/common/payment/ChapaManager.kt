package com.bayera.travel.common.payment

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object ChapaManager {
    // YOUR LIVE BACKEND URL
    private const val BACKEND_URL = "https://bayra-travel.onrender.com/api/pay"
    
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
                    val checkoutUrl = resJson.optString("checkoutUrl")
                    callback(checkoutUrl)
                } else {
                    // Log error for debugging
                    println("Backend Error: " + resBody)
                    callback(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }.start()
    }
}
