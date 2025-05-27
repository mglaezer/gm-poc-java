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
        private CustomerState state;
        
        public void setState(CustomerState state) {
            this.state = state;
        }
        
        @Tool("Calculate charging costs for an EV")
        public ChargingCost calculateChargingCosts(
                @P("Vehicle ID") String vehicleId,
                @P("ZIP code") String zipCode,
                @P("Daily miles driven") double dailyMiles) {
            ChargingCost cost = tools.calculateChargingCosts(vehicleId, zipCode, dailyMiles);
            if (state != null && cost != null) {
                state.logToolCall("EV_SPECIALIST", "calculateChargingCosts", 
                    String.format("vehicleId=%s, zipCode=%s, dailyMiles=%.1f", vehicleId, zipCode, dailyMiles),
                    "Charging cost for " + vehicleId + ": $" + cost.monthlyCost() + "/month");
            }
            return cost;
        }
        
        @Tool("Find charging stations near a location")
        public List<ChargingStation> findChargingStations(
                @P("ZIP code") String zipCode,
                @P("Search radius in miles") double radiusMiles) {
            List<ChargingStation> stations = tools.findChargingStations(zipCode, radiusMiles);
            if (state != null && !stations.isEmpty()) {
                state.logToolCall("EV_SPECIALIST", "findChargingStations", 
                    String.format("zipCode=%s, radiusMiles=%.1f", zipCode, radiusMiles),
                    "Found " + stations.size() + " charging stations within " + radiusMiles + " miles of " + zipCode);
            }
            return stations;
        }
        
        @Tool("Estimate range for a specific trip")
        public RangeEstimate estimateRange(
                @P("Vehicle ID") String vehicleId,
                @P("Trip distance in miles") double tripDistance,
                @P("Weather condition (hot, cold, rain, normal)") String weatherCondition) {
            RangeEstimate estimate = tools.estimateRangeForTrip(vehicleId, tripDistance, weatherCondition);
            if (state != null && estimate != null) {
                state.logToolCall("EV_SPECIALIST", "estimateRange", 
                    String.format("vehicleId=%s, tripDistance=%.1f, weather=%s", vehicleId, tripDistance, weatherCondition),
                    "Range estimate for " + vehicleId + ": " + estimate.adjustedRange() + " miles in " + weatherCondition + " weather");
            }
            return estimate;
        }
    }
    
    interface EVAssistant {
        @SystemMessage("""
            You are an electric vehicle specialist for GM.
            You help customers understand EVs and address their concerns.
            
            IMPORTANT: Extract customer needs and concerns from the conversation history.
            Look for mentions of:
            - Daily driving distances or commute patterns
            - Home charging availability
            - Range concerns or long trips
            - Environmental preferences
            
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
        // Pass state to tools so they can update it
        tools.setState(state);
        
        String query = state.getCurrentQuery();
        String conversation = String.join("\n", state.getConversationHistory());
        conversation += "\nUser: " + query;
        
        String response = assistant.provideEVGuidance(conversation);
        state.addToConversationHistory("EV Specialist: " + response);
        
        return state;
    }
    
    @Override
    public String getName() {
        return "EV_SPECIALIST";
    }
}