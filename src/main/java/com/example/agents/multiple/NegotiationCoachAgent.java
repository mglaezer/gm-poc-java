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
 * Negotiation Coach Agent - Helps with pricing strategy and trade-ins
 */
public class NegotiationCoachAgent {

    static class NegotiationTools {
        private final ToolsImpl tools = new ToolsImpl();

        @Tool("Calculate trade-in value for current vehicle")
        public TradeInValue calculateTradeIn(
                @P("Vehicle make") String make,
                @P("Vehicle model") String model,
                @P("Year") int year,
                @P("Mileage") int mileage,
                @P("Condition (excellent, good, fair, poor)") String condition) {
            ToolLogger.logToolCall(
                    "calculateTradeIn",
                    "make",
                    make,
                    "model",
                    model,
                    "year",
                    year,
                    "mileage",
                    mileage,
                    "condition",
                    condition);
            VehicleTradeIn vehicleTradeIn =
                    new VehicleTradeIn(VehicleMake.fromString(make), model, year, mileage, condition, List.of());

            return tools.calculateTradeInValue(vehicleTradeIn);
        }

        @Tool("Suggest negotiation strategy")
        public NegotiationStrategy suggestStrategy(
                @P("Vehicle ID") String vehicleId,
                @P("Market conditions (buyers_market, sellers_market, balanced)") String marketConditions,
                @P("Time of year") String timeOfYear,
                @P("Urgency (high, medium, low)") String urgency) {
            ToolLogger.logToolCall(
                    "suggestStrategy",
                    "vehicleId",
                    vehicleId,
                    "market",
                    marketConditions,
                    "timeOfYear",
                    timeOfYear,
                    "urgency",
                    urgency);
            MarketConditions conditions = new MarketConditions(
                    marketConditions.contains("buyers") ? "high" : "low",
                    marketConditions.contains("sellers") ? "high" : "low",
                    timeOfYear.toLowerCase().contains("end")
                            && timeOfYear.toLowerCase().contains("month"),
                    timeOfYear.toLowerCase().contains("end")
                            && timeOfYear.toLowerCase().contains("year"),
                    timeOfYear);

            return tools.suggestNegotiationStrategy(vehicleId, conditions);
        }

        @Tool("Find current incentives and rebates")
        public List<Incentive> findIncentives(
                @P("Vehicle ID") String vehicleId,
                @P("ZIP code") String zipCode,
                @P("Customer type (general, military, student, first_responder)") String customerType) {
            ToolLogger.logToolCall(
                    "findIncentives", "vehicleId", vehicleId, "zipCode", zipCode, "customerType", customerType);

            return tools.findIncentivesAndRebates(vehicleId, zipCode);
        }
    }

    interface NegotiationAssistant {
        @SystemMessage(
                """
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

    public NegotiationCoachAgent(ChatModel model, ConversationState conversationState) {
        this.assistant = AiServices.builder(NegotiationAssistant.class)
                .chatModel(model)
                .tools(new NegotiationTools())
                .chatMemory(conversationState.getChatMemory())
                .build();
    }

    public String execute(String query) {
        return assistant.provideNegotiationCoaching(query);
    }
}
