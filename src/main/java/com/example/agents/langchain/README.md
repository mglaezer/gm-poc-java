# LangChain4j GM Vehicle Agent

This is a pure LangChain4j implementation of the GM Vehicle Selection Agent.

## Features

The agent can:
- Search for vehicles by make and model
- Get detailed information about any vehicle including:
  - Basic specs (price, year, category)
  - Performance data (engine, horsepower, transmission)
  - Fuel economy (MPG city/highway)
  - Dimensions and capacity
  - Safety and technology features
- Compare multiple vehicles
- Check dealer availability
- Calculate financing options

## Usage

To run the agent:

```bash
# With OpenAI API key
export OPENAI_API_KEY=your-api-key
mvn exec:java -Dexec.mainClass="com.example.agents.langchain.GMVehicleAgentDemo"

# Or use the main launcher
mvn exec:java -Dexec.args="langchain"
```

## Example Interactions

```
You: Tell me about the Chevrolet Silverado
Agent: [Provides comprehensive information about the Silverado including specs, features, and pricing]

You: What electric vehicles do you have?
Agent: [Shows information about the Bolt EV]

You: Compare the Equinox and Traverse
Agent: [Provides side-by-side comparison of both vehicles]
```

## Architecture

- **GMVehicleAgent**: Main agent class that orchestrates the conversation
- **VehicleSearchTools**: LangChain4j tools for searching and retrieving vehicle data
- **ToolsImpl**: Implementation of the common tools interface
- **MockVehicleData**: Hardcoded vehicle inventory for demonstration

The agent uses LangChain4j's AiServices to create a conversational interface with tool support.