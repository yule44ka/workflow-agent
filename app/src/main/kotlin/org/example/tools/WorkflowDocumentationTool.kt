package org.example.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool

@Tool
@LLMDescription("Get documentation about YouTrack Workflows.")
fun readWorkflowDocumentation(): String {
    val resourceName = "workflow_context.md"
    val cl = Thread.currentThread().contextClassLoader

    return cl.getResourceAsStream(resourceName)
        ?.reader(Charsets.UTF_8)
        ?.readText()
        ?: ""
}

