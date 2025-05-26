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
            
            IMPORTANT: Consider what information has already been gathered:
            - If customer profile exists, don't route to CUSTOMER_PROFILER unless they want to update it
            - If vehicles have been recommended, consider routing to FINANCIAL_ADVISOR or AVAILABILITY_COORDINATOR
            - Route based on the user's current need in the conversation flow
            
            Available agents:
            - CUSTOMER_PROFILER: For understanding customer needs, family size, lifestyle, preferences
            - TECHNICAL_EXPERT: For vehicle specs, performance, comparisons, safety ratings
            - FINANCIAL_ADVISOR: For financing, budgeting, insurance, total cost of ownership
            - AVAILABILITY_COORDINATOR: For checking inventory, scheduling test drives
            - NEGOTIATION_COACH: For pricing strategy, trade-ins, incentives
            - EV_SPECIALIST: For electric vehicle questions, charging, range
            
            Return your response in this exact format:
            AGENT: [agent name]
            REASON: [brief explanation why this agent was chosen, max 15 words]
            
            Example:
            AGENT: TECHNICAL_EXPERT
            REASON: User asking about vehicle performance and specifications
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
            // If no profile exists, start with customer profiler
            if (state.getCustomerProfile() == null) {
                state.setNextAgent("CUSTOMER_PROFILER");
                state.setRoutingReason("No customer profile found, starting with profiling");
            } else {
                // If we have a profile, go to technical expert
                state.setNextAgent("TECHNICAL_EXPERT");
                state.setRoutingReason("Customer profile exists, showing vehicle options");
            }
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