package com.bayera.travel.common.payment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object ChapaManager {
    // ⚠️ ENSURE YOUR KEY IS CORRECT HERE
    private const val CHAPA_PUBLIC_KEY = "CHAPUBK-RMBiuos5FVUuNSjGYEANHhjFDJaCkTuk"
    
    // Suspend function to run in background
    suspend fun initializePayment(
        email: String,
        amount: Double,
        firstName: String,
        lastName: String,
        txRef: String,
        callback: (String?) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
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
                
                val body = json.toString().toRequestBody(mediaType)
                
                val request = Request.Builder()
                    .url("https://api.chapa.co/v1/transaction/initialize")
                    .post(body)
                    .addHeader("Authorization", "Bearer $CHAPA_PUBLIC_KEY")
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val resBody = response.body?.string()

                if (response.isSuccessful && resBody != null) {
                    val resJson = JSONObject(resBody)
                    val checkoutUrl = resJson.getJSONObject("data").getString("checkout_url")
                    withContext(Dispatchers.Main) { callback(checkoutUrl) }
                } else {
                    withContext(Dispatchers.Main) { callback(null) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { callback(null) }
            }
        }
    }
}
