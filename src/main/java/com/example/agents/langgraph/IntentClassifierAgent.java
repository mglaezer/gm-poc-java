package com.example.agents.langgraph;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Intent Classifier Agent - Routes user queries to appropriate expert agents
 */
public class IntentClassifierAgent implements AgentNode {
    
    interface IntentClassifier {
        @SystemMessage("""
            You are an intent classifier for a GM vehicle selection system.
            Analyze the user's query and the current conversation state to determine which expert agent should handle it.
            
            CRITICAL ROUTING RULES:
            1. AVOID CUSTOMER_PROFILER unless absolutely necessary:
               - Only route there if user explicitly asks to set preferences/budget
               - Or if they say something like "I need help choosing" with NO other context
            2. Default to TECHNICAL_EXPERT for general vehicle questions
            3. If user mentions any specific vehicle, feature, or comparison -> TECHNICAL_EXPERT
            4. If profile exists, NEVER route to CUSTOMER_PROFILER unless user explicitly asks to change it
            5. For vague requests like "help me find a car", prefer TECHNICAL_EXPERT to show options
            
            Available agents:
            - CUSTOMER_PROFILER: ONLY for explicit preference setting requests
            - TECHNICAL_EXPERT: For ALL vehicle questions, specs, features, recommendations
            - FINANCIAL_ADVISOR: For financing, payments, insurance, budgeting
            - AVAILABILITY_COORDINATOR: For inventory, test drives, dealer locations
            - NEGOTIATION_COACH: For pricing, deals, trade-ins, incentives
            - EV_SPECIALIST: For electric vehicle specific questions
            
            Return your response in this exact format:
            AGENT: [agent name]
            REASON: [brief explanation why this agent was chosen, max 15 words]
            
            Example:
            AGENT: TECHNICAL_EXPERT
            REASON: User asking about vehicles, showing popular options
            """)
        String classifyIntent(@UserMessage String context);
    }
    
    private final IntentClassifier classifier;
    
    public IntentClassifierAgent(ChatModel model) {
        this.classifier = AiServices.builder(IntentClassifier.class)
                .chatModel(model)
                .build();
    }
    
    @Override
    public CustomerState process(CustomerState state) {
        String query = state.getCurrentQuery();
        if (query == null || query.isEmpty()) {
            // Always default to technical expert to show options
            state.setNextAgent("TECHNICAL_EXPERT");
            state.setRoutingReason("Showing popular vehicle options");
            return state;
        }
        
        // Build context for the classifier
        StringBuilder context = new StringBuilder();
        context.append("User Query: ").append(query);
        
        // Add current state information
        if (state.getCustomerProfile() != null) {
            context.append("\nCustomer Profile: EXISTS (Family size: ")
                   .append(state.getCustomerProfile().familySize())
                   .append(", Budget: $")
                   .append(state.getCustomerProfile().budgetMin())
                   .append("-$")
                   .append(state.getCustomerProfile().budgetMax())
                   .append(")");
        } else {
            context.append("\nCustomer Profile: NOT YET CREATED");
        }
        
        if (!state.getRecommendedVehicles().isEmpty()) {
            context.append("\nRecommended Vehicles: ")
                   .append(state.getRecommendedVehicles().size())
                   .append(" vehicles already recommended");
        }
        
        if (state.getSelectedVehicle() != null) {
            context.append("\nSelected Vehicle: ")
                   .append(state.getSelectedVehicle().make().getDisplayName())
                   .append(" ")
                   .append(state.getSelectedVehicle().model());
        }
        
        if (state.getFinancingOptions() != null && !state.getFinancingOptions().isEmpty()) {
            context.append("\nFinancing: Options already calculated");
        }
        
        String response = classifier.classifyIntent(context.toString());
        
        // Parse the response
        String agent = "CUSTOMER_PROFILER"; // default
        String reason = "Unable to classify intent";
        
        String[] lines = response.split("\n");
        for (String line : lines) {
            if (line.startsWith("AGENT:")) {
                agent = line.substring(6).trim().toUpperCase();
            } else if (line.startsWith("REASON:")) {
                reason = line.substring(7).trim();
            }
        }
        
        state.setNextAgent(agent);
        state.setRoutingReason(reason);
        state.addToConversationHistory("Router: Directing to " + agent + " - " + reason);
        
        return state;
    }
    
    @Override
    public String getName() {
        return "INTENT_CLASSIFIER";
    }
}