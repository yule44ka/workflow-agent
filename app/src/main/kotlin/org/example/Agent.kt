package org.example

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.prompt.executor.llms.Executors.promptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
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

import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.reflect.tools
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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.lang.System.getenv

private val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
            }
        )
    }
}

val domain: String = getenv("DOMAIN") ?: throw IllegalStateException("DOMAIN environment variable is not set")
val token: String = getenv("YOUTRACK_TOKEN") ?: throw IllegalStateException("YOUTRACK_TOKEN environment variable is not set")

@Tool
@LLMDescription("Get documentation about YouTrack workflows.")
fun readWorkflowContext(): String {
    println("📥 Reading workflow context documentation...")
    val resourceName = "workflow_context.md"
    val cl = Thread.currentThread().contextClassLoader

    val result = cl.getResourceAsStream(resourceName)
        ?.reader(Charsets.UTF_8)
        ?.readText()
        ?: ""
    
    println("📤 Loaded ${result.length} characters of documentation")
    return result
}

@LLMDescription("Tools for getting info from YouTrack.")
class MyToolSet : ToolSet {
    @Tool
    @LLMDescription("Validate if YouTrack project exists by id.")
    fun validateProject(
        @LLMDescription("Id of project to validate.")
        projectId: String
    ): String {
        println("📥 Validating project: $projectId")
        return runBlocking {
            val url = "$domain/api/admin/projects/$projectId?fields=id,name"
            val response = client.get(url) {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                response.body()
            } else {
                "Project with id '$projectId' does not exist."
            }
        }
    }

    @Tool
    @LLMDescription("Get all workflows for YouTrack project.")
    fun getProjectWorkflows(projectId: String): List<Workflow> = runBlocking {
        println("🔍 Fetching workflows for project: $projectId")

        val url = "$domain/api/admin/projects/$projectId/workflows?fields=workflow(id,name)"

        val response = client.get(url) {
            header("Authorization", "Bearer $token")
        }

        // Print status + basic info
        println("📡 GET $url → ${response.status}")

        if (!response.status.isSuccess()) {
            println("❌ Failed to load workflows for project $projectId")
            return@runBlocking emptyList()
        }

        // Read raw JSON (for debug)
        val rawJson = response.bodyAsText()
        println("📥 Raw JSON:\n$rawJson")

        // Deserialize
        val usages: List<WorkflowUsageResponse> = try {
            response.body()
        } catch (e: Exception) {
            println("⚠️ Failed to deserialize response: ${e.message}")
            return@runBlocking emptyList()
        }

        val workflows = usages.mapNotNull { it.workflow }

        println("✅ Found ${workflows.size} workflow(s): ${workflows.joinToString { it.name }}")

        workflows
    }

    @Tool
    @LLMDescription("Get all enabled rules for project by workflow.")
    suspend fun getEnabledRulesForWorkflow(
        @LLMDescription("Project ID is its name in YouTrack it should be letters for example NEW.")
        projectId: String,
        workflow: Workflow): List<WorkflowRule> {
        println("📥 Getting enabled rules for workflow '${workflow.name}' (${workflow.id}) in project: $projectId")
        val url = "$domain/api/admin/apps/${workflow.id}?fields=pluggableObjects(id,name,description,script(id,script),usages(enabled,configuration(project(shortName))))"
        val response = client.get(url) {
            header("Authorization", "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            println("📤 Failed to retrieve workflow rules")
            return emptyList()
        }
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
        println("📤 Found ${rules.size} enabled rule(s): ${rules.joinToString { it.ruleName }}")
        return rules
    }

}


val youtrackTools = MyToolSet()

val agent = AIAgent(
    promptExecutor = simpleAnthropicExecutor(
        getenv("ANTHROPIC_API_KEY") ?: throw IllegalStateException("ANTHROPIC_API_KEY environment variable is not set")
    ),
//    strategy = singleRunStrategy(),
    systemPrompt = """
        You are a youtrack workflow specialist. 
        You need to help user with his problem.
        You need to find a workflow rule that made some action in YouTrack by user's description.
        Expected output: explanation why it happened + links to workflow rules that potentially could lead to this behaviour.
        Usual link for workflow looks like this: $domain/projects/<projectId>?tab=workflow&selected=<workflowId>
    """.trimIndent(),
    llmModel = AnthropicModels.Sonnet_4,
    toolRegistry = ToolRegistry {
        readWorkflowContext()
        tools(youtrackTools)
    },
    maxIterations = 100
) {
    handleEvents {
        onToolCallStarting { ctx ->
            println("\n═══════════════════════════════════════════")
            println("🔧 Tool Called: ${ctx.tool.name}")
            println("═══════════════════════════════════════════")
        }

        onToolCallCompleted { ctx ->
            println("✅ Tool Completed: ${ctx.tool.name}")
            println("═══════════════════════════════════════════\n")
        }

        onToolCallFailed { ctx ->
            println("❌ Tool Failed: ${ctx.tool.name}")
            println("═══════════════════════════════════════════\n")
        }
    }
}

fun main() = runBlocking {
    println("Please describe your YouTrack problem:")
    val input = "When I create simple issue without parameters in DEMO project it failes with error"

    val result = agent.run(input)
    println("\n--- Result ---")
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