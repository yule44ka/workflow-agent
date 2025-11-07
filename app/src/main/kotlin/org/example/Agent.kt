package org.example

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import kotlinx.coroutines.runBlocking
import org.example.tools.YouTrackToolSet
import org.example.tools.readWorkflowContext
import java.lang.System.getenv

val youtrackTools = YouTrackToolSet()

val agent = AIAgent(
    promptExecutor = simpleAnthropicExecutor(
        getenv("ANTHROPIC_API_KEY") ?: throw IllegalStateException("ANTHROPIC_API_KEY environment variable is not set")
    ),
    systemPrompt = """
        You are a youtrack workflow specialist. 
        You must help user with his problem.
        You need to find a workflow rule that made some action in YouTrack by user's description.
        Provide short explanation why it happened + links to workflow rules that potentially could lead to this behaviour.
        DO NOT add any code.
        Usual link for workflow looks like this: ${org.example.tools.domain}/projects/<projectId>?tab=workflow&selected=<workflowId>
    """.trimIndent(),
    llmModel = AnthropicModels.Sonnet_4,
    toolRegistry = ToolRegistry {
        tools(youtrackTools)
    },
    maxIterations = 100
) {
    handleEvents {
        onToolCallStarting { ctx ->
            "Tool ${ctx.tool.name}, args ${
                ctx.toolArgs.toString().replace('\n', ' ').take(100)
            }..."
        }
    }
}
