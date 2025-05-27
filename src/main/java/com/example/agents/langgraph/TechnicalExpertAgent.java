package com.example.agents.langgraph;

import com.example.agents.CommonRequirements.*;
import com.example.agents.MockVehicleData;
import com.example.agents.ToolsImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Technical Expert Agent - Provides detailed vehicle information and comparisons
 */
public class TechnicalExpertAgent implements AgentNode {
    
    static class TechnicalTools extends BaseToolLogger {
        private final ToolsImpl tools = new ToolsImpl();
        private CustomerState state;
        
        public void setState(CustomerState state) {
            this.state = state;
        }

        @Tool("Search vehicles by criteria")
        public List<VehicleInfo> searchVehicles(
                @P("Category like Truck, SUV, Sedan or null for all") String category,
                @P("Min price") Double minPrice,
                @P("Max price") Double maxPrice,
                @P("Min MPG") Integer minMpg,
                @P("Fuel type") String fuelType) {
            String priceRange = (minPrice != null ? "$" + minPrice : "$0") + "-" + (maxPrice != null ? "$" + maxPrice : "unlimited");
            logToolCall("searchVehicles", "category", category, "priceRange", priceRange);
            VehicleCategory cat = null;
            if (category != null && !category.equalsIgnoreCase("All") && !category.equalsIgnoreCase("Any")) {
                cat = VehicleCategory.fromString(category);
            }
            SearchCriteria criteria = new SearchCriteria(cat, minPrice, maxPrice, minMpg, fuelType, null);
            List<VehicleInfo> results = tools.searchVehicleInventory(criteria);
            
            // Log results to conversation history
            if (state != null) {
                String params = String.format("category=%s, priceRange=%s", category, priceRange);
                String result = results.isEmpty() ? "No vehicles found" : 
                    String.format("Found %d vehicles: %s", results.size(), 
                        results.stream().limit(3)
                            .map(v -> v.make().getDisplayName() + " " + v.model())
                            .collect(Collectors.joining(", ")) + (results.size() > 3 ? "..." : ""));
                state.logToolCall("TECHNICAL_EXPERT", "searchVehicles", params, result);
            }
            return results;
        }
        
        @Tool("Search vehicles by make/brand")
        public List<VehicleInfo> searchVehiclesByMake(
                @P("Make like Chevrolet, GMC, Cadillac") String make,
                @P("Exclude EVs") boolean excludeEVs) {
            logToolCall("searchVehiclesByMake", "make", make != null ? make : "null", "excludeEVs", excludeEVs);
            if (make == null) {
                return new ArrayList<>();
            }
            VehicleMake vehicleMake = VehicleMake.fromString(make);
            if (vehicleMake == null) {
                return new ArrayList<>();
            }
            
            List<VehicleInfo> results = MockVehicleData.VEHICLES.stream()
                .filter(v -> v.make() == vehicleMake)
                .filter(v -> !excludeEVs || !v.fuelType().equalsIgnoreCase("Electric"))
                .collect(Collectors.toList());
                
            // Log results to conversation history
            if (state != null) {
                String params = String.format("make=%s, excludeEVs=%s", make, excludeEVs);
                String result = results.isEmpty() ? "No vehicles found" : 
                    String.format("Found %d %s vehicles%s: %s", results.size(), make, 
                        excludeEVs ? " (non-EV)" : "",
                        results.stream().limit(3)
                            .map(v -> v.model())
                            .collect(Collectors.joining(", ")) + (results.size() > 3 ? "..." : ""));
                state.logToolCall("TECHNICAL_EXPERT", "searchVehiclesByMake", params, result);
            }
            return results;
        }
        
        @Tool("Get detailed vehicle information by ID")
        public VehicleInfo getVehicleDetails(@P("Vehicle ID") String vehicleId) {
            return tools.getVehicleDetails(vehicleId);
        }
        
        @Tool("Search vehicle by make and model")
        public VehicleInfo searchByMakeModel(
                @P("Make (Chevrolet, GMC, Cadillac)") String make,
                @P("Model name") String model) {
            logToolCall("searchByMakeModel", "make", make, "model", model);
            return tools.getVehicleByMakeAndModel(make, model);
        }
        
