package com.example.util.ai

import com.example.data.local.DailyMetric
import com.example.data.local.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AssistantRouterTest {

    @Test
    fun testCloudSettingAlwaysCloud() {
        val r1 = AssistantRouter.route("Hello", AiEngineSetting.CLOUD, modelReady = true, online = true)
        assertEquals(AssistantRoute.CLOUD, r1)
        val r2 = AssistantRouter.route("i ate 2 eggs", AiEngineSetting.CLOUD, modelReady = true, online = false)
        assertEquals(AssistantRoute.CLOUD, r2)
    }

    @Test
    fun testAutoSettingRouting() {
        // AUTO + ready + "Explain what my resting heart rate means" -> ON_DEVICE
        val r1 = AssistantRouter.route("Explain what my resting heart rate means", AiEngineSetting.AUTO, modelReady = true, online = true)
        assertEquals(AssistantRoute.ON_DEVICE, r1)

        // AUTO + "i ate 2 eggs for breakfast" -> CLOUD
        val r2 = AssistantRouter.route("i ate 2 eggs for breakfast", AiEngineSetting.AUTO, modelReady = true, online = true)
        assertEquals(AssistantRoute.CLOUD, r2)

        // AUTO + "What is a healthy blood glucose range?" -> CLOUD
        val r3 = AssistantRouter.route("What is a healthy blood glucose range?", AiEngineSetting.AUTO, modelReady = true, online = true)
        assertEquals(AssistantRoute.CLOUD, r3)

        // AUTO + "Give me a workout plan" -> CLOUD
        val r4 = AssistantRouter.route("Give me a workout plan", AiEngineSetting.AUTO, modelReady = true, online = true)
        assertEquals(AssistantRoute.CLOUD, r4)

        // AUTO + a 500-character message -> CLOUD
        val longMsg = "a".repeat(500)
        val r5 = AssistantRouter.route(longMsg, AiEngineSetting.AUTO, modelReady = true, online = true)
        assertEquals(AssistantRoute.CLOUD, r5)

        // AUTO + "আমার ক্যালরি কত?" -> ON_DEVICE
        val r6 = AssistantRouter.route("আমার ক্যালরি কত?", AiEngineSetting.AUTO, modelReady = true, online = true)
        assertEquals(AssistantRoute.ON_DEVICE, r6)

        // AUTO + "I have chest pain" -> ON_DEVICE
        val r7 = AssistantRouter.route("I have chest pain", AiEngineSetting.AUTO, modelReady = true, online = true)
        assertEquals(AssistantRoute.ON_DEVICE, r7)

        // AUTO + model not ready -> CLOUD
        val r8 = AssistantRouter.route("Hello", AiEngineSetting.AUTO, modelReady = false, online = true)
        assertEquals(AssistantRoute.CLOUD, r8)

        // AUTO + offline + "give me a diet chart" -> NEEDS_INTERNET
        val r9 = AssistantRouter.route("give me a diet chart", AiEngineSetting.AUTO, modelReady = true, online = false)
        assertEquals(AssistantRoute.NEEDS_INTERNET, r9)

        // AUTO + offline + "How do I sleep better?" -> ON_DEVICE
        val r10 = AssistantRouter.route("How do I sleep better?", AiEngineSetting.AUTO, modelReady = true, online = false)
        assertEquals(AssistantRoute.ON_DEVICE, r10)

        // New AP-4b routing tests:
        assertEquals(AssistantRoute.ON_DEVICE, AssistantRouter.route("What should I drink to sleep better?", AiEngineSetting.AUTO, modelReady = true, online = true))
        assertEquals(AssistantRoute.ON_DEVICE, AssistantRouter.route("How did I sleep this week?", AiEngineSetting.AUTO, modelReady = true, online = true))
        assertEquals(AssistantRoute.ON_DEVICE, AssistantRouter.route("What was my average glucose this week?", AiEngineSetting.AUTO, modelReady = true, online = true))
        assertEquals(AssistantRoute.CLOUD, AssistantRouter.route("Is my glucose normal?", AiEngineSetting.AUTO, modelReady = true, online = true))
        assertEquals(AssistantRoute.CLOUD, AssistantRouter.route("give me a recipe for better sleep", AiEngineSetting.AUTO, modelReady = true, online = true))
    }

    @Test
    fun testOnDeviceSettingRouting() {
        // ON_DEVICE + model not ready -> NEEDS_SETUP
        val r1 = AssistantRouter.route("Hello", AiEngineSetting.ON_DEVICE, modelReady = false, online = true)
        assertEquals(AssistantRoute.NEEDS_SETUP, r1)

        // ON_DEVICE + "log my lunch" -> NEEDS_CLOUD_ENGINE
        val r2 = AssistantRouter.route("log my lunch", AiEngineSetting.ON_DEVICE, modelReady = true, online = true)
        assertEquals(AssistantRoute.NEEDS_CLOUD_ENGINE, r2)

        // ON_DEVICE + "I have chest pain" -> ON_DEVICE
        val r3 = AssistantRouter.route("I have chest pain", AiEngineSetting.ON_DEVICE, modelReady = true, online = true)
        assertEquals(AssistantRoute.ON_DEVICE, r3)
    }

    @Test
    fun testStripFooter() {
        val text = "Hello" + AssistantRouter.ON_DEVICE_FOOTER
        assertEquals("Hello", AssistantRouter.stripFooter(text))
        assertEquals("Hello", AssistantRouter.stripFooter("Hello"))

        val tail = AssistantRouter.buildFooter("sleep (last 30 days)", listOf("Banana Peel Tea"), true)
        val fullText = "Hello" + tail
        assertEquals("Hello", AssistantRouter.stripFooter(fullText))
    }

    @Test
    fun testBuildContext() {
        val profile = UserProfile(age = 30, gender = "Male", weightKg = 70f, heightCm = 172f, activityLevel = "Moderate")
        val metrics = listOf(
            DailyMetric(date = "2026-10-01", steps = 5000, sleepHours = 7f),
            DailyMetric(date = "2026-10-02", steps = 8000, sleepHours = 8f)
        )
        val contextStr = AssistantRouter.buildContext(profile, metrics)
        assertTrue(contextStr.contains("age 30"))
        assertTrue(contextStr.contains("70.0 kg") || contextStr.contains("70 kg"))
        assertTrue(contextStr.contains("8000 steps"))
    }

    @Test
    fun testDeepRequestAndHealthQuestions() {
        assertTrue(AssistantRouter.isDeepRequest(AssistantRouter.DEEP_REVIEW_PROMPT))
        assertTrue(AssistantRouter.isDeepRequest(AssistantRouter.GO_DEEPER_PREFIX + "how is my sleep"))
        org.junit.Assert.assertFalse(AssistantRouter.isDeepRequest("hello"))
        org.junit.Assert.assertFalse(AssistantRouter.isDeepRequest("what is a deep review"))

        assertTrue(AssistantRouter.isCloudOnlyTopic(AssistantRouter.DEEP_REVIEW_PROMPT))
        assertEquals(AssistantRoute.CLOUD, AssistantRouter.route(AssistantRouter.DEEP_REVIEW_PROMPT, AiEngineSetting.AUTO, modelReady = true, online = true))
        assertEquals(AssistantRoute.NEEDS_INTERNET, AssistantRouter.route(AssistantRouter.DEEP_REVIEW_PROMPT, AiEngineSetting.AUTO, modelReady = true, online = false))
        assertEquals(AssistantRoute.NEEDS_CLOUD_ENGINE, AssistantRouter.route(AssistantRouter.DEEP_REVIEW_PROMPT, AiEngineSetting.ON_DEVICE, modelReady = true, online = true))

        assertTrue(AssistantRouter.isHealthQuestion("how did I sleep this week"))
        assertTrue(AssistantRouter.isHealthQuestion("what should I eat for lunch"))
        org.junit.Assert.assertFalse(AssistantRouter.isHealthQuestion("hello"))
    }
}
