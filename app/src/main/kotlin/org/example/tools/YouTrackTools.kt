package org.example.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.json.Json
import org.example.model.AppDetails
import org.example.model.Workflow
import org.example.model.WorkflowRule
import org.example.model.WorkflowUsageResponse
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

@LLMDescription("Tools for getting info from and about YouTrack.")
class YouTrackToolSet : ToolSet {
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

    @Tool
    @LLMDescription("Validate if YouTrack project exists by its name.")
    fun validateProject(
        @LLMDescription("Name of project you want to validate.")
        projectName: String
    ): String {
        return runBlocking {
            val url = "$domain/api/admin/projects/$projectName?fields=id,name"
            val response = client.get(url) {
                header("Authorization", "Bearer $token")
            }
            if (response.status.isSuccess()) {
                response.body()
            } else {
                "Project with id '$projectName' does not exist."
            }
        }
    }

    private suspend fun fetchProjectWorkflows(projectName: String): List<Workflow> {
        val url = "$domain/api/admin/projects/$projectName/workflows?fields=workflow(id,name)"
        val response = client.get(url) {
            header("Authorization", "Bearer $token")
        }
        if (!response.status.isSuccess()) return emptyList()
        val usages: List<WorkflowUsageResponse> = try {
            response.body()
        } catch (_: Exception) {
            emptyList()
        }
        return usages.mapNotNull { it.workflow }
    }


    suspend fun getEnabledRulesForWorkflow(
        projectName: String,
        workflow: Workflow
    ): List<WorkflowRule> {
        val url = "$domain/api/admin/apps/${workflow.id}?fields=pluggableObjects(id,name,description,script(id,script),usages(enabled,configuration(project(shortName))))"
        val response = client.get(url) {
            header("Authorization", "Bearer $token")
        }
        if (!response.status.isSuccess()) {
            return emptyList()
        }
        val app: AppDetails = response.body()
        val rules = mutableListOf<WorkflowRule>()
        for (po in app.pluggableObjects) {
            val enabledForProject = po.usages.any { usage ->
                usage.enabled && usage.configuration?.project?.shortName?.equals(projectName, ignoreCase = true) == true
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

    @Tool
    @LLMDescription("Get all enabled rules for a project across all workflows. Executes per-workflow requests in parallel for speed.")
    suspend fun getEnabledRulesForProject(
        @LLMDescription("Project name for its rules retrieving, e.g. NEW")
        projectName: String
    ): List<WorkflowRule> = coroutineScope {
        val workflows = fetchProjectWorkflows(projectName)
        if (workflows.isEmpty()) return@coroutineScope emptyList()

        val deferred = workflows.map { wf ->
            async {
                runCatching { getEnabledRulesForWorkflow(projectName, wf) }
                    .getOrElse { emptyList() }
            }
        }
        deferred.awaitAll().flatten()
    }
}

