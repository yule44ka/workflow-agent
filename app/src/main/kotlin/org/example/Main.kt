package org.example
import org.example.agent

suspend fun main() {
    println("YouTrack Workflow Assistant")
    println("Type 'exit' to quit at any time.\n")
    
    print("Enter your problem: ")
    val userInput = readln().trim()
    if (userInput.equals("exit", true)) return
    if (userInput.isBlank()) {
        println("Input cannot be empty. Please describe your problem.")
        return
    }

    // Run the agent - it will handle the conversation flow internally
    // using AskUser, SayToUser, and ExitTool
    try {
        agent.run(userInput)
    } catch (e: Exception) {
        println("An error occurred: ${e.message}")
        e.printStackTrace()
    }
}