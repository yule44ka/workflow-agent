package org.example
import org.example.agent

suspend fun main() {
    // Use agent
    while (true) {
        print("\nEnter your problem (or type 'exit'): ")
        val userInput = readln().trim()
        if (userInput.equals("exit", true)) break

        if (userInput.isBlank()) {
            println("Input cannot be empty. Please describe your problem.")
            continue
        }

        println("\nProcessing...\n")
        val result = agent.run(userInput)

        println("Response:\n$result")
    }
}