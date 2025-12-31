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

            post("/api/pay") {
                try {
                    println("Received Payment Request...") // LOG 1
                    
                    val req = call.receive<PaymentRequest>()
                    println("Request Data: $req") // LOG 2
                    
                    val secretKey = System.getenv("CHAPA_SECRET_KEY")
                    if (secretKey.isNullOrEmpty()) {
                        println("CRITICAL ERROR: CHAPA_SECRET_KEY is missing or empty!") // LOG 3
                        call.respond(HttpStatusCode.InternalServerError, "Server Config Error: Missing Secret Key")
                        return@post
                    }

                    println("Calling Chapa API...") // LOG 4
                    val client = OkHttpClient()
                    val mediaType = "application/json".toMediaType()
                    val json = JSONObject()
                    json.put("amount", req.amount.toString())
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
                        .addHeader("Content-Type", "application/json")
                        .build()

                    val response = client.newCall(request).execute()
                    val resBody = response.body?.string() ?: ""
                    
                    println("Chapa Response Code: ${response.code}") // LOG 5
                    println("Chapa Response Body: $resBody") // LOG 6

                    if (response.isSuccessful) {
                        val resJson = JSONObject(resBody)
                        val data = resJson.optJSONObject("data")
                        val url = data?.optString("checkout_url")
                        
                        if (!url.isNullOrEmpty()) {
                            call.respond(PaymentResponse(url))
                        } else {
                            call.respond(HttpStatusCode.InternalServerError, "Chapa Error: No URL returned")
                        }
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Chapa Failed: $resBody")
                    }
                } catch (e: Exception) {
                    e.printStackTrace() // Print stack trace to logs
                    call.respond(HttpStatusCode.InternalServerError, "Exception: ${e.message}")
                }
            }
        }
    }.start(wait = true)
}
