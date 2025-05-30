package com.example.agents.multiple;

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

    static class EVTools {
        private final ToolsImpl tools = new ToolsImpl();
        private final ToolLogger logger = new ToolLogger();

        @Tool("Calculate charging costs for an EV")
        public ChargingCost calculateChargingCosts(
                @P("Vehicle ID") String vehicleId,
                @P("ZIP code") String zipCode,
                @P("Miles per year") int milesPerYear,
                @P("Home charging percentage (0-100)") int homeChargingPercentage) {
            logger.logToolCall(
                    "calculateChargingCosts",
                    "vehicleId",
                    vehicleId,
                    "zipCode",
                    zipCode,
                    "milesPerYear",
                    milesPerYear,
                    "homeCharging%",
                    homeChargingPercentage);
            double dailyMiles = milesPerYear / 365.0;
            ChargingCost cost = tools.calculateChargingCosts(vehicleId, zipCode, dailyMiles);
            return cost;
        }

        @Tool("Find charging stations near a location")
        public List<ChargingStation> findChargingStations(
                @P("ZIP code or city") String location,
                @P("Radius in miles") int radiusMiles,
                @P("Charging type (Level2, DC_Fast, All)") String chargingType) {
            logger.logToolCall(
                    "findChargingStations", "location", location, "radius", radiusMiles, "type", chargingType);
            List<ChargingStation> stations = tools.findChargingStations(location, radiusMiles);
            return stations;
        }

        @Tool("Estimate real-world range for an EV")
        public RangeEstimate estimateRange(
                @P("Vehicle ID") String vehicleId,
                @P("Temperature (F)") int temperature,
                @P("Highway percentage (0-100)") int highwayPercentage,
                @P("Use AC/Heat") boolean useClimate) {
            logger.logToolCall(
                    "estimateRange",
                    "vehicleId",
                    vehicleId,
                    "temp",
                    temperature,
                    "highway%",
                    highwayPercentage,
                    "climate",
                    useClimate);
            String weatherCondition = temperature < 32 ? "cold" : temperature > 90 ? "hot" : "moderate";
            double tripDistance = 200;
            RangeEstimate range = tools.estimateRangeForTrip(vehicleId, tripDistance, weatherCondition);
            return range;
        }
    }

    interface EVAssistant {
        @SystemMessage(
                """
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

    public EVSpecialistAgent(ChatModel model, ConversationState conversationState) {
        this.tools = new EVTools();
        this.assistant = AiServices.builder(EVAssistant.class)
                .chatModel(model)
                .tools(tools)
                .chatMemory(conversationState.getChatMemory())
                .build();
    }

    public String execute(String query) {
        String response = assistant.provideEVGuidance(query);
        return response;
    }
}
