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
import java.util.ArrayList;

/**
 * Negotiation Coach Agent - Helps with pricing strategy and trade-ins
 */
public class NegotiationCoachAgent {
    
    static class NegotiationTools extends BaseToolLogger {
        private final ToolsImpl tools = new ToolsImpl();
        private CustomerState state;
        
        public void setState(CustomerState state) {
            this.state = state;
        }
        
        @Tool("Calculate trade-in value for current vehicle")
        public TradeInValue calculateTradeIn(
                @P("Vehicle make") String make,
                @P("Vehicle model") String model,
                @P("Year") int year,
                @P("Mileage") int mileage,
                @P("Condition (excellent, good, fair, poor)") String condition) {
            logToolCall("calculateTradeIn", "make", make, "model", model, "year", year, 
                       "mileage", mileage, "condition", condition);
            VehicleTradeIn vehicleTradeIn = new VehicleTradeIn(
                VehicleMake.fromString(make), model, year, mileage, condition, List.of());
            TradeInValue tradeIn = tools.calculateTradeInValue(vehicleTradeIn);
            if (state != null && tradeIn != null) {
                state.logToolCall("NEGOTIATION_COACH", "calculateTradeIn",
                    String.format("%d %s %s, %d miles, %s", year, make, model, mileage, condition),
                    String.format("Trade-in value: $%,.0f", tradeIn.dealerValue()));
            }
            return tradeIn;
        }
        
        @Tool("Suggest negotiation strategy")
        public NegotiationStrategy suggestStrategy(
                @P("Vehicle ID") String vehicleId,
                @P("Market conditions (buyers_market, sellers_market, balanced)") String marketConditions,
                @P("Time of year") String timeOfYear,
                @P("Urgency (high, medium, low)") String urgency) {
            logToolCall("suggestStrategy", "vehicleId", vehicleId, "market", marketConditions, 
                       "timeOfYear", timeOfYear, "urgency", urgency);
            MarketConditions conditions = new MarketConditions(
                marketConditions.contains("buyers") ? "high" : "low",
                marketConditions.contains("sellers") ? "high" : "low",
                timeOfYear.toLowerCase().contains("end") && timeOfYear.toLowerCase().contains("month"),
                timeOfYear.toLowerCase().contains("end") && timeOfYear.toLowerCase().contains("year"),
                timeOfYear
            );
            NegotiationStrategy strategy = tools.suggestNegotiationStrategy(vehicleId, conditions);
            if (state != null && strategy != null) {
                state.logToolCall("NEGOTIATION_COACH", "suggestStrategy",
                    String.format("vehicleId=%s, market=%s", vehicleId, marketConditions),
                    "Strategy: Target $" + String.format("%.0f", strategy.targetPrice()));
            }
            return strategy;
        }
        
        @Tool("Find current incentives and rebates")
        public List<Incentive> findIncentives(
                @P("Vehicle ID") String vehicleId,
                @P("ZIP code") String zipCode,
                @P("Customer type (general, military, student, first_responder)") String customerType) {
            logToolCall("findIncentives", "vehicleId", vehicleId, "zipCode", zipCode, "customerType", customerType);
            List<Incentive> incentives = tools.findIncentivesAndRebates(vehicleId, zipCode);
            if (state != null) {
                state.logToolCall("NEGOTIATION_COACH", "findIncentives",
                    String.format("vehicleId=%s, zipCode=%s, type=%s", vehicleId, zipCode, customerType),
                    String.format("Found %d incentives", incentives.size()));
            }
            return incentives;
        }
    }
    
    interface NegotiationAssistant {
        @SystemMessage("""
            You are a negotiation coach helping customers get the best deal on GM vehicles.
            Your expertise includes:
            - Trade-in value assessment
            - Market timing and seasonal trends
            - Negotiation tactics and strategies
            - Understanding of dealer incentives and rebates
            - Price comparison and fair market value
            
            Always:
            - Empower customers with knowledge
            - Suggest realistic negotiation targets
            - Explain the rationale behind strategies
            - Consider the customer's specific situation
            - Be honest about market conditions
            - Help customers understand total deal value (not just monthly payments)
            """)
        String provideNegotiationCoaching(@UserMessage String conversation);
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
    
    public String execute(CustomerState state, String query) {
        // Pass state to tools so they can update it
        tools.setState(state);
        
        // Include conversation history
        String conversation = state.getConversationContext();
        
        // Let the LLM process the request with tools
        String response = assistant.provideNegotiationCoaching(conversation);
        
        // Log the agent response (user message already added by GMVehicleGraphAgent)
        state.addAiMessage(response);
        
        return response;
    }
}