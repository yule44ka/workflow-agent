# YouTrack Workflow Agent

AI agent that helps diagnose and explain YouTrack workflow behavior. The agent analyzes workflow rules and identifies which rules may have caused specific actions in your YouTrack instance.

## Features

- Interactive CLI
- Connection to your YouTrack instance
- Automatic workflow rules retrieving
- Documentation usage as context for a better understanding of workflows
- AI analysis using Claude Sonnet 4
- Links to relevant workflows

## Requirements

- Java 21 or higher
- Anthropic API key
- YouTrack instance with API access

## Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd workflow-agent
   ```

2. **Set environment variables**
   ```bash
   export ANTHROPIC_API_KEY="your-anthropic-api-key"
   export DOMAIN="https://your-youtrack-instance.myjetbrains.com"
   export YOUTRACK_TOKEN="your-youtrack-permanent-token"
   ```

3. **Build the project**
   ```bash
   ./gradlew build
   ```

4. **Run the agent**
   ```bash
   ./gradlew run
   ```

## Usage

Once running, enter your workflow-related questions.

The agent will analyze your YouTrack workflows and provide an explanation with links to the relevant workflow rules.

Type `exit` to quit the application.

## Tech Stack

- **Koog** - AI agent framework
- **Anthropic Claude** - LLM for analysis
- **Ktor** - HTTP client for YouTrack API
- **Gradle** - Build system
