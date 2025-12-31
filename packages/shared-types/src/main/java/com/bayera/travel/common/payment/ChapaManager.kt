package com.bayera.travel.common.payment

import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ChapaManager {
    private const val BACKEND_URL = "https://bayra-travel.onrender.com/api/pay"

    fun initializePayment(
        email: String,
        amount: Double,
        firstName: String,
        lastName: String,
        txRef: String,
        callback: (String?, String?) -> Unit // Changed to return (URL?, Error?)
    ) {
        Thread {
            try {
                val url = URL(BACKEND_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val json = JSONObject()
                json.put("amount", amount)
                json.put("email", email)
                json.put("firstName", firstName)
                json.put("lastName", lastName)
                json.put("txRef", txRef)

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(json.toString())
                writer.flush()
                writer.close()

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    val resJson = JSONObject(response.toString())
                    // Check if server sent a checkout URL or an error message inside the JSON
                    val checkoutUrl = resJson.optString("checkoutUrl")
                    if (checkoutUrl.isNotEmpty()) {
                        callback(checkoutUrl, null)
                    } else {
                        // Sometimes server returns 200 but with error message
                        callback(null, "Server said: $response")
                    }
                } else {
                    // Read Error Stream
                    val reader = BufferedReader(InputStreamReader(conn.errorStream))
                    val errorResponse = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        errorResponse.append(line)
                    }
                    callback(null, "HTTP $responseCode: $errorResponse")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null, "Exception: ${e.message}")
            }
        }.start()
    }
}
