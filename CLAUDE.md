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

This is a Java Maven project for experimenting with LangChain4j, a Java implementation of LangChain for building LLM-powered applications.

### Key Dependencies
- **LangChain4j Core**: Main framework for building LLM applications
- **LangChain4j OpenAI**: OpenAI integration for chat models
- **LangChain4j Embeddings**: Vector embeddings support
- **LangChain4j Document Loaders**: File system document loading
- **LangChain4j Easy RAG**: Simplified RAG implementation

### Project Structure
- `src/main/java/com/example/`: Main application code
- `src/test/java/com/example/`: Test code
- Java 21 is used as the target version

### Environment Setup
- Requires Java 21 or higher
- Maven 3.6+ for building
- OpenAI API key may be needed for LLM integration (set via environment variable `OPENAI_API_KEY`)