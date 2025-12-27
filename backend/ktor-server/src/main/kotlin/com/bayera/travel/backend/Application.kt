package com.bayera.travel.backend

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import io.ktor.http.*
import io.ktor.server.request.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class PaymentRequest(
    val email: String,
    val amount: Double,
    val firstName: String,
    val lastName: String,
    val txRef: String
)

data class PaymentResponse(val checkoutUrl: String)

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            gson { }
        }
        
        routing {
            get("/") {
                call.respondText("Bayera Backend is Running! 🚀")
            }

            // SECURE ENDPOINT
            post("/api/pay") {
                val req = call.receive<PaymentRequest>()
                
                // 1. Get Key from Environment Variable (SAFE!)
                // On your computer/cloud, you set this variable. It is NOT in the code.
                val secretKey = System.getenv("CHAPA_SECRET_KEY") ?: "CHASECK_TEST-YourFallbackKey"

                // 2. Call Chapa (Server to Server)
                val client = OkHttpClient()
                val mediaType = "application/json".toMediaType()
                val json = JSONObject()
                json.put("amount", req.amount.toString())
                json.put("currency", "ETB")
                json.put("email", req.email)
                json.put("first_name", req.firstName)
                json.put("last_name", req.lastName)
                json.put("tx_ref", req.txRef)
                json.put("callback_url", "https://bayera.com/callback")
                json.put("return_url", "https://bayera.com/return")

                val body = json.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("https://api.chapa.co/v1/transaction/initialize")
                    .post(body)
                    .addHeader("Authorization", "Bearer $secretKey")
                    .addHeader("Content-Type", "application/json")
                    .build()

                try {
                    val response = client.newCall(request).execute()
                    val resBody = response.body?.string()
                    
                    if (response.isSuccessful && resBody != null) {
                        val resJson = JSONObject(resBody)
                        val url = resJson.getJSONObject("data").getString("checkout_url")
                        call.respond(PaymentResponse(url))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Chapa Failed: $resBody")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown Error")
                }
            }
        }
    }.start(wait = true)
}
