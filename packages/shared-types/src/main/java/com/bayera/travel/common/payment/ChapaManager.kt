package com.bayera.travel.common.payment

import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

object ChapaManager {
    fun initializePayment(amount: Double, email: String, firstName: String, lastName: String, txRef: String, secretKey: String): String? {
        return try {
            val url = URL("https://api.chapa.co/v1/transaction/initialize")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $secretKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val jsonInputString = """
                {
                    "amount": "$amount",
                    "currency": "ETB",
                    "email": "$email",
                    "first_name": "$firstName",
                    "last_name": "$lastName",
                    "tx_ref": "$txRef",
                    "callback_url": "https://bayera-backend.onrender.com/callback",
                    "customization": {
                        "title": "Bayera Travel",
                        "description": "Payment for Trip"
                    }
                }
            """.trimIndent()

            OutputStreamWriter(conn.outputStream).use { it.write(jsonInputString) }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val scanner = Scanner(conn.inputStream).useDelimiter("\\A")
                val response = if (scanner.hasNext()) scanner.next() else ""
                // Simple parsing to find the checkout_url
                val regex = "\"checkout_url\":\"(.*?)\"".toRegex()
                regex.find(response)?.groupValues?.get(1)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
