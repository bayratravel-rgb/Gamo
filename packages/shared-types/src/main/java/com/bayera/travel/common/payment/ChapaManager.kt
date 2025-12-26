package com.bayera.travel.common.payment

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object ChapaManager {
    // YOUR REAL KEY
    private const val CHAPA_PUBLIC_KEY = "CHAPUBK-RMBiuos5FVUuNSjGYEANHhjFDJaCkTuk" 
    
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
        json.put("currency", "ETB")
        json.put("email", email)
        json.put("first_name", firstName)
        json.put("last_name", lastName)
        json.put("tx_ref", txRef)
        json.put("callback_url", "https://google.com")
        json.put("return_url", "https://google.com") // Returns to app eventually

        val body = json.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("https://api.chapa.co/v1/transaction/initialize")
            .post(body)
            .addHeader("Authorization", "Bearer $CHAPA_PUBLIC_KEY")
            .addHeader("Content-Type", "application/json")
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val resBody = response.body?.string()
                    val resJson = JSONObject(resBody)
                    val checkoutUrl = resJson.getJSONObject("data").getString("checkout_url")
                    callback(checkoutUrl)
                } else {
                    callback(null)
                }
            } catch (e: Exception) {
                callback(null)
            }
        }.start()
    }
}
