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

data class PaymentRequest(val email: String, val amount: Double, val firstName: String, val lastName: String, val txRef: String)
data class PaymentResponse(val checkoutUrl: String)

fun main() {
    // FIX: Listen on 0.0.0.0 (Required for Render/Docker)
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) { gson {} }
        
        routing {
            get("/") { call.respondText("Bayera Backend is Running! 🚀") }
            
            post("/api/pay") {
                // ... (Payment Logic - Keeping it same as before) ...
                try {
                    val req = call.receive<PaymentRequest>()
                    println("Received Payment Request: $req") // LOGGING

                    val secretKey = System.getenv("CHAPA_SECRET_KEY")
                    if (secretKey.isNullOrEmpty()) {
                        println("Error: Missing Secret Key")
                        call.respond(HttpStatusCode.InternalServerError, "Server Config Error")
                        return@post
                    }

                    val client = OkHttpClient()
                    val mediaType = "application/json".toMediaType()
                    val json = JSONObject()
                    json.put("amount", req.amount)
                    json.put("currency", "ETB")
                    json.put("email", req.email)
                    json.put("first_name", req.firstName)
                    json.put("last_name", req.lastName)
                    json.put("tx_ref", req.txRef)
                    json.put("callback_url", "https://google.com")
                    json.put("return_url", "https://google.com")

                    val body = json.toString().toRequestBody(mediaType)
                    val request = Request.Builder()
                        .url("https://api.chapa.co/v1/transaction/initialize")
                        .post(body)
                        .addHeader("Authorization", "Bearer $secretKey")
                        .build()

                    val response = client.newCall(request).execute()
                    val resBody = response.body?.string() ?: ""
                    println("Chapa Response: $resBody")

                    if (response.isSuccessful) {
                        val resJson = JSONObject(resBody)
                        val url = resJson.getJSONObject("data").getString("checkout_url")
                        call.respond(PaymentResponse(url))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, resBody)
                    }
                } catch (e: Exception) {
                    println("Exception: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, e.message ?: "Unknown")
                }
            }
        }
    }.start(wait = true)
}
