package com.example.agents.multiple;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Intent Classifier Agent - Routes user queries to appropriate expert agents
 */
public class IntentClassifierAgent {
    
    public record IntentClassification(
        String agent,
        String reason
    ) {}
    
    interface IntentClassifier {
        @SystemMessage("""
            You are an intent classifier for a GM vehicle selection system.
            Analyze the user's last message AND the entire conversation history to determine which expert agent should handle it.
            The conversation history contains all context about the user's preferences, budget, and needs.
            Make sure you give more preference to the last message and the end of the conversation rather than the beginning.
            
            1. Route to FINANCIAL_ADVISOR when:
               - User mentions is interested in financing, leasing, or buying options.
            
            2. Route to NEGOTIATION_COACH when:
               - User mentions "trade-in", "negotiate", "deal", "incentive", "rebate" or similar
               - User asks about best time to buy or pricing strategies
            
            3. Route to EV_SPECIALIST when:
               - User specifically asks about particular electric vehicles, charging, or range
            
            4. Route to AVAILABILITY_COORDINATOR when:
               - User wants to check inventory, availability, or dealer stock
               - User wants to schedule a test drive
            
            5. Route to CUSTOMER_PROFILER ONLY when:
               - User explicitly asks for help choosing or seems overwhelmed with technical choices.
               - NEVER route to this agent if the user expressed what they want and insists on answers.
            
            6. Route to TECHNICAL_EXPERT when:
               - User asks to see/show/display vehicles
               - User mentions specific vehicles
               - User asks about specs, features, performance, safety ratings, prices
               - User wants to compare vehicles
            
            Available agents and their capabilities:
            
            TECHNICAL_EXPERT - Vehicle Information & Recommendations
            Tools: searchVehicles, searchVehiclesByMake, getVehicleDetails, searchByMakeModel,
                   compareVehicles, compareToCompetitors, calculateTCO, checkSafety
            Route here for: showing vehicles, comparing models, vehicle specs, features,
                           performance data, safety ratings, total cost of ownership
            
            FINANCIAL_ADVISOR - Financing & Budget Management
            Tools: calculateFinancing, compareFinancing, calculateInsurance, suggestBudget
            Route here for: lease/buy/finance decisions, monthly payments, loan terms,
                           insurance costs, budget planning, affordability analysis
            
            CUSTOMER_PROFILER - Narrowing Down Options
            Tools: analyzeNeeds, buildProfile, suggestCategories, filterVehicles, createQuickProfile
            Route here ONLY for: when user is overwhelmed with choices, explicitly asks for help deciding,
                                wants to narrow down from many shown vehicles
            
            AVAILABILITY_COORDINATOR - Inventory & Test Drives
            Tools: checkAvailability, scheduleTestDrive
            Route here for: checking dealer inventory, finding available vehicles,
                           scheduling test drives, dealer locations
            
            NEGOTIATION_COACH - Pricing Strategy & Trade-ins
            Tools: calculateTradeIn, suggestStrategy, findIncentives
            Route here for: trade-in values, negotiation tips, best time to buy,
                           available rebates, dealer incentives, pricing strategies
            
            EV_SPECIALIST - Electric Vehicle Expertise
            Tools: calculateChargingCosts, findChargingStations, estimateRange
            Route here for: EV-specific questions, charging costs, charging station locations,
                           range anxiety, electric vs gas comparisons
            
            Return your response as a JSON object with this exact format:
            {
                "agent": "AGENT_NAME",
                "reason": "Brief explanation why this agent was chosen (max 15 words)"
            }
            
            Example responses:
            {"agent": "TECHNICAL_EXPERT", "reason": "User wants to see SUV options and compare features"}
            {"agent": "FINANCIAL_ADVISOR", "reason": "User asking about monthly payments and financing options"}
            {"agent": "EV_SPECIALIST", "reason": "User inquiring about electric vehicle charging infrastructure"}
            
            """)
        @UserMessage("{{context}}")
        IntentClassification classifyIntent(@V("context") String context);
    }
    
    private final IntentClassifier classifier;
    
    public IntentClassifierAgent(ChatModel model) {
        this.classifier = AiServices.builder(IntentClassifier.class)
                .chatModel(model)
                .build();
    }
    
    public String classifyIntent(CustomerState state) {
        var lastUserMessage = state.getLastUserMessage();
            
        if (lastUserMessage == null) {
            state.logAgentAction("INTENT_CLASSIFIER", "Routing", 
                "No user query found, defaulting to TECHNICAL_EXPERT");
            return "TECHNICAL_EXPERT";
        }
        
        // Build context for the classifier
        StringBuilder context = new StringBuilder();
        context.append("User Query: ").append(lastUserMessage.singleText());
        
        // Add conversation history for context
        String conversationContext = state.getConversationContext();
        if (!conversationContext.isEmpty()) {
            context.append("\n\nRecent Conversation:\n");
            context.append(conversationContext);
        }
        
        try {
            IntentClassification result = classifier.classifyIntent(context.toString());
            
            // Log the routing decision
            state.logAgentAction("INTENT_CLASSIFIER", "Routing", 
                "Directing to " + result.agent() + " - " + result.reason());
            
            return result.agent();
            
        } catch (Exception e) {
            // Fallback to manual parsing if structured output fails
            System.err.println("Error with structured output: " + e.getMessage());
            state.logAgentAction("INTENT_CLASSIFIER", "Routing", 
                "Error in classification, defaulting to TECHNICAL_EXPERT");
            return "TECHNICAL_EXPERT";
        }
    }
    
    public IntentClassification classifyIntentWithReason(CustomerState state) {
        var lastUserMessage = state.getLastUserMessage();
            
        if (lastUserMessage == null) {
            return new IntentClassification("TECHNICAL_EXPERT", "No user query found");
        }
        
        // Build context for the classifier
        StringBuilder context = new StringBuilder();
        context.append("User Query: ").append(lastUserMessage.singleText());
        
        // Add conversation history for context
        String conversationContext = state.getConversationContext();
        if (!conversationContext.isEmpty()) {
            context.append("\n\nRecent Conversation:\n");
            context.append(conversationContext);
        }
        
        try {
            IntentClassification result = classifier.classifyIntent(context.toString());
            
            // Log the routing decision
            state.logAgentAction("INTENT_CLASSIFIER", "Routing", 
                "Directing to " + result.agent() + " - " + result.reason());
            
            return result;
            
        } catch (Exception e) {
            System.err.println("Error with structured output: " + e.getMessage());
            return new IntentClassification("TECHNICAL_EXPERT", "Classification error occurred");
        }
    }
}