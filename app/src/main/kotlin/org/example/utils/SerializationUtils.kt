package org.example.utils

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.example.model.WorkflowRule

object SerializationUtils {
    private val json = Json { prettyPrint = true }

    fun rulesToJson(rules: List<WorkflowRule>): String = 
        json.encodeToString(ListSerializer(WorkflowRule.serializer()), rules)
}