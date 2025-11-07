package org.example
import org.example.createAgent

suspend fun main() {
    // Use agent
    while (true) {
        print("\nEnter your problem (or type 'exit' to stop): ")
        val userInput = readln().trim()
        if (userInput.equals("exit", true)) break
        if (userInput.isBlank()) {
            println("Input cannot be empty. Please describe your problem.")
            continue
        }

        println("\nProcessing...\n")
        val agent = createAgent()
        var result = agent.run(userInput)
        // Cut off the analysis section
        result = result.replace(Regex("<investigation_analysis>[\\s\\S]*?</investigation_analysis>", RegexOption.IGNORE_CASE), "").trimStart()

        println("Response:\n$result")
    }
}