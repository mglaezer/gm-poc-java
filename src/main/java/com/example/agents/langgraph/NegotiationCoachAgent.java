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
 * Negotiation Coach Agent - Helps with pricing strategy and incentives
 */
public class NegotiationCoachAgent implements AgentNode {
    
    static class NegotiationTools extends BaseToolLogger {
        private final ToolsImpl tools = new ToolsImpl();
        private CustomerState state;
        
        public void setState(CustomerState state) {
            this.state = state;
        }
        
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
            TradeInValue value = tools.calculateTradeInValue(tradeIn);
            if (state != null && value != null) {
                state.logToolCall("NEGOTIATION_COACH", "calculateTradeIn", 
                    String.format("make=%s, model=%s, year=%d, mileage=%d, condition=%s", make, model, year, mileage, condition),
                    "Trade-in value for " + year + " " + make + " " + model + ": $" + value.fairValue());
            }
            return value;
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
            NegotiationStrategy strategy = tools.suggestNegotiationStrategy(vehicleId, conditions);
            if (state != null && strategy != null) {
                state.logToolCall("NEGOTIATION_COACH", "suggestStrategy", 
                    String.format("vehicleId=%s, inventoryLevel=%s, endOfMonth=%b", vehicleId, inventoryLevel, endOfMonth),
                    "Negotiation strategy for " + vehicleId + ": " + strategy.bestTimeToNegotiate());
            }
            return strategy;
        }
        
        @Tool("Find available incentives and rebates")
        public List<Incentive> findIncentives(
                @P("Vehicle ID") String vehicleId,
                @P("ZIP code") String zipCode) {
            List<Incentive> incentives = tools.findIncentivesAndRebates(vehicleId, zipCode);
            if (state != null && !incentives.isEmpty()) {
                double totalSavings = incentives.stream().mapToDouble(Incentive::amount).sum();
                state.logToolCall("NEGOTIATION_COACH", "findIncentives", 
                    String.format("vehicleId=%s, zipCode=%s", vehicleId, zipCode),
                    "Found " + incentives.size() + " incentives for " + vehicleId + " totaling $" + totalSavings);
            }
            return incentives;
        }
    }
    
    interface NegotiationAssistant {
        @SystemMessage("""
            You are a negotiation coach helping customers get the best deal on GM vehicles.
            You provide strategic advice on pricing and timing.
            
            IMPORTANT: Extract relevant information from the conversation history.
            Look for mentions of:
            - Trade-in vehicles
            - Budget constraints
            - Timing preferences
            - Previous negotiation experiences
            
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
    
    public NegotiationCoachAgent(ChatModel model) {
        this.tools = new NegotiationTools();
        this.assistant = AiServices.builder(NegotiationAssistant.class)
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
        
        String response = assistant.provideNegotiationAdvice(conversation);
        state.addToConversationHistory("Negotiation Coach: " + response);
        
        return state;
    }
    
    @Override
    public String getName() {
        return "NEGOTIATION_COACH";
    }
}