        @Tool("Compare multiple vehicles")
        public VehicleComparison compareVehicles(@P("List of vehicle IDs") List<String> vehicleIds) {
            logToolCall("compareVehicles", "vehicleIds", vehicleIds);
            VehicleComparison comparison = tools.compareVehicles(vehicleIds);
            
            // Log comparison to conversation history
            if (state != null && comparison != null) {
                String vehicleNames = comparison.vehicles().stream()
                    .map(v -> v.make().getDisplayName() + " " + v.model())
                    .collect(Collectors.joining(" vs "));
                state.logToolCall("TECHNICAL_EXPERT", "compareVehicles", 
                    vehicleIds.toString(), "Compared: " + vehicleNames);
            }
            return comparison;
        }
        
        @Tool("Compare vehicle to competitors")
        public VehicleComparison compareToCompetitors(@P("Vehicle ID") String vehicleId) {
            return tools.compareToCompetitors(vehicleId);
        }
        
        @Tool("Calculate total cost of ownership")
        public TotalCostOfOwnership calculateTCO(
                @P("Vehicle ID") String vehicleId,
                @P("Years of ownership") int years) {
            return tools.calculateTotalCostOfOwnership(vehicleId, years);
        }
        
        @Tool("Check safety ratings")
        public SafetyRatings checkSafety(@P("Vehicle ID") String vehicleId) {
            return tools.checkSafetyRatings(vehicleId);
        }
    }
    
    interface TechnicalAssistant {
        @SystemMessage("""
            You are a technical expert on GM vehicles.
            You provide detailed information about vehicle specifications, performance, and features.
            
            IMPORTANT RULES:
            1. Analyze the ENTIRE conversation history to understand user preferences and context
            2. Extract preferences from previous messages (budget, size, features mentioned)
            3. For requests like "show me cars/SUVs/trucks", use searchVehicles tool
            4. For brand-specific requests (e.g., "Chevy", "GMC"), use searchVehiclesByMake tool
            5. For "non-EV" requests, use searchVehiclesByMake with excludeEVs=true
            6. NEVER ask profiling questions - extract needs from conversation
            7. For comparison requests, do ONE comparison only
            8. If showing 4+ vehicles, suggest they can ask for specific comparisons
            9. When user says "show me [vehicle type]", immediately search and display matching vehicles
            
            CONTEXT EXTRACTION:
            - Look for budget mentions: "under 50k", "around $40,000", "affordable"
            - Look for size/type preferences: "family", "compact", "7-seater"
            - Look for feature needs: "towing", "fuel efficient", "AWD"
            - Use these to set appropriate search parameters
            
            You can:
            - Show popular GM vehicles if no specific request
            - Search for vehicles by any criteria
            - Provide detailed specs and features
            - Compare multiple vehicles
            - Explain technical differences
            - Discuss safety ratings
            - Calculate total cost of ownership
            
            Use the tools to get accurate information.
            If no profile exists, default to showing a variety of popular options.
            Be helpful and informative without being pushy about profiling.
            """)
        String provideTechnicalInfo(@UserMessage String conversation);
    }
    
    private final TechnicalAssistant assistant;
    private final TechnicalTools tools;
    
    public TechnicalExpertAgent(ChatModel model) {
        this.tools = new TechnicalTools();
        this.assistant = AiServices.builder(TechnicalAssistant.class)
                .chatModel(model)
                .tools(tools)
                .build();
    }
    
    @Override
    public CustomerState process(CustomerState state) {
        // Pass state to tools so they can update it
        tools.setState(state);
        
        String query = state.getCurrentQuery();
        
        // Include full conversation history (up to 30 messages maintained by CustomerState)
        var history = state.getConversationHistory();
        String conversation = "";
        if (!history.isEmpty()) {
            conversation = String.join("\n", history);
        }
        
        // The conversation history contains all context needed
        conversation += "\nUser: " + query;
        
        // Let the LLM process the request with tools
        String response = assistant.provideTechnicalInfo(conversation);
        
        // Log the agent's response
        state.logAgentAction("TECHNICAL_EXPERT", "Response", response);
        state.addToConversationHistory("User: " + query);
        state.addToConversationHistory("Technical Expert: " + response);
        
        return state;
    }
    
    @Override
    public String getName() {
        return "TECHNICAL_EXPERT";
    }
}