package com.example.agents.langgraph;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Main GM Vehicle Graph Agent that orchestrates multiple specialized agents
 */
public class GMVehicleGraphAgent {
    
    private final Map<String, AgentNode> agents;
    private final IntentClassifierAgent router;
    private final ChatModel model;
    
    public GMVehicleGraphAgent() {
        this(OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4.1")
                .temperature(0.7)
                .build());
    }
    
    public GMVehicleGraphAgent(ChatModel model) {
        this.model = model;
        this.agents = new HashMap<>();
        
        // Initialize all agents
        this.router = new IntentClassifierAgent(model);
        
        // Register all specialist agents
        registerAgent(new CustomerProfilerAgent(model));
        registerAgent(new TechnicalExpertAgent(model));
        registerAgent(new FinancialAdvisorAgent(model));
        registerAgent(new AvailabilityCoordinatorAgent(model));
        registerAgent(new NegotiationCoachAgent(model));
        registerAgent(new EVSpecialistAgent(model));
    }
    
    private void registerAgent(AgentNode agent) {
        agents.put(agent.getName(), agent);
    }
    
    /**
     * Process a user query through the graph
     */
    public String processQuery(String userQuery, CustomerState state) {
        if (state == null) {
            state = new CustomerState();
        }
        
        state.setCurrentQuery(userQuery);
        state.addToConversationHistory("User: " + userQuery);
        
        // First, route through intent classifier
        System.out.println("\n🔄 Routing: Intent Classifier analyzing query...");
        state = router.process(state);
        
        // Get the next agent to process
        String nextAgentName = state.getNextAgent();
        
        // If we have a valid next agent, process through it
        if (nextAgentName != null && agents.containsKey(nextAgentName)) {
            String reason = state.getRoutingReason();
            if (reason != null && !reason.isEmpty()) {
                System.out.println("➡️  Agent: " + nextAgentName + " (Reason: " + reason + ")");
            } else {
                System.out.println("➡️  Agent: " + nextAgentName);
            }
            AgentNode nextAgent = agents.get(nextAgentName);
            state = nextAgent.process(state);
        }
        
        // Return the last response from conversation history
        var history = state.getConversationHistory();
        if (!history.isEmpty()) {
            return history.get(history.size() - 1);
        }
        
        return "I'm here to help you find the perfect GM vehicle. What are you looking for?";
    }
    
    /**
     * Start a new conversation
     */
    public CustomerState startNewConversation() {
        CustomerState state = new CustomerState();
        state.addToConversationHistory(
            "GM Vehicle Assistant: Hello! I'm here to help you find the perfect GM vehicle. " +
            "You can:\n" +
            "• Ask about specific vehicles (e.g., \"Tell me about the Silverado\")\n" +
            "• Compare models (e.g., \"Compare Tahoe vs Traverse\")\n" +
            "• Get recommendations (e.g., \"Best SUV under $50k\")\n" +
            "• Check financing options\n" +
            "• Schedule test drives\n\n" +
            "What would you like to know?"
        );
        return state;
    }
    
    /**
     * Process a query with automatic routing through multiple agents if needed
     */
    public String processWithAutoRouting(String userQuery, CustomerState state, int maxSteps) {
        if (state == null) {
            state = startNewConversation();
        }
        
        String lastResponse = "";
        
        for (int i = 0; i < maxSteps; i++) {
            state.setCurrentQuery(userQuery);
            
            // Route and process
            if (i == 0) {
                System.out.println("\n🔄 Routing: Intent Classifier analyzing query...");
            }
            state = router.process(state);
            String nextAgentName = state.getNextAgent();
            
            if (nextAgentName == null || !agents.containsKey(nextAgentName)) {
                break;
            }
            
            String reason = state.getRoutingReason();
            if (reason != null && !reason.isEmpty()) {
                System.out.println("➡️  Agent: " + nextAgentName + " (Reason: " + reason + ")");
            } else {
                System.out.println("➡️  Agent: " + nextAgentName);
            }
            AgentNode agent = agents.get(nextAgentName);
            state = agent.process(state);
            
            // Get the last response
            var history = state.getConversationHistory();
            if (!history.isEmpty()) {
                lastResponse = history.get(history.size() - 1);
            }
            
            // Check if we should continue routing
            // In a real implementation, this would be more sophisticated
            if (lastResponse.toLowerCase().contains("would you like") || 
                lastResponse.toLowerCase().contains("anything else")) {
                break;
            }
        }
        
        return lastResponse;
    }
}