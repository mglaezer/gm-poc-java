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
public class FinancialAdvisorAgent implements AgentNode {
    
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
                    "Compared " + options.size() + " financing options for " + vehicleId);
            }
            return options;
        }
        
        @Tool("Calculate insurance costs")
        public InsuranceCost calculateInsurance(
                @P("Vehicle ID") String vehicleId,
                @P("ZIP code") String zipCode,
                @P("Driver age") int age,
                @P("Years licensed") int yearsLicensed,
                @P("Accidents in last 5 years") int accidents,
                @P("Credit score") String creditScore) {
            DriverProfile driver = new DriverProfile(age, "unknown", yearsLicensed, accidents, 0, creditScore);
            InsuranceCost cost = tools.calculateInsuranceCosts(vehicleId, zipCode, driver);
            if (state != null && cost != null) {
                state.logToolCall("FINANCIAL_ADVISOR", "calculateInsurance", 
                    String.format("vehicleId=%s, zipCode=%s, age=%d, yearsLicensed=%d", vehicleId, zipCode, age, yearsLicensed),
                    "Calculated insurance for " + vehicleId + ": $" + cost.monthlyPremium() + "/month");
            }
            return cost;
        }
        
        @Tool("Suggest budget allocation")
        public BudgetRecommendation suggestBudget(
                @P("Monthly income") double monthlyIncome,
                @P("Monthly expenses") double monthlyExpenses) {
            BudgetRecommendation budget = tools.suggestBudgetAllocation(monthlyIncome, monthlyExpenses);
            if (state != null && budget != null) {
                state.logToolCall("FINANCIAL_ADVISOR", "suggestBudget", 
                    String.format("monthlyIncome=%.2f, monthlyExpenses=%.2f", monthlyIncome, monthlyExpenses),
                    "Suggested max car payment: $" + budget.maxMonthlyPayment() + "/month");
            }
            return budget;
        }
    }
    
    interface FinancialAssistant {
        @SystemMessage("""
            You are a financial advisor specializing in vehicle financing.
            You help customers understand financing options and make informed decisions.
            
            IMPORTANT: Extract budget and credit score information from the conversation history.
            Look for mentions of:
            - Budget ranges or monthly payment preferences
            - Credit score or credit history
            - Down payment capabilities
            - Current monthly expenses
            
            You can:
            - Calculate monthly payments based on the customer's budget
            - Compare financing terms using their credit score
            - Estimate insurance costs
            - Analyze total cost of ownership
            - Suggest budget allocations
            - Explain financing concepts
            
            Use the tools to provide accurate calculations.
            Be clear about all costs and fees.
            Focus on options that fit within the customer's stated budget.
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
    
    @Override
    public CustomerState process(CustomerState state) {
        // Pass state to tools so they can update it
        tools.setState(state);
        
        String query = state.getCurrentQuery();
        String conversation = String.join("\n", state.getConversationHistory());
        
        conversation += "\nUser: " + query;
        
        String response = assistant.provideFinancialAdvice(conversation);
        state.addToConversationHistory("Financial Advisor: " + response);
        
        return state;
    }
    
    @Override
    public String getName() {
        return "FINANCIAL_ADVISOR";
    }
}