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
import io.ktor.client.statement.bodyAsText
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

@LLMDescription("Tools for getting info from YouTrack.")
class YouTrackToolSet : ToolSet {
    @Tool
    @LLMDescription("Validate if YouTrack project exists by id.")
    fun validateProject(
        @LLMDescription("Id of project to validate.")
        projectId: String
    ): String {
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
        val url = "$domain/api/admin/projects/$projectId/workflows?fields=workflow(id,name)"

        val response = client.get(url) {
            header("Authorization", "Bearer $token")
        }

        if (!response.status.isSuccess()) {
            return@runBlocking emptyList()
        }

        val usages: List<WorkflowUsageResponse> = try {
            response.body()
        } catch (e: Exception) {
            return@runBlocking emptyList()
        }

        usages.mapNotNull { it.workflow }
    }

    // Internal suspend version to avoid blocking when composing other suspend tools
    private suspend fun fetchProjectWorkflows(projectId: String): List<Workflow> {
        val url = "$domain/api/admin/projects/$projectId/workflows?fields=workflow(id,name)"
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

    @Tool
    @LLMDescription("Get all enabled rules for project by workflow.")
    suspend fun getEnabledRulesForWorkflow(
        @LLMDescription("Project ID is its name in YouTrack it should be letters for example NEW.")
        projectId: String,
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

    @Tool
    @LLMDescription("Get all enabled rules for a project across all workflows. Executes per-workflow requests in parallel for speed.")
    suspend fun getEnabledRulesForProject(
        @LLMDescription("Project ID, e.g. NEW")
        projectId: String
    ): List<WorkflowRule> = coroutineScope {
        val workflows = fetchProjectWorkflows(projectId)
        if (workflows.isEmpty()) return@coroutineScope emptyList()

        val deferred = workflows.map { wf ->
            async {
                runCatching { getEnabledRulesForWorkflow(projectId, wf) }
                    .getOrElse { emptyList() }
            }
        }
        deferred.awaitAll().flatten()
    }
}

