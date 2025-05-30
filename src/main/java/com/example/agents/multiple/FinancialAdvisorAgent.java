package com.example.agents.multiple;

import com.example.agents.CommonRequirements.BudgetRecommendation;
import com.example.agents.CommonRequirements.DriverProfile;
import com.example.agents.CommonRequirements.FinancingOption;
import com.example.agents.CommonRequirements.InsuranceCost;
import com.example.agents.ToolsImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
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
        @SystemMessage(
                """
                        You are a financial advisor specializing in vehicle financing.
                        Help customers understand their financing options and make informed decisions.

                        IMPORTANT: When a user asks about financing, leasing, or insurance for a specific vehicle:
                        - If you don't know the vehicle ID, use the searchByMakeModel tool to find it first
                        - Never ask the user for a vehicle ID - look it up yourself using the make and model
                        - Once you have the vehicle ID, proceed with the financial calculations

                        If the user repeatedly insists on showing financing options without providing all financial details, use some good defaults.
                        If the user wants to compare financing options, use the corresponding tools several times and compare.

                        Use available tools and never ask for anything more than required by the tools.
                        Use tools, do not invent financing or insurance options by yourself.
                        Be friendly, professional, and informative! Important: Not too wordy.

                        """)
        String provideFinancialAdvice(@UserMessage String conversation);
    }

    private final FinancialAssistant assistant;

    public FinancialAdvisorAgent(ChatModel model, ConversationState conversationState) {
        this.assistant = AiServices.builder(FinancialAssistant.class)
                .chatModel(model)
                .tools(new FinancialTools(), new SharedVehicleSearchTools())
                .chatMemory(conversationState.getChatMemory())
                .build();
    }

    public String execute(String query) {
        return assistant.provideFinancialAdvice(query);
    }
}
