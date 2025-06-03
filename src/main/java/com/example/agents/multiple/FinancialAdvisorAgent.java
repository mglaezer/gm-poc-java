package com.example.agents.multiple;

import com.example.agents.CommonRequirements.BudgetRecommendation;
import com.example.agents.CommonRequirements.DriverProfile;
import com.example.agents.CommonRequirements.FinancingOption;
import com.example.agents.CommonRequirements.InsuranceCost;
import com.example.agents.ToolsImpl;
import com.example.llmtoolkit.core.JacksonSourceResponseStructuringStrategy;
import com.example.llmtoolkit.core.JteTemplateProcessor;
import com.example.llmtoolkit.core.TemplatedLLMServiceFactory;
import com.example.llmtoolkit.core.annotations.PT;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import java.util.List;

/**
 * Financial Advisor Agent - Handles financing, budgeting, and insurance
 */
public class FinancialAdvisorAgent {

    static class FinancialTools {
        private final ToolsImpl tools = new ToolsImpl();

        @Tool("Calculate financing options for a vehicle")
        public FinancingOption calculateFinancing(
                @P("Vehicle ID") String vehicleId,
                @P("Down payment amount") double downPayment,
                @P("Loan term in months") int termMonths,
                @P("Credit score (excellent, good, fair, poor)") String creditScore) {
            ToolLogger.logToolCall(
                    "calculateFinancing",
                    "vehicleId",
                    vehicleId,
                    "downPayment",
                    "$" + downPayment,
                    "termMonths",
                    termMonths,
                    "creditScore",
                    creditScore);

            return tools.calculateFinancing(vehicleId, downPayment, termMonths, creditScore);
        }

        @Tool("Compare multiple financing options")
        public List<FinancingOption> compareFinancing(
                @P("Vehicle ID") String vehicleId, @P("Credit score") String creditScore) {

            return tools.compareFinancingOptions(vehicleId, creditScore);
        }

        @Tool("Calculate insurance costs")
        public InsuranceCost calculateInsurance(
                @P("Vehicle ID") String vehicleId,
                @P("Driver age") int driverAge,
                @P("Driving record (clean, minor_issues, major_issues)") String drivingRecord,
                @P("Coverage level (basic, standard, comprehensive)") String coverageLevel) {

            return tools.calculateInsuranceCosts(
                    vehicleId, "00000", new DriverProfile(driverAge, "N/A", 10, 0, 0, drivingRecord));
        }

        @Tool("Suggest budget allocation")
        public BudgetRecommendation suggestBudget(
                @P("Annual income") double annualIncome,
                @P("Monthly expenses") double monthlyExpenses,
                @P("Current car payment") double currentCarPayment) {

            return tools.suggestBudgetAllocation(annualIncome / 12, monthlyExpenses);
        }
    }

    interface FinancialAssistant {
        @PT(templatePath = "financial_advisor.jte")
        AgentResponse provideFinancialAdvice();
    }

    private final FinancialAssistant assistant;
    private final ConversationState conversationState;

    public FinancialAdvisorAgent(ChatModel model, ConversationState conversationState) {
        this.conversationState = conversationState;
        this.assistant = TemplatedLLMServiceFactory.builder()
                .serviceStrategy(new JacksonSourceResponseStructuringStrategy())
                .model(model)
                .templateProcessor(JteTemplateProcessor.create())
                .aiServiceCustomizer(aiServices -> {
                    aiServices.tools(new FinancialTools(), new SharedVehicleSearchTools());
                    aiServices.chatMemory(conversationState.getChatMemory());
                })
                .build()
                .create(FinancialAssistant.class);
    }

    public AgentResponse execute(String query) {
        conversationState.getChatMemory().add(UserMessage.from(query));
        return assistant.provideFinancialAdvice();
    }
}
