package com.bayera.travel.common.payment

import android.os.AsyncTask
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
        callback: (String?) -> Unit
    ) {
        // Run network on background thread manually
        Thread {
            try {
                val url = URL(BACKEND_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.doInput = true

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
                    val checkoutUrl = resJson.optString("checkoutUrl")
                    
                    // Callback must run on this thread (Caller handles UI switch)
                    callback(checkoutUrl)
                } else {
                    println("Server Error: $responseCode")
                    callback(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }.start()
    }
}
