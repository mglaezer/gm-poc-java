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
public class TechnicalExpertAgent {
    
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
                        results.stream().limit(3).map(v -> v.make().getDisplayName() + " " + v.model())
                            .collect(Collectors.joining(", ")) + (results.size() > 3 ? "..." : ""));
                state.logToolCall("TECHNICAL_EXPERT", "searchVehicles", params, result);
            }
            return results;
        }
        
        @Tool("Search vehicles by make/brand")
        public List<VehicleInfo> searchVehiclesByMake(
                @P("Make like Chevrolet, GMC, Cadillac, Buick") String make,
                @P("Exclude EVs") boolean excludeEVs) {
            logToolCall("searchVehiclesByMake", "make", make != null ? make : "null", "excludeEVs", excludeEVs);
            if (make == null) {
                return new ArrayList<>();
            }
            VehicleMake vehicleMake = VehicleMake.fromString(make);
            if (vehicleMake == null) {
                return new ArrayList<>();
            }
            List<VehicleInfo> results = tools.searchVehicleInventory(
                new SearchCriteria(null, null, null, null, null, null)
            ).stream()
                .filter(v -> v.make() == vehicleMake)
                .collect(Collectors.toList());
            if (excludeEVs) {
                results = results.stream()
                    .filter(v -> !"Electric".equalsIgnoreCase(v.fuelType()))
                    .collect(Collectors.toList());
            }
            
            // Log results
            if (state != null) {
                String params = String.format("make=%s, excludeEVs=%s", make, excludeEVs);
                String result = results.isEmpty() ? "No vehicles found" : 
                    String.format("Found %d %s vehicles", results.size(), make);
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
                @P("Make (Chevrolet, GMC, Cadillac, Buick)") String make,
                @P("Model name") String model) {
            logToolCall("searchByMakeModel", "make", make, "model", model);
            return tools.getVehicleByMakeAndModel(make, model);
        }
        
        @Tool("Compare multiple vehicles")
        public VehicleComparison compareVehicles(@P("List of vehicle IDs") List<String> vehicleIds) {
            logToolCall("compareVehicles", "vehicleIds", vehicleIds);
            return tools.compareVehicles(vehicleIds);
        }
        
        @Tool("Compare GM vehicle to competitors")
        public String compareToCompetitors(
                @P("GM vehicle ID") String gmVehicleId,
                @P("Competitor vehicles (e.g., 'Toyota Highlander, Honda Pilot')") String competitorList) {
            logToolCall("compareToCompetitors", "gmVehicleId", gmVehicleId, "competitors", competitorList);
            // Simple comparison - just use our compareToCompetitors method
            VehicleComparison comparison = tools.compareToCompetitors(gmVehicleId);
            if (comparison == null) return "Vehicle not found";
            
            StringBuilder result = new StringBuilder();
            result.append("Comparing GM vehicle to competitors:\n\n");
            for (ComparisonPoint point : comparison.comparisonPoints()) {
                result.append(point.category()).append(": ").append(point.value()).append("\n");
            }
            return result.toString();
        }
        
        @Tool("Calculate total cost of ownership")
        public TotalCostOfOwnership calculateTCO(
                @P("Vehicle ID") String vehicleId,
                @P("Annual miles driven") int annualMiles,
                @P("Years of ownership") int years) {
            logToolCall("calculateTCO", "vehicleId", vehicleId, "annualMiles", annualMiles, "years", years);
            return tools.calculateTotalCostOfOwnership(vehicleId, years);
        }
        
        @Tool("Check vehicle safety ratings")
        public SafetyRatings checkSafety(@P("Vehicle ID") String vehicleId) {
            logToolCall("checkSafety", "vehicleId", vehicleId);
            // Mock safety ratings since getSafetyRatings doesn't exist in ToolsImpl
            return new SafetyRatings(
                vehicleId,
                5, // NHTSA overall
                5, // frontal crash
                5, // side crash
                4, // rollover
                List.of("Forward Collision Warning", "Automatic Emergency Braking", 
                       "Blind Spot Monitoring", "Lane Keep Assist"),
                true // IIHS Top Safety Pick
            );
        }
    }
    
    interface TechnicalAssistant {
        @SystemMessage("""
            You are a knowledgeable GM vehicle technical expert and sales assistant.
            You have access to comprehensive vehicle information and can:
            - Search for vehicles by any criteria
            - Provide detailed specs and features
            - Compare multiple vehicles
            - Explain technical differences
            - Discuss safety ratings
            - Calculate total cost of ownership
            
            Use the tools to get accurate information.
            If no preferences exist, default to showing a variety of popular options, but ask first.
            Be helpful and informative.
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
    
    public String execute(CustomerState state, String query) {
        // Pass state to tools so they can update it
        tools.setState(state);
        
        // Include conversation history
        String conversation = state.getConversationContext();
        
        // Let the LLM process the request with tools
        String response = assistant.provideTechnicalInfo(conversation);
        
        // Log the agent response (user message already added by GMVehicleGraphAgent)
        state.addAiMessage(response);
        
        return response;
    }
}