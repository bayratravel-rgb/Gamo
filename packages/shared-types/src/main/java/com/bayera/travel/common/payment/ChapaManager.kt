package com.bayera.travel.common.payment
import android.os.AsyncTask
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ChapaManager {
    // YOUR LIVE BACKEND
    private const val BACKEND_URL = "https://bayra-travel.onrender.com/api/pay"
    private const val VERIFY_URL = "https://bayra-travel.onrender.com/api/verify"

    fun initializePayment(email: String, amount: Double, firstName: String, lastName: String, txRef: String, callback: (String?, String?) -> Unit) {
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
                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) response.append(line)
                    val resJson = JSONObject(response.toString())
                    val checkoutUrl = resJson.optString("checkoutUrl")
                    if (checkoutUrl.isNotEmpty()) callback(checkoutUrl, null) else callback(null, "No URL")
                } else callback(null, "Server Error: ${conn.responseCode}")
            } catch (e: Exception) { callback(null, e.message) }
        }.start()
    }
    
    fun verifyPayment(txRef: String, callback: (Boolean) -> Unit) {
        Thread {
            try {
                val url = URL(VERIFY_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                val json = JSONObject()
                json.put("txRef", txRef)
                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(json.toString())
                writer.flush()
                writer.close()
                if (conn.responseCode == 200) callback(true) else callback(false)
            } catch (e: Exception) { callback(false) }
        }.start()
    }
}
