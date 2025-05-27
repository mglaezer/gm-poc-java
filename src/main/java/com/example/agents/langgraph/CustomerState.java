package com.example.agents.langgraph;

import java.util.*;

/**
 * Simplified CustomerState that uses conversation history as the single source of truth.
 * All agent interactions, tool results, and user responses are stored in the conversation.
 */
public class CustomerState {
    private final List<String> conversationHistory;
    private String currentQuery;
    private String nextAgent;
    private String routingReason;
    
    public CustomerState() {
        this.conversationHistory = new ArrayList<>();
    }
    
    // Conversation history management
    public List<String> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }
    
    public void addToConversationHistory(String entry) {
        conversationHistory.add(entry);
        // Keep only last 30 entries to prevent overflow
        if (conversationHistory.size() > 30) {
            conversationHistory.remove(0);
        }
    }
    
    // Log agent actions with structured format
    public void logAgentAction(String agentName, String action, String details) {
        String entry = String.format("[%s] %s: %s", agentName, action, details);
        addToConversationHistory(entry);
    }
    
    // Log tool calls and results
    public void logToolCall(String agentName, String toolName, String parameters, String result) {
        String entry = String.format("[%s] Tool %s(%s) -> %s", agentName, toolName, parameters, result);
        addToConversationHistory(entry);
    }
    
    // Current query being processed
    public String getCurrentQuery() {
        return currentQuery;
    }
    
    public void setCurrentQuery(String query) {
        this.currentQuery = query;
    }
    
    // Routing information
    public String getNextAgent() {
        return nextAgent;
    }
    
    public void setNextAgent(String nextAgent) {
        this.nextAgent = nextAgent;
    }
    
    public String getRoutingReason() {
        return routingReason;
    }
    
    public void setRoutingReason(String reason) {
        this.routingReason = reason;
    }
    
    // Extract information from conversation history
    public List<String> getRecentVehiclesMentioned() {
        List<String> vehicles = new ArrayList<>();
        for (String entry : conversationHistory) {
            // Look for vehicle search tool calls
            if (entry.contains("Tool searchVehicles") || entry.contains("Tool searchVehiclesByMake")) {
                vehicles.add(entry);
            }
        }
        return vehicles;
    }
    
    public String getLastAgentResponse() {
        for (int i = conversationHistory.size() - 1; i >= 0; i--) {
            String entry = conversationHistory.get(i);
            // Look for agent responses (not tool calls or routing)
            if ((entry.contains("Expert:") || entry.contains("Advisor:") || 
                 entry.contains("Coordinator:") || entry.contains("Coach:") || 
                 entry.contains("Profiler:") || entry.contains("Specialist:")) 
                && !entry.startsWith("[")) {
                return entry;
            }
        }
        return null;
    }
    
    public boolean hasRecentFinancingDiscussion() {
        // Check last 5 entries for financing-related content
        int start = Math.max(0, conversationHistory.size() - 5);
        for (int i = start; i < conversationHistory.size(); i++) {
            String entry = conversationHistory.get(i).toLowerCase();
            if (entry.contains("lease") || entry.contains("finance") || entry.contains("loan") || entry.contains("payment")) {
                return true;
            }
        }
        return false;
    }
}