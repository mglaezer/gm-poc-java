2?# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

- **Build the project**: `mvn compile`
- **Run tests**: `mvn test`
- **Run the application**: `mvn exec:java`
- **Clean build**: `mvn clean compile`
- **Package application**: `mvn package`
- **Run a single test**: `mvn test -Dtest=ClassNameTest`

## Project Architecture

This is a Java Maven project for experimenting with LangChain4j, and LangGraph4j

### Project Structure
- `src/main/java/com/example/`: Main application code
- `src/test/java/com/example/`: Test code
- Java 21 is used as the target version

### Environment Setup
- Requires Java 21 or higher
- Maven 3.6+ for building
- OpenAI API key may be needed for LLM integration (set via environment variable `OPENAI_API_KEY`)