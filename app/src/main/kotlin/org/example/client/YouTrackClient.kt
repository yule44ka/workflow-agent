package org.example.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.model.*

class YouTrackClient(
    private val domain: String,
    private val token: String,
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private fun baseUrl(path: String): String = "https://$domain$path"

    suspend fun validateProject(projectId: String): Project? {
        val url = baseUrl("/api/admin/projects/$projectId") + "?fields=id,name"
        val response = client.get(url) {
            header("Authorization", "Bearer $token")
        }
        return if (response.status.isSuccess()) response.body() else null
    }

    suspend fun getProjectWorkflows(projectId: String): List<Workflow> {
        val url = baseUrl("/api/admin/projects/$projectId/workflows") + "?fields=workflow(id,name)"
        val response = client.get(url) {
            header("Authorization", "Bearer $token")
        }
        if (!response.status.isSuccess()) return emptyList()
        val usages: List<WorkflowUsageResponse> = response.body()
        return usages.mapNotNull { it.workflow }
    }

    suspend fun getEnabledRulesForWorkflow(projectId: String, workflow: Workflow): List<WorkflowRule> {
        val url = baseUrl("/api/admin/apps/${workflow.id}") +
                "?fields=pluggableObjects(id,name,description,script(id,script),usages(enabled,configuration(project(shortName))))"
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
}