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
 * EV Specialist Agent - Expert on electric vehicles and charging
 */
public class EVSpecialistAgent implements AgentNode {
    
    static class EVTools extends BaseToolLogger {
        private final ToolsImpl tools = new ToolsImpl();
        
        @Tool("Calculate charging costs for an EV")
        public ChargingCost calculateChargingCosts(
                @P("Vehicle ID") String vehicleId,
                @P("ZIP code") String zipCode,
                @P("Daily miles driven") double dailyMiles) {
            return tools.calculateChargingCosts(vehicleId, zipCode, dailyMiles);
        }
        
        @Tool("Find charging stations near a location")
        public List<ChargingStation> findChargingStations(
                @P("ZIP code") String zipCode,
                @P("Search radius in miles") double radiusMiles) {
            return tools.findChargingStations(zipCode, radiusMiles);
        }
        
        @Tool("Estimate range for a specific trip")
        public RangeEstimate estimateRange(
                @P("Vehicle ID") String vehicleId,
                @P("Trip distance in miles") double tripDistance,
                @P("Weather condition (hot, cold, rain, normal)") String weatherCondition) {
            return tools.estimateRangeForTrip(vehicleId, tripDistance, weatherCondition);
        }
    }
    
    interface EVAssistant {
        @SystemMessage("""
            You are an electric vehicle specialist for GM.
            You help customers understand EVs and address their concerns.
            
            You can:
            - Explain EV technology and benefits
            - Calculate charging costs and savings
            - Find charging infrastructure
            - Estimate real-world range
            - Address range anxiety
            - Compare EVs to gas vehicles
            - Explain home charging setup
            
            Use the tools to provide accurate information.
            Be enthusiastic about EV technology while being honest about limitations.
            Help customers determine if an EV fits their lifestyle.
            """)
        String provideEVGuidance(@UserMessage String conversation);
    }
    
    private final EVAssistant assistant;
    private final EVTools tools;
    
    public EVSpecialistAgent(ChatModel model) {
        this.tools = new EVTools();
        this.assistant = AiServices.builder(EVAssistant.class)
                .chatModel(model)
                .tools(tools)
                .build();
    }
    
    @Override
    public CustomerState process(CustomerState state) {
        String query = state.getCurrentQuery();
        String conversation = String.join("\n", state.getConversationHistory());
        conversation += "\nUser: " + query;
        
        // Add context about any EV vehicles being considered
        List<VehicleInfo> recommendedVehicles = state.getRecommendedVehicles();
        for (VehicleInfo vehicle : recommendedVehicles) {
            if ("Electric".equals(vehicle.fuelType()) || vehicle.range() != null) {
                conversation += "\nConsidering EV: " + vehicle.make().getDisplayName() + 
                              " " + vehicle.model() + " - Range: " + vehicle.range() + " miles";
            }
        }
        
        String response = assistant.provideEVGuidance(conversation);
        state.addToConversationHistory("EV Specialist: " + response);
        
        return state;
    }
    
    @Override
    public String getName() {
        return "EV_SPECIALIST";
    }
}