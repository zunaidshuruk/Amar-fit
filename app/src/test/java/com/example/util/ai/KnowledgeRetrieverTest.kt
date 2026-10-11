package com.example.util.ai

import com.example.data.local.CoachTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeRetrieverTest {

    private val topic1 = CoachTopic("Magnesium Supplementation", "Banana Peel Tea", "Provides magnesium and relaxation")
    private val topic2 = CoachTopic("Sleep Initiation", "The Power Down Hour", "Relaxation before bed")
    private val topic3 = CoachTopic("Metabolic Health", "Walk After Meals", "Lowers glucose")
    private val topic4 = CoachTopic("Insulin Resistance", "Insulin Crushing ACV and Cinnamon Shot: Organic raw ACV with cinnamon", "Lowers insulin")

    private val testTopics = listOf(topic1, topic2, topic3, topic4)

    @Test
    fun testSearch() {
        val hits = KnowledgeRetriever.search("what should I drink to sleep better", testTopics)
        val hitTopics = hits.map { it.topic }
        assertTrue(hitTopics.contains(topic1))
        assertTrue(hitTopics.contains(topic2))
        assertFalse(hitTopics.contains(topic3))

        val emptyHits = KnowledgeRetriever.search("zzzz qqqq", testTopics)
        assertTrue(emptyHits.isEmpty())
    }

    @Test
    fun testProtocolNames() {
        val hits = listOf(
            KnowledgeHit(topic1, 10),
            KnowledgeHit(topic4, 10)
        )
        val names = KnowledgeRetriever.protocolNames(hits)
        assertEquals(listOf("Banana Peel Tea", "Insulin Crushing ACV and Cinnamon Shot"), names)
    }

    @Test
    fun testNeedsDoctorNote() {
        val acvHits = listOf(KnowledgeHit(topic4, 10))
        assertTrue(KnowledgeRetriever.needsDoctorNote(acvHits, "anything"))
        assertTrue(KnowledgeRetriever.needsDoctorNote(emptyList(), "can I do a 24 hour fast"))

        val bananaHits = listOf(KnowledgeHit(topic1, 10))
        assertFalse(KnowledgeRetriever.needsDoctorNote(bananaHits, "how do I sleep better"))
    }
}
