package org.example

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import org.example.tools.YouTrackToolSet
import org.example.tools.readWorkflowContext
import java.lang.System.getenv

val youtrackTools = YouTrackToolSet()

val agent = AIAgent(
    promptExecutor = simpleAnthropicExecutor(
        getenv("ANTHROPIC_API_KEY") ?: throw IllegalStateException("ANTHROPIC_API_KEY environment variable is not set")
    ),
    llmModel = AnthropicModels.Sonnet_4,
    toolRegistry = ToolRegistry {
        readWorkflowContext()
        tools(youtrackTools)
    },
    systemPrompt = """
        You are a youtrack workflow specialist. 
        You need to help user with his problem.
        You need to find a workflow rule that made some action in YouTrack by user's description.
        Please call all the tools at once.
        Expected output: explanation why it happened + links to workflow rules that potentially could lead to this behaviour.
        Usual link for workflow looks like this: ${org.example.tools.domain}/projects/<projectId>?tab=workflow&selected=<workflowId>
    """.trimIndent(),
    maxIterations = 100
)
// For DEBUG
//{
//    handleEvents {
//        onToolCallStarting { e ->
//            println("Tool called: ${e.tool.name}, args=${e.toolArgs}")
//        }
//        onToolCallCompleted {
//            println("Tool completed: ${it.tool.name}, result=${it.result}")
//        }
//        onAgentExecutionFailed { e ->
//            println("Agent error: ${e.throwable.message}")
//        }
//    }
//}
