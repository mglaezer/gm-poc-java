package com.example.agents.langgraph;

import com.example.agents.CommonRequirements.*;
import com.example.agents.ToolsImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.util.List;

/**
 * Negotiation Coach Agent - Helps with pricing strategy and incentives
 */
public class NegotiationCoachAgent implements AgentNode {
    
    static class NegotiationTools extends BaseToolLogger {
        private final ToolsImpl tools = new ToolsImpl();
        
        @Tool("Calculate trade-in value")
        public TradeInValue calculateTradeIn(
                @P("Make (Chevrolet, GMC, etc)") String make,
                @P("Model") String model,
                @P("Year") int year,
                @P("Mileage") int mileage,
                @P("Condition (excellent, good, fair, poor)") String condition) {
            VehicleMake vehicleMake = VehicleMake.fromString(make);
            if (vehicleMake == null) vehicleMake = VehicleMake.CHEVROLET;
            VehicleTradeIn tradeIn = new VehicleTradeIn(vehicleMake, model, year, mileage, condition, List.of());
            return tools.calculateTradeInValue(tradeIn);
        }
        
        @Tool("Suggest negotiation strategy")
        public NegotiationStrategy suggestStrategy(
                @P("Vehicle ID") String vehicleId,
                @P("Inventory level (high, medium, low)") String inventoryLevel,
                @P("Is end of month?") boolean endOfMonth,
                @P("Is end of year?") boolean endOfYear,
                @P("Season (spring, summer, fall, winter)") String season) {
            MarketConditions conditions = new MarketConditions(
                inventoryLevel, "medium", endOfMonth, endOfYear, season
            );
            return tools.suggestNegotiationStrategy(vehicleId, conditions);
        }
        
        @Tool("Find available incentives and rebates")
        public List<Incentive> findIncentives(
                @P("Vehicle ID") String vehicleId,
                @P("ZIP code") String zipCode) {
            return tools.findIncentivesAndRebates(vehicleId, zipCode);
        }
    }
    
    interface NegotiationAssistant {
        @SystemMessage("""
            You are a negotiation coach helping customers get the best deal on GM vehicles.
            You provide strategic advice on pricing and timing.
            
            You can:
            - Calculate trade-in values
            - Suggest negotiation strategies
            - Find available incentives and rebates
            - Advise on best times to buy
            - Explain dealer margins and pricing
            - Provide tips for negotiating
            
            Use the tools to provide data-driven advice.
            Be honest and transparent about pricing.
            Help customers feel confident in negotiations.
            """)
        String provideNegotiationAdvice(@UserMessage String conversation);
    }
    
    private final NegotiationAssistant assistant;
    private final NegotiationTools tools;
    
    public NegotiationCoachAgent(ChatLanguageModel model) {
        this.tools = new NegotiationTools();
        this.assistant = AiServices.builder(NegotiationAssistant.class)
                .chatLanguageModel(model)
                .tools(tools)
                .build();
    }
    
    @Override
    public CustomerState process(CustomerState state) {
        String query = state.getCurrentQuery();
        String conversation = String.join("\n", state.getConversationHistory());
        conversation += "\nUser: " + query;
        
        // Add selected vehicle context if available
        VehicleInfo selectedVehicle = state.getSelectedVehicle();
        if (selectedVehicle != null) {
            conversation += "\nSelected Vehicle: " + selectedVehicle.make().getDisplayName() + 
                          " " + selectedVehicle.model() + " - MSRP: $" + selectedVehicle.price();
        }
        
        String response = assistant.provideNegotiationAdvice(conversation);
        state.addToConversationHistory("Negotiation Coach: " + response);
        
        return state;
    }
    
    @Override
    public String getName() {
        return "NEGOTIATION_COACH";
    }
}