package org.example.model

import kotlinx.serialization.Serializable

@Serializable
data class WorkflowRule(
    val ruleId: String,
    val ruleName: String,
    val ruleDescription: String? = null,
    val ruleScript: String? = null,
    val workflowId: String,
    val workflowName: String,
)

@Serializable
data class Project(
    val id: String,
    val name: String,
)

@Serializable
data class Workflow(
    val id: String,
    val name: String,
)

@Serializable
data class WorkflowUsageResponse(
    val workflow: Workflow? = null,
)

@Serializable
data class AppDetails(
    val pluggableObjects: List<PluggableObject> = emptyList()
)

@Serializable
data class PluggableObject(
    val id: String,
    val name: String,
    val description: String? = null,
    val script: Script? = null,
    val usages: List<Usage> = emptyList(),
)

@Serializable
data class Script(
    val id: String,
    val script: String? = null,
)

@Serializable
data class Usage(
    val enabled: Boolean = false,
    val configuration: Configuration? = null,
)

@Serializable
data class Configuration(
    val project: ProjectShort? = null,
)

@Serializable
data class ProjectShort(
    val shortName: String,
)
