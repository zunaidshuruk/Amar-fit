package com.example

import com.example.data.remote.Content
import com.example.data.remote.GenerateContentRequest
import com.example.data.remote.Part
import com.example.data.remote.RetrofitClient
import kotlinx.coroutines.runBlocking
import org.junit.Test
import retrofit2.HttpException

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    val apiKey = BuildConfig.GEMINI_API_KEY
    println("DEBUG: apiKey=$apiKey")
    try {
      runBlocking {
        val request = GenerateContentRequest(
          contents = listOf(Content(role = "user", parts = listOf(Part(text = "hello")))),
          systemInstruction = Content(parts = listOf(Part(text = "You are KardIQ AI")))
        )
        val response = RetrofitClient.service.generateContent("gemini-3.1-flash-lite-preview", apiKey, request)
        println("DEBUG: generateContent success: $response")
      }
    } catch (e: HttpException) {
      val err = e.response()?.errorBody()?.string()
      println("DEBUG: generateContent HttpException: ${e.code()} body: $err")
    } catch (e: Exception) {
      println("DEBUG: generateContent error: $e")
    }

    try {
      runBlocking {
        val request = GenerateContentRequest(
          contents = listOf(Content(role = "user", parts = listOf(Part(text = "hello")))),
          systemInstruction = Content(parts = listOf(Part(text = "You are KardIQ AI")))
        )
        val response = RetrofitClient.service.streamGenerateContent("gemini-3.1-flash-lite-preview", apiKey, "sse", request)
        println("DEBUG: streamGenerateContent success: $response")
        val line = response.source().readUtf8Line()
        println("DEBUG: stream first line: $line")
      }
    } catch (e: HttpException) {
      val err = e.response()?.errorBody()?.string()
      println("DEBUG: streamGenerateContent HttpException: ${e.code()} body: $err")
    } catch (e: Exception) {
      println("DEBUG: streamGenerateContent error: $e")
    }
  }
}
