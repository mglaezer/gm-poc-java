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
            
            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: vehicleId=").append(vehicleId)
              .append(", zipCode=").append(zipCode)
              .append(", milesPerYear=").append(milesPerYear)
              .append(", homeCharging=").append(homeChargingPercentage).append("%\n");
            
            if (cost == null) {
                sb.append("Unable to calculate charging costs - vehicle not found or not an EV");
            } else {
                sb.append(String.format("EV Charging Cost Analysis:\n"));
                sb.append(String.format("Daily Miles: %.1f | Annual Miles: %,d\n", dailyMiles, milesPerYear));
                sb.append(String.format("Daily Cost: $%.2f\n", cost.dailyCost()));
                sb.append(String.format("Monthly Cost: $%.2f\n", cost.monthlyCost()));
                sb.append(String.format("Annual Cost: $%.2f\n", cost.monthlyCost() * 12));
                sb.append(String.format("Cost per Mile: $%.3f\n", cost.costPerMile()));
                sb.append(String.format("Home Charging: $%.2f/kWh | Public: $%.2f/kWh", 
                    cost.homeChargingCost(), cost.publicChargingCost()));
            }
            state.addToolResult("calculateChargingCosts", sb.toString());
            return cost;
        }
        
        @Tool("Find charging stations near a location")
        public List<ChargingStation> findChargingStations(
                @P("ZIP code or city") String location,
                @P("Radius in miles") int radiusMiles,
                @P("Charging type (Level2, DC_Fast, All)") String chargingType) {
            logToolCall("findChargingStations", "location", location, "radius", radiusMiles, "type", chargingType);
            List<ChargingStation> stations = tools.findChargingStations(location, radiusMiles);
            
            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: location=").append(location)
              .append(", radius=").append(radiusMiles).append(" miles")
              .append(", type=").append(chargingType).append("\n");
            
            if (stations.isEmpty()) {
                sb.append("No charging stations found in the specified area");
            } else {
                sb.append("Found ").append(stations.size()).append(" charging stations:\n");
                for (ChargingStation station : stations) {
                    sb.append(String.format("- %s (%s) - %.1f mi away\n", 
                        station.name(), station.address(), station.distance()));
                    sb.append(String.format("  Type: %s | Network: %s | Ports: %d | Cost: $%.2f/kWh\n",
                        station.chargerType(), station.network(), 
                        station.availablePorts(), station.costPerKwh()));
                }
            }
            state.addToolResult("findChargingStations", sb.toString());
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
            String weatherCondition = temperature < 32 ? "cold" : temperature > 90 ? "hot" : "moderate";
            double tripDistance = 200;
            RangeEstimate range = tools.estimateRangeForTrip(vehicleId, tripDistance, weatherCondition);
            
            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: vehicleId=").append(vehicleId)
              .append(", temperature=").append(temperature).append("°F")
              .append(", highway=").append(highwayPercentage).append("%")
              .append(", climate=").append(useClimate ? "on" : "off").append("\n");
            
            if (range == null) {
                sb.append("Unable to estimate range - vehicle not found or not an EV");
            } else {
                sb.append(String.format("Range Estimate:\n"));
                sb.append(String.format("EPA Rated Range: %.0f miles\n", range.baseRange()));
                sb.append(String.format("Adjusted Range: %.0f miles\n", range.adjustedRange()));
                sb.append(String.format("Range Reduction: %.0f%% (%.0f miles)\n", 
                    (range.rangeReduction() * 100), range.baseRange() - range.adjustedRange()));
                sb.append("Affecting Factors: ").append(String.join(", ", range.affectingFactors())).append("\n");
                sb.append("Can Complete 200mi Trip: ").append(range.canCompleteTrip() ? "Yes" : "No");
            }
            state.addToolResult("estimateRange", sb.toString());
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