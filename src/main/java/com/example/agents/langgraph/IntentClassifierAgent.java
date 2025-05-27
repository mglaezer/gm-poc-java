package com.example.agents.langgraph;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import com.example.agents.CommonRequirements.*;
import java.util.List;

/**
 * Intent Classifier Agent - Routes user queries to appropriate expert agents
 */
public class IntentClassifierAgent implements AgentNode {
    
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
            
            Return your response in this exact format:
            AGENT: [agent name]
            REASON: [brief explanation why this agent was chosen, max 15 words]
            
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
        
        // Add conversation history for context
        List<String> conversationHistory = state.getConversationHistory();
        if (!conversationHistory.isEmpty()) {
            context.append("\n\nRecent Conversation (last 10 messages):\n");
            int start = Math.max(0, conversationHistory.size() - 10);
            for (int i = start; i < conversationHistory.size(); i++) {
                context.append(conversationHistory.get(i)).append("\n");
            }
        }
        
        // Extract context from conversation history
        List<String> recentVehicles = state.getRecentVehiclesMentioned();
        if (!recentVehicles.isEmpty()) {
            context.append("\nRecent vehicle searches: ").append(recentVehicles.size());
            // Check if many vehicles were found in recent searches
            for (String vehicleEntry : recentVehicles) {
                if (vehicleEntry.contains("Found") && vehicleEntry.contains("vehicles")) {
                    context.append("\n").append(vehicleEntry);
                }
            }
        }
        
        if (state.hasRecentFinancingDiscussion()) {
            context.append("\nFinancing: Recent financing discussion detected");
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
        state.logAgentAction("INTENT_CLASSIFIER", "Routing", 
            "Directing to " + agent + " - " + reason);
        
        return state;
    }
    
    @Override
    public String getName() {
        return "INTENT_CLASSIFIER";
    }
}