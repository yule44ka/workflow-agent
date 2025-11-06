package org.example

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.prompt.executor.llms.Executors.promptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import org.example.client.YouTrackClient
import org.example.model.Workflow
import org.example.model.WorkflowRule
import org.example.model.Project
import org.example.model.WorkflowUsageResponse
import org.example.model.AppDetails
import org.example.model.PluggableObject
import org.example.model.Script
import org.example.model.Configuration
import org.example.model.ProjectShort
import org.example.model.Usage
import org.example.utils.SerializationUtils
import org.example.utils.ContextLoader

import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.ext.tool.file.EditFileTool
import ai.koog.agents.ext.tool.file.ListDirectoryTool
import ai.koog.agents.ext.tool.file.ReadFileTool
import ai.koog.agents.ext.tool.file.WriteFileTool
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.rag.base.files.JVMFileSystemProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import java.lang.System.getenv

private val client = HttpClient(CIO)
val domain: String? = getenv("DOMAIN")
val token: String? = getenv("YOUTRACK_TOKEN")

@Tool
@LLMDescription("Validate if YouTrack project exists by id.")
fun validateProject(projectId: String): Project? {
    return runBlocking {
        val url = "$domain/api/admin/projects/$projectId?fields=id,name"
        val response = client.get(url) {
            header("Authorization", "Bearer $token")
        }
        if (response.status.isSuccess()) response.body() else null
    }
}

@Tool
@LLMDescription("Get all workflows for YouTrack project.")
suspend fun getProjectWorkflows(projectId: String): List<Workflow> {
    val url = "$domain/api/admin/projects/$projectId/workflows?fields=workflow(id,name)"
    val response = client.get(url) {
        header("Authorization", "Bearer $token")
    }
    if (!response.status.isSuccess()) return emptyList()
    val usages: List<WorkflowUsageResponse> = response.body()
    return usages.mapNotNull { it.workflow }
}

@Tool
@LLMDescription("Get all enabled rules for project by workflow.")
suspend fun getEnabledRulesForWorkflow(projectId: String, workflow: Workflow): List<WorkflowRule> {
    val url = "$domain/api/admin/apps/${workflow.id}?fields=pluggableObjects(id,name,description,script(id,script),usages(enabled,configuration(project(shortName))))"
    val response = client.get(url) {
        header("Authorization", "Bearer $token")
    }
    if (!response.status.isSuccess()) return emptyList()
    val app: AppDetails = response.body()
    val rules = mutableListOf<WorkflowRule>()
    for (po in app.pluggableObjects) {
        val enabledForProject = po.usages.any { usage ->
            usage.enabled && usage.configuration?.project?.shortName?.equals(projectId, ignoreCase = true) == true
        }
        if (enabledForProject) {
            rules += WorkflowRule(
                ruleId = po.id,
                ruleName = po.name,
                ruleDescription = po.description,
                ruleScript = po.script?.script,
                workflowId = workflow.id,
                workflowName = workflow.name,
            )
        }
    }
    return rules
}


val agent = AIAgent(
    promptExecutor = simpleAnthropicExecutor(getenv("ANTHROPIC_API_KEY")),
//    strategy = singleRunStrategy(),
    systemPrompt = """
        You are a youtrack workflow specialist. 
        You need to help user with his problem.
        You need to find a workflow rule that made some action in YouTrack by user’s description.
        Expected output: explanation why it happened + links to workflow rules that potentially could lead to this behaviour.
        Usual link for workflow looks like this: $domain/<projectId>?tab=workflow&selected=<workflowId>
    """.trimIndent(),
    llmModel = AnthropicModels.Sonnet_4,
    toolRegistry = ToolRegistry {
        validateProject(),
        getProjectWorkflows(),
        getEnabledRulesForWorkflow()
    },
    maxIterations = 100
) {
    handleEvents {
        onToolCallStarting { ctx ->
            println("Tool called: ${ctx.tool.name}")
        }
    }
}

fun main(args: Array<String>) = runBlocking {
    if (args.isEmpty()) {
        println("Error: Please provide the problem")
        return@runBlocking
    }

    val input: String = args[0]
    val result = agent.run(input)
    println(result)
}







//
//
//fun createWorkflowAgent(
//    promptExecutor: PromptExecutor,
//    youtrackClient: YouTrackClient,
//    showMessage: suspend (String) -> String,
//    contextLoader: ContextLoader
//): AIAgent<UserInput, Explanation> {
//
//}
//
//
//private fun agentStrategy(
//    promptExecutor: PromptExecutor,
//    youtrackClient: YouTrackClient,
//    showMessage: suspend (String) -> String,
//    contextLoader: ContextLoader
//) = strategy<UserInput, Explanation>("workflow-strategy") {
//    // Nodes
//
//    // Set additional system instructions
//    val setup by node<UserInput, String> { userInput ->
//
//    }
//}