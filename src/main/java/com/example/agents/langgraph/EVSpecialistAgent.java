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
public class EVSpecialistAgent {
    
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
                @P("Miles per year") int milesPerYear,
                @P("Home charging percentage (0-100)") int homeChargingPercentage) {
            logToolCall("calculateChargingCosts", "vehicleId", vehicleId, "zipCode", zipCode, 
                       "milesPerYear", milesPerYear, "homeCharging%", homeChargingPercentage);
            double dailyMiles = milesPerYear / 365.0;
            ChargingCost cost = tools.calculateChargingCosts(vehicleId, zipCode, dailyMiles);
            if (state != null && cost != null) {
                state.logToolCall("EV_SPECIALIST", "calculateChargingCosts",
                    String.format("vehicleId=%s, zipCode=%s, miles=%d", vehicleId, zipCode, milesPerYear),
                    String.format("Monthly cost: $%.2f", cost.monthlyCost()));
            }
            return cost;
        }
        
        @Tool("Find charging stations near a location")
        public List<ChargingStation> findChargingStations(
                @P("ZIP code or city") String location,
                @P("Radius in miles") int radiusMiles,
                @P("Charging type (Level2, DC_Fast, All)") String chargingType) {
            logToolCall("findChargingStations", "location", location, "radius", radiusMiles, "type", chargingType);
            List<ChargingStation> stations = tools.findChargingStations(location, radiusMiles);
            if (state != null) {
                state.logToolCall("EV_SPECIALIST", "findChargingStations",
                    String.format("location=%s, radius=%d miles", location, radiusMiles),
                    String.format("Found %d charging stations", stations.size()));
            }
            return stations;
        }
        
        @Tool("Estimate real-world range for an EV")
        public RangeEstimate estimateRange(
                @P("Vehicle ID") String vehicleId,
                @P("Temperature (F)") int temperature,
                @P("Highway percentage (0-100)") int highwayPercentage,
                @P("Use AC/Heat") boolean useClimate) {
            logToolCall("estimateRange", "vehicleId", vehicleId, "temp", temperature, 
                       "highway%", highwayPercentage, "climate", useClimate);
            // Convert to weather condition string
            String weatherCondition = temperature < 32 ? "cold" : temperature > 90 ? "hot" : "moderate";
            double tripDistance = 200; // Default trip distance for estimation
            RangeEstimate range = tools.estimateRangeForTrip(vehicleId, tripDistance, weatherCondition);
            if (state != null && range != null) {
                state.logToolCall("EV_SPECIALIST", "estimateRange",
                    String.format("vehicleId=%s, temp=%dF, highway=%d%%", vehicleId, temperature, highwayPercentage),
                    String.format("Estimated range: %.0f miles", range.adjustedRange()));
            }
            return range;
        }
    }
    
    interface EVAssistant {
        @SystemMessage("""
            You are an EV specialist for GM electric vehicles.
            Your expertise includes:
            - Electric vehicle technology and benefits
            - Charging infrastructure and costs
            - Range estimation and factors affecting range
            - EV ownership experience
            - Addressing common EV concerns (range anxiety, charging time, etc.)
            
            Always:
            - Be enthusiastic about EV technology
            - Address concerns honestly and factually
            - Provide practical advice for EV ownership
            - Compare EV costs to gas vehicles when relevant
            - Emphasize GM's EV advantages
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
    
    public String execute(CustomerState state, String query) {
        // Pass state to tools so they can update it
        tools.setState(state);
        
        // Include conversation history
        String conversation = state.getConversationContext();
        
        // Let the LLM process the request with tools
        String response = assistant.provideEVGuidance(conversation);
        
        // Log the agent response (user message already added by GMVehicleGraphAgent)
        state.addAiMessage(response);
        
        return response;
    }
}