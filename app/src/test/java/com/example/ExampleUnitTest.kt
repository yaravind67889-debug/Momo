package com.example

import com.example.data.remote.AiServiceClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun autonomousPlanner_generatesSearchSteps() {
        val aiClient = AiServiceClient()
        val plan = aiClient.generateAutonomousPlan("Search for latest Android Jetpack Compose updates", emptyList())
        assertTrue("Steps should not be empty", plan.steps.isNotEmpty())
        assertEquals("browser_open", plan.steps[0].toolName)
        assertTrue(plan.steps.any { it.toolName == "browser_read_page" })
        assertTrue(plan.steps.any { it.toolName == "memory_save" })
    }

    @Test
    fun autonomousPlanner_flagsSensitivePaymentActions() {
        val aiClient = AiServiceClient()
        val plan = aiClient.generateAutonomousPlan("Order and make payment for new laptop", emptyList())
        val sensitiveStep = plan.steps.firstOrNull { it.isSensitive }
        assertTrue("Financial action must be marked sensitive requiring owner approval", sensitiveStep != null)
    }

    @Test
    fun autonomousPlanner_flagsSensitiveMessageActions() {
        val aiClient = AiServiceClient()
        val plan = aiClient.generateAutonomousPlan("Send email message to contact@example.com", emptyList())
        val sensitiveStep = plan.steps.firstOrNull { it.isSensitive }
        assertTrue("Sending email must be marked sensitive requiring owner approval", sensitiveStep != null)
    }
}
