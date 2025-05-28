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
        private final ToolLogger logger = new ToolLogger();

        @Tool("Calculate trade-in value for current vehicle")
        public TradeInValue calculateTradeIn(
                @P("Vehicle make") String make,
                @P("Vehicle model") String model,
                @P("Year") int year,
                @P("Mileage") int mileage,
                @P("Condition (excellent, good, fair, poor)") String condition) {
            logger.logToolCall(
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
            TradeInValue tradeIn = tools.calculateTradeInValue(vehicleTradeIn);

            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: vehicle=")
                    .append(year)
                    .append(" ")
                    .append(make)
                    .append(" ")
                    .append(model)
                    .append(", mileage=")
                    .append(String.format("%,d", mileage))
                    .append(", condition=")
                    .append(condition)
                    .append("\n");

            if (tradeIn == null) {
                sb.append("Unable to calculate trade-in value");
            } else {
                sb.append(String.format("Trade-In Value Estimate:\n"));
                sb.append(String.format("Dealer Offer: $%,.0f\n", tradeIn.dealerValue()));
                sb.append(String.format("Private Party: $%,.0f\n", tradeIn.privatePartyValue()));
                sb.append(String.format("Fair Market: $%,.0f\n", tradeIn.fairValue()));
                sb.append(String.format("Market Demand: %s\n", tradeIn.marketDemand()));
                if (!tradeIn.valueFactors().isEmpty()) {
                    sb.append("Value Factors: ").append(String.join(", ", tradeIn.valueFactors()));
                }
            }
            return tradeIn;
        }

        @Tool("Suggest negotiation strategy")
        public NegotiationStrategy suggestStrategy(
                @P("Vehicle ID") String vehicleId,
                @P("Market conditions (buyers_market, sellers_market, balanced)") String marketConditions,
                @P("Time of year") String timeOfYear,
                @P("Urgency (high, medium, low)") String urgency) {
            logger.logToolCall(
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
            NegotiationStrategy strategy = tools.suggestNegotiationStrategy(vehicleId, conditions);

            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: vehicleId=")
                    .append(vehicleId)
                    .append(", market=")
                    .append(marketConditions)
                    .append(", timing=")
                    .append(timeOfYear)
                    .append(", urgency=")
                    .append(urgency)
                    .append("\n");

            if (strategy == null) {
                sb.append("Unable to generate negotiation strategy - vehicle not found");
            } else {
                sb.append(String.format("Negotiation Strategy:\n"));
                sb.append(String.format("Target Price: $%,.0f\n", strategy.targetPrice()));
                sb.append(String.format("Walk-Away Price: $%,.0f\n", strategy.walkAwayPrice()));
                sb.append(String.format("Best Time to Negotiate: %s\n", strategy.bestTimeToNegotiate()));
                sb.append("Key Points:\n");
                for (String point : strategy.negotiationPoints()) {
                    sb.append("- ").append(point).append("\n");
                }
                if (!strategy.leveragePoints().isEmpty()) {
                    sb.append("Leverage: ").append(String.join(", ", strategy.leveragePoints()));
                }
            }
            return strategy;
        }

        @Tool("Find current incentives and rebates")
        public List<Incentive> findIncentives(
                @P("Vehicle ID") String vehicleId,
                @P("ZIP code") String zipCode,
                @P("Customer type (general, military, student, first_responder)") String customerType) {
            logger.logToolCall(
                    "findIncentives", "vehicleId", vehicleId, "zipCode", zipCode, "customerType", customerType);
            List<Incentive> incentives = tools.findIncentivesAndRebates(vehicleId, zipCode);

            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: vehicleId=")
                    .append(vehicleId)
                    .append(", zipCode=")
                    .append(zipCode)
                    .append(", customerType=")
                    .append(customerType)
                    .append("\n");

            if (incentives.isEmpty()) {
                sb.append("No current incentives found for this vehicle");
            } else {
                sb.append("Available Incentives (").append(incentives.size()).append(" found):\n");
                double totalSavings = 0;
                for (Incentive inc : incentives) {
                    sb.append(String.format("- %s: $%,.0f - %s\n", inc.type(), inc.amount(), inc.description()));
                    if (inc.expirationDate() != null) {
                        sb.append(String.format("  Expires: %s\n", inc.expirationDate()));
                    }
                    if (!inc.eligibilityRequirements().isEmpty()) {
                        sb.append("  Requirements: ")
                                .append(String.join(", ", inc.eligibilityRequirements()))
                                .append("\n");
                    }
                    totalSavings += inc.amount();
                }
                sb.append(String.format("Total Potential Savings: $%,.0f", totalSavings));
            }
            return incentives;
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
    private final NegotiationTools tools;

    public NegotiationCoachAgent(ChatModel model, ConversationState conversationState) {
        this.tools = new NegotiationTools();
        this.assistant = AiServices.builder(NegotiationAssistant.class)
                .chatModel(model)
                .tools(tools)
                .chatMemory(conversationState.getChatMemory())
                .build();
    }

    public String execute(String query) {
        String response = assistant.provideNegotiationCoaching(query);
        return response;
    }
}
