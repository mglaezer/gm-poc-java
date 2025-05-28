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
 * Financial Advisor Agent - Handles financing, budgeting, and insurance
 */
public class FinancialAdvisorAgent {

    static class FinancialTools {
        private final ToolsImpl tools = new ToolsImpl();
        private final ToolLogger logger = new ToolLogger();

        @Tool("Calculate financing options for a vehicle")
        public FinancingOption calculateFinancing(
                @P("Vehicle ID") String vehicleId,
                @P("Down payment amount") double downPayment,
                @P("Loan term in months") int termMonths,
                @P("Credit score (excellent, good, fair, poor)") String creditScore) {
            logger.logToolCall(
                    "calculateFinancing",
                    "vehicleId",
                    vehicleId,
                    "downPayment",
                    "$" + downPayment,
                    "termMonths",
                    termMonths,
                    "creditScore",
                    creditScore);
            FinancingOption option = tools.calculateFinancing(vehicleId, downPayment, termMonths, creditScore);

            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: vehicleId=")
                    .append(vehicleId)
                    .append(", downPayment=$")
                    .append(String.format("%.0f", downPayment))
                    .append(", termMonths=")
                    .append(termMonths)
                    .append(", creditScore=")
                    .append(creditScore)
                    .append("\n");

            if (option == null) {
                sb.append("Unable to calculate financing - vehicle not found");
            } else {
                sb.append(String.format("Financing Details:\n"));
                sb.append(String.format("Vehicle Price: $%,.0f\n", option.vehiclePrice()));
                sb.append(String.format(
                        "Down Payment: $%,.0f (%.0f%%)\n",
                        option.downPayment(), (option.downPayment() / option.vehiclePrice() * 100)));
                sb.append(String.format("Loan Amount: $%,.0f\n", option.vehiclePrice() - option.downPayment()));
                sb.append(
                        String.format("Term: %d months | Rate: %.1f%%\n", option.termMonths(), option.interestRate()));
                sb.append(String.format("Monthly Payment: $%,.2f\n", option.monthlyPayment()));
                sb.append(String.format("Total Cost: $%,.0f", option.totalCost()));
            }
            return option;
        }

        @Tool("Compare multiple financing options")
        public List<FinancingOption> compareFinancing(
                @P("Vehicle ID") String vehicleId, @P("Credit score") String creditScore) {
            List<FinancingOption> options = tools.compareFinancingOptions(vehicleId, creditScore);

            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: vehicleId=")
                    .append(vehicleId)
                    .append(", creditScore=")
                    .append(creditScore)
                    .append("\n");

            if (options.isEmpty()) {
                sb.append("No financing options available");
            } else {
                sb.append("Financing Options Comparison:\n");
                for (FinancingOption opt : options) {
                    sb.append(String.format(
                            "- %d months: $%,.2f/mo (%.1f%% APR, $%,.0f down)\n",
                            opt.termMonths(), opt.monthlyPayment(), opt.interestRate(), opt.downPayment()));
                }
            }
            return options;
        }

        @Tool("Calculate insurance costs")
        public InsuranceCost calculateInsurance(
                @P("Vehicle ID") String vehicleId,
                @P("Driver age") int driverAge,
                @P("Driving record (clean, minor_issues, major_issues)") String drivingRecord,
                @P("Coverage level (basic, standard, comprehensive)") String coverageLevel) {
            InsuranceCost insurance = tools.calculateInsuranceCosts(
                    vehicleId, "00000", new DriverProfile(driverAge, "N/A", 10, 0, 0, drivingRecord));

            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: vehicleId=")
                    .append(vehicleId)
                    .append(", driverAge=")
                    .append(driverAge)
                    .append(", drivingRecord=")
                    .append(drivingRecord)
                    .append(", coverage=")
                    .append(coverageLevel)
                    .append("\n");

            if (insurance == null) {
                sb.append("Unable to calculate insurance - vehicle not found");
            } else {
                sb.append(String.format("Insurance Estimate:\n"));
                sb.append(String.format("Monthly Premium: $%.2f\n", insurance.monthlyPremium()));
                sb.append(String.format("Annual Premium: $%,.0f\n", insurance.annualPremium()));
                sb.append(String.format("Coverage Level: %s\n", insurance.coverageLevel()));
                if (!insurance.discountsApplied().isEmpty()) {
                    sb.append("Discounts Applied: ").append(String.join(", ", insurance.discountsApplied()));
                }
            }
            return insurance;
        }

        @Tool("Suggest budget allocation")
        public BudgetRecommendation suggestBudget(
                @P("Annual income") double annualIncome,
                @P("Monthly expenses") double monthlyExpenses,
                @P("Current car payment") double currentCarPayment) {
            BudgetRecommendation budget = tools.suggestBudgetAllocation(annualIncome / 12, monthlyExpenses);

            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: annualIncome=$")
                    .append(String.format("%.0f", annualIncome))
                    .append(", monthlyExpenses=$")
                    .append(String.format("%.0f", monthlyExpenses))
                    .append(", currentCarPayment=$")
                    .append(String.format("%.0f", currentCarPayment))
                    .append("\n");

            if (budget == null) {
                sb.append("Unable to calculate budget recommendation");
            } else {
                sb.append(String.format("Budget Recommendation:\n"));
                sb.append(String.format("Monthly Income: $%,.0f\n", annualIncome / 12));
                sb.append(String.format("Monthly Expenses: $%,.0f\n", monthlyExpenses));
                sb.append(String.format("Recommended Max Vehicle Price: $%,.0f\n", budget.recommendedVehiclePrice()));
                sb.append(String.format("Recommended Down Payment: $%,.0f\n", budget.recommendedDownPayment()));
                sb.append(String.format("Max Monthly Payment: $%,.0f\n", budget.maxMonthlyPayment()));
                sb.append(String.format("Affordability Rating: %s\n", budget.affordabilityRating()));
                if (!budget.suggestions().isEmpty()) {
                    sb.append("Suggestions: ").append(String.join("; ", budget.suggestions()));
                }
            }
            return budget;
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
            Be friendly, professional, and informative, but also humorous! Not too wordy.

            """)
        String provideFinancialAdvice(@UserMessage String conversation);
    }

    private final FinancialAssistant assistant;
    private final FinancialTools tools;
    private final SharedVehicleSearchTools sharedSearchTools;

    public FinancialAdvisorAgent(ChatModel model, ConversationState conversationState) {
        this.tools = new FinancialTools();
        this.sharedSearchTools = new SharedVehicleSearchTools();
        this.assistant = AiServices.builder(FinancialAssistant.class)
                .chatModel(model)
                .tools(tools, sharedSearchTools)
                .chatMemory(conversationState.getChatMemory())
                .build();
    }

    public String execute(String query) {
        String response = assistant.provideFinancialAdvice(query);
        return response;
    }
}
