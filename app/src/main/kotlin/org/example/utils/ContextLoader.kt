package org.example.utils

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool


object ContextLoader {
    @Tool
    @LLMDescription("Get documentation about YouTrack workflows.")
    fun readWorkflowContext(): String {
        val resourceName = "workflow_context.md"
        val cl = Thread.currentThread().contextClassLoader

        return cl.getResourceAsStream(resourceName)
            ?.reader(Charsets.UTF_8)
            ?.readText()
            ?: ""
    }
}
