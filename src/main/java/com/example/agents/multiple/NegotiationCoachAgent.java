package com.example.agents.multiple;

import com.example.agents.CommonRequirements.*;
import com.example.agents.ToolsImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import java.util.List;
import org.llmtoolkit.core.JteTemplateProcessor;
import org.llmtoolkit.core.TemplatedLLMServiceFactory;
import org.llmtoolkit.core.annotations.PT;

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
        @PT(templatePath = "negotiation_coach.jte")
        String provideNegotiationCoaching();
    }

    private final NegotiationAssistant assistant;
    private final ConversationState conversationState;

    public NegotiationCoachAgent(ChatModel model, ConversationState conversationState) {
        this.conversationState = conversationState;
        this.assistant = TemplatedLLMServiceFactory.builder()
                .model(model)
                .templateProcessor(JteTemplateProcessor.create())
                .aiServiceCustomizer(aiServices -> {
                    aiServices.tools(new NegotiationTools(), new SharedVehicleSearchTools());
                    aiServices.chatMemory(conversationState.getChatMemory());
                })
                .build()
                .create(NegotiationAssistant.class);
    }

    public String execute(String query) {
        conversationState.getChatMemory().add(UserMessage.from(query));
        return assistant.provideNegotiationCoaching();
    }
}
