package com.example.agents.langgraph;

import com.example.agents.CommonRequirements.*;
import com.example.agents.ToolsImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.util.List;

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
                @P("Category like Truck, SUV, Sedan") String category,
                @P("Min price") Double minPrice,
                @P("Max price") Double maxPrice,
                @P("Min MPG") Integer minMpg,
                @P("Fuel type") String fuelType) {
            logToolCall("searchVehicles", "category", category, "priceRange", "$" + minPrice + "-$" + maxPrice);
            VehicleCategory cat = category != null ? VehicleCategory.fromString(category) : null;
            SearchCriteria criteria = new SearchCriteria(cat, minPrice, maxPrice, minMpg, fuelType, null);
            List<VehicleInfo> results = tools.searchVehicleInventory(criteria);
            if (state != null && !results.isEmpty()) {
                state.setRecommendedVehicles(results);
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
            if (state != null) {
                state.set("lastComparison", comparison);
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
            1. If NO customer profile exists, show popular vehicles across different categories
            2. NEVER ask profiling questions - that's not your job
            3. Extract context clues from the user's question (e.g., "family car" = SUV/Minivan)
            4. Always be ready to provide specific vehicle information immediately
            5. For comparison requests, do ONE comparison only - don't repeat or try multiple combinations
            
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
        
        // Only include recent conversation history to avoid confusion
        var history = state.getConversationHistory();
        String conversation = "";
        if (!history.isEmpty()) {
            // Include only the last few exchanges to avoid confusing the LLM with old recommendations
            int start = Math.max(0, history.size() - 4); // Last 2 exchanges
            conversation = String.join("\n", history.subList(start, history.size()));
        }
        
        // Add customer profile context if available
        CustomerProfile profile = state.getCustomerProfile();
        if (profile != null) {
            conversation += "\n\nCustomer Profile:";
            conversation += "\n- Family size: " + profile.familySize();
            conversation += "\n- Primary usage: " + profile.primaryUsage();
            conversation += "\n- Budget: $" + profile.budgetMin() + "-$" + profile.budgetMax();
            conversation += "\n- Preferred categories: " + profile.preferredCategories();
            conversation += "\n- Fuel preference: " + profile.fuelPreference();
            if (profile.needsTowing()) {
                conversation += "\n- Needs towing capability";
            }
            if (profile.needsOffRoad()) {
                conversation += "\n- Needs off-road capability";
            }
            conversation += "\n- Must-have features: " + profile.preferences();
        }
        
        // Add any previously recommended vehicles
        List<VehicleInfo> previousRecommendations = state.getRecommendedVehicles();
        if (previousRecommendations != null && !previousRecommendations.isEmpty()) {
            conversation += "\n\nPreviously discussed vehicles:";
            for (VehicleInfo vehicle : previousRecommendations) {
                conversation += "\n- " + vehicle.make().getDisplayName() + " " + vehicle.model() + 
                              " ($" + String.format("%,.0f", vehicle.price()) + ")";
            }
        }
        
        conversation += "\nUser: " + query;
        
        String response = assistant.provideTechnicalInfo(conversation);
        state.addToConversationHistory("Technical Expert: " + response);
        
        return state;
    }
    
    @Override
    public String getName() {
        return "TECHNICAL_EXPERT";
    }
}