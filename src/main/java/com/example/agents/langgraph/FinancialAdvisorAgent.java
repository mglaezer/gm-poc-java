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
 * Financial Advisor Agent - Handles financing, budgeting, and insurance
 */
public class FinancialAdvisorAgent {
    
    static class FinancialTools extends BaseToolLogger {
        private final ToolsImpl tools = new ToolsImpl();
        private CustomerState state;
        
        public void setState(CustomerState state) {
            this.state = state;
        }
        
        @Tool("Calculate financing options for a vehicle")
        public FinancingOption calculateFinancing(
                @P("Vehicle ID") String vehicleId,
                @P("Down payment amount") double downPayment,
                @P("Loan term in months") int termMonths,
                @P("Credit score (excellent, good, fair, poor)") String creditScore) {
            logToolCall("calculateFinancing", "vehicleId", vehicleId, "downPayment", "$" + downPayment, "termMonths", termMonths);
            FinancingOption option = tools.calculateFinancing(vehicleId, downPayment, termMonths, creditScore);
            if (state != null && option != null) {
                state.logToolCall("FINANCIAL_ADVISOR", "calculateFinancing", 
                    String.format("vehicleId=%s, downPayment=%.2f, termMonths=%d, creditScore=%s", vehicleId, downPayment, termMonths, creditScore),
                    "Calculated financing for " + vehicleId + ": $" + option.monthlyPayment() + "/month");
            }
            return option;
        }
        
        @Tool("Compare multiple financing options")
        public List<FinancingOption> compareFinancing(
                @P("Vehicle ID") String vehicleId,
                @P("Credit score") String creditScore) {
            List<FinancingOption> options = tools.compareFinancingOptions(vehicleId, creditScore);
            if (state != null && !options.isEmpty()) {
                state.logToolCall("FINANCIAL_ADVISOR", "compareFinancing", 
                    String.format("vehicleId=%s, creditScore=%s", vehicleId, creditScore),
                    String.format("Generated %d financing options", options.size()));
            }
            return options;
        }
        
        @Tool("Calculate insurance costs")
        public InsuranceCost calculateInsurance(
                @P("Vehicle ID") String vehicleId,
                @P("Driver age") int driverAge,
                @P("Driving record (clean, minor_issues, major_issues)") String drivingRecord,
                @P("Coverage level (basic, standard, comprehensive)") String coverageLevel) {
            InsuranceCost insurance = tools.calculateInsuranceCosts(vehicleId, "00000",
                new DriverProfile(driverAge, "N/A", 10, 0, 0, drivingRecord));
            if (state != null && insurance != null) {
                state.logToolCall("FINANCIAL_ADVISOR", "calculateInsurance", 
                    String.format("vehicleId=%s, age=%d, record=%s", vehicleId, driverAge, drivingRecord),
                    String.format("Monthly premium: $%.2f", insurance.monthlyPremium()));
            }
            return insurance;
        }
        
        @Tool("Suggest budget allocation")
        public BudgetRecommendation suggestBudget(
                @P("Annual income") double annualIncome,
                @P("Monthly expenses") double monthlyExpenses,
                @P("Current car payment") double currentCarPayment) {
            BudgetRecommendation budget = tools.suggestBudgetAllocation(annualIncome / 12, monthlyExpenses);
            if (state != null && budget != null) {
                state.logToolCall("FINANCIAL_ADVISOR", "suggestBudget", 
                    String.format("income=$%.0f, expenses=$%.0f", annualIncome, monthlyExpenses),
                    String.format("Recommended max payment: $%.2f", budget.maxMonthlyPayment()));
            }
            return budget;
        }
    }
    
    interface FinancialAssistant {
        @SystemMessage("""
            You are a financial advisor specializing in vehicle financing.
            Help customers understand their financing options and make informed decisions.
            
            Your expertise includes:
            - Calculating monthly payments and total costs
            - Comparing lease vs buy options
            - Estimating insurance costs
            - Budget planning and affordability analysis
            
            Always:
            - Consider the customer's financial situation
            - Explain the pros and cons of different options
            - Suggest the most cost-effective solutions
            - Be transparent about all costs involved
            """)
        String provideFinancialAdvice(@UserMessage String conversation);
    }
    
    private final FinancialAssistant assistant;
    private final FinancialTools tools;
    
    public FinancialAdvisorAgent(ChatModel model) {
        this.tools = new FinancialTools();
        this.assistant = AiServices.builder(FinancialAssistant.class)
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
        String response = assistant.provideFinancialAdvice(conversation);
        
        // Log the agent response (user message already added by GMVehicleGraphAgent)
        state.addAiMessage(response);
        
        return response;
    }
}