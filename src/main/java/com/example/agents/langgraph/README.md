# LangGraph-Style Multi-Agent GM Vehicle Selection System

This implementation demonstrates a graph-based multi-agent system using LangChain4j, inspired by LangGraph architecture patterns.

## Architecture

### Core Components

1. **CustomerState** - Shared state passed between agents containing:
   - Customer profile and requirements
   - Recommended vehicles
   - Financial information
   - Conversation history
   - Routing information

2. **AgentNode Interface** - Base interface for all specialized agents

3. **GMVehicleGraphAgent** - Main orchestrator that manages agent routing and execution

### Specialized Agents

Each agent is an expert in a specific domain:

#### 🧭 Intent Classifier Agent
- Routes user queries to appropriate expert agents
- Analyzes intent and directs conversation flow
- Ensures users get connected to the right specialist

#### 👥 Customer Profiler Agent
- Understands customer needs, family size, lifestyle
- Builds comprehensive customer profiles
- Suggests suitable vehicle categories
- Tools: `analyzeNeeds`, `buildProfile`, `suggestCategories`

#### 🔧 Technical Expert Agent
- Provides detailed vehicle specifications
- Compares vehicles and features
- Calculates total cost of ownership
- Tools: `searchVehicles`, `compareVehicles`, `checkSafety`, `calculateTCO`

#### 💰 Financial Advisor Agent
- Handles financing calculations
- Compares loan terms and options
- Estimates insurance costs
- Suggests budget allocations
- Tools: `calculateFinancing`, `compareFinancing`, `calculateInsurance`

#### 📍 Availability Coordinator Agent
- Checks dealer inventory
- Schedules test drives
- Manages availability across dealers
- Tools: `checkAvailability`, `scheduleTestDrive`

#### 💡 Negotiation Coach Agent
- Provides pricing strategies
- Calculates trade-in values
- Finds incentives and rebates
- Advises on timing
- Tools: `calculateTradeIn`, `suggestStrategy`, `findIncentives`

#### ⚡ EV Specialist Agent
- Expert on electric vehicles
- Calculates charging costs
- Finds charging infrastructure
- Estimates real-world range
- Tools: `calculateChargingCosts`, `findChargingStations`, `estimateRange`

## Graph Flow

```
User Query
    ↓
Intent Classifier (checks existing context)
    ↓
Routes to appropriate expert(s)
    ↓
Expert reads context from CustomerState
    ↓
Expert processes with specialized tools
    ↓
State updated with findings (for next agents)
    ↓
Response returned to user
```

## Context-Aware Features

### How Agents Share Context

1. **Customer Profiler** stores in state:
   - Customer profile (family size, budget, preferences)
   - Customer requirements (credit score, usage patterns)
   - Suggested vehicle categories

2. **Technical Expert** uses profile to:
   - Search vehicles matching budget and categories
   - Store recommended vehicles in state
   - Avoid asking about already-known preferences

3. **Financial Advisor** uses context to:
   - Apply correct credit score without asking
   - Calculate payments for recommended vehicles
   - Work within stated budget constraints

4. **Other Agents** similarly:
   - Read relevant context before processing
   - Update state with their findings
   - Build upon previous interactions

### Benefits of Context Awareness

- **No Redundant Questions**: Information gathered once is reused
- **Coherent Experience**: Feels like talking to one intelligent assistant
- **Efficient Conversations**: Faster path to solutions
- **Progressive Refinement**: Each agent adds value to the conversation

## Key Features

1. **Non-Linear Conversation Flow**: Users can naturally jump between topics
2. **Specialized Expertise**: Each agent has deep knowledge in their domain
3. **Tool Integration**: 20+ specialized tools across all agents
4. **State Persistence**: Conversation context maintained across agents
5. **Intelligent Routing**: Queries automatically directed to best expert
6. **Context Awareness**: Agents build upon each other's work without redundant questions

## Usage

```bash
# Run the multi-agent system
mvn exec:java -Dexec.args="langgraph"

# Run the context-aware demo
mvn exec:java -Dexec.mainClass="com.example.agents.langgraph.ContextAwareDemo"

# Or with OpenAI API key
export OPENAI_API_KEY=your-api-key
mvn exec:java -Dexec.args="langgraph"
```

## Example Interactions

```
User: I need a family SUV under $40k
→ Routes to Customer Profiler
→ Gathers family details
→ Routes to Technical Expert
→ Shows suitable vehicles

User: What financing options do I have?
→ Routes to Financial Advisor
→ Calculates payment options
→ Suggests best terms

User: Is the Bolt EV good for my 50-mile commute?
→ Routes to EV Specialist
→ Analyzes range and charging
→ Provides recommendations
```

## Benefits of Graph Architecture

1. **Scalability**: Easy to add new expert agents
2. **Modularity**: Each agent is independent
3. **Flexibility**: Dynamic routing based on context
4. **Expertise**: Specialized prompts and tools per domain
5. **Natural Flow**: Mimics real car-buying consultations