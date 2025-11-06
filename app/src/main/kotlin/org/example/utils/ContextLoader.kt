package org.example.utils

object ContextLoader {
    fun readWorkflowContext(): String {
        val resourceName = "workflow_context.md"
        val cl = Thread.currentThread().contextClassLoader

        return cl.getResourceAsStream(resourceName)
            ?.reader(Charsets.UTF_8)
            ?.readText()
            ?: ""
    }
}
