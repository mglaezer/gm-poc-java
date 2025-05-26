package com.example.agents.langgraph;

import com.example.agents.CommonRequirements.*;
import java.util.*;

/**
 * Shared state for the multi-agent graph system.
 * This state is passed between different agent nodes.
 */
public class CustomerState {
    private final Map<String, Object> data;
    
    public CustomerState() {
        this.data = new HashMap<>();
    }
    
    public CustomerState(Map<String, Object> initialData) {
        this.data = new HashMap<>(initialData);
    }
    
    // Customer profile and requirements
    public CustomerProfile getCustomerProfile() {
        return (CustomerProfile) data.get("customerProfile");
    }
    
    public void setCustomerProfile(CustomerProfile profile) {
        data.put("customerProfile", profile);
    }
    
    public CustomerRequirements getCustomerRequirements() {
        return (CustomerRequirements) data.get("customerRequirements");
    }
    
    public void setCustomerRequirements(CustomerRequirements requirements) {
        data.put("customerRequirements", requirements);
    }
    
    // Vehicle recommendations
    @SuppressWarnings("unchecked")
    public List<VehicleInfo> getRecommendedVehicles() {
        return (List<VehicleInfo>) data.getOrDefault("recommendedVehicles", new ArrayList<>());
    }
    
    public void setRecommendedVehicles(List<VehicleInfo> vehicles) {
        data.put("recommendedVehicles", vehicles);
    }
    
    // Selected vehicle
    public VehicleInfo getSelectedVehicle() {
        return (VehicleInfo) data.get("selectedVehicle");
    }
    
    public void setSelectedVehicle(VehicleInfo vehicle) {
        data.put("selectedVehicle", vehicle);
    }
    
    // Financial information
    public FinancingOption getSelectedFinancing() {
        return (FinancingOption) data.get("selectedFinancing");
    }
    
    public void setSelectedFinancing(FinancingOption financing) {
        data.put("selectedFinancing", financing);
    }
    
    @SuppressWarnings("unchecked")
    public List<FinancingOption> getFinancingOptions() {
        return (List<FinancingOption>) data.getOrDefault("financingOptions", new ArrayList<>());
    }
    
    public void setFinancingOptions(List<FinancingOption> options) {
        data.put("financingOptions", options);
    }
    
    // Conversation history
    @SuppressWarnings("unchecked")
    public List<String> getConversationHistory() {
        return (List<String>) data.getOrDefault("conversationHistory", new ArrayList<>());
    }
    
    public void addToConversationHistory(String message) {
        List<String> history = getConversationHistory();
        history.add(message);
        data.put("conversationHistory", history);
    }
    
    // Current query/intent
    public String getCurrentQuery() {
        return (String) data.get("currentQuery");
    }
    
    public void setCurrentQuery(String query) {
        data.put("currentQuery", query);
    }
    
    // Next agent to route to
    public String getNextAgent() {
        return (String) data.get("nextAgent");
    }
    
    public void setNextAgent(String agent) {
        data.put("nextAgent", agent);
    }
    
    // Routing reason
    public String getRoutingReason() {
        return (String) data.get("routingReason");
    }
    
    public void setRoutingReason(String reason) {
        data.put("routingReason", reason);
    }
    
    // Generic getter/setter
    public Object get(String key) {
        return data.get(key);
    }
    
    public void set(String key, Object value) {
        data.put(key, value);
    }
    
    // Add vehicle to selected list (for comparisons)
    @SuppressWarnings("unchecked")
    public void addSelectedVehicleForComparison(VehicleInfo vehicle) {
        List<VehicleInfo> selectedVehicles = (List<VehicleInfo>) data.getOrDefault("selectedVehiclesForComparison", new ArrayList<>());
        if (!selectedVehicles.contains(vehicle)) {
            selectedVehicles.add(vehicle);
        }
        data.put("selectedVehiclesForComparison", selectedVehicles);
    }
    
    @SuppressWarnings("unchecked")
    public List<VehicleInfo> getSelectedVehiclesForComparison() {
        return (List<VehicleInfo>) data.getOrDefault("selectedVehiclesForComparison", new ArrayList<>());
    }
    
    // Copy state
    public CustomerState copy() {
        return new CustomerState(new HashMap<>(data));
    }
}