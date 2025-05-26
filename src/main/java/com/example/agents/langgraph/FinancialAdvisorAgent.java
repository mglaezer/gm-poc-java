package com.example.agents.langgraph;

import com.example.agents.CommonRequirements.*;
import com.example.agents.ToolsImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatLanguageModel;
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
                state.setSelectedFinancing(option);
            }
            return option;
        }
        
        @Tool("Compare multiple financing options")
        public List<FinancingOption> compareFinancing(
                @P("Vehicle ID") String vehicleId,
                @P("Credit score") String creditScore) {
            List<FinancingOption> options = tools.compareFinancingOptions(vehicleId, creditScore);
            if (state != null && !options.isEmpty()) {
                state.setFinancingOptions(options);
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
            return tools.calculateInsuranceCosts(vehicleId, zipCode, driver);
        }
        
        @Tool("Suggest budget allocation")
        public BudgetRecommendation suggestBudget(
                @P("Monthly income") double monthlyIncome,
                @P("Monthly expenses") double monthlyExpenses) {
            return tools.suggestBudgetAllocation(monthlyIncome, monthlyExpenses);
        }
    }
    
    interface FinancialAssistant {
        @SystemMessage("""
            You are a financial advisor specializing in vehicle financing.
            You help customers understand financing options and make informed decisions.
            
            IMPORTANT: Use the customer's budget and credit score from their profile.
            Don't ask for information that was already collected by the Customer Profiler.
            
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
    
    public FinancialAdvisorAgent(ChatLanguageModel model) {
        this.tools = new FinancialTools();
        this.assistant = AiServices.builder(FinancialAssistant.class)
                .chatLanguageModel(model)
                .tools(tools)
                .build();
    }
    
    @Override
    public CustomerState process(CustomerState state) {
        // Pass state to tools so they can update it
        tools.setState(state);
        
        String query = state.getCurrentQuery();
        String conversation = String.join("\n", state.getConversationHistory());
        
        // Add customer profile for financial context
        CustomerProfile profile = state.getCustomerProfile();
        if (profile != null) {
            conversation += "\n\nCustomer Financial Profile:";
            conversation += "\n- Budget: $" + profile.budgetMin() + "-$" + profile.budgetMax();
        }
        
        CustomerRequirements requirements = state.getCustomerRequirements();
        if (requirements != null) {
            conversation += "\n- Credit score: " + requirements.creditScore();
            conversation += "\n- Monthly budget estimate: $" + (requirements.budget() / 60); // Assuming 60-month loan
        }
        
        // Add selected vehicle context if available
        VehicleInfo selectedVehicle = state.getSelectedVehicle();
        if (selectedVehicle != null) {
            conversation += "\n\nSelected Vehicle: " + selectedVehicle.make().getDisplayName() + 
                          " " + selectedVehicle.model() + " - Price: $" + String.format("%,.0f", selectedVehicle.price());
        }
        
        // Add recommended vehicles if no vehicle selected yet
        if (selectedVehicle == null) {
            List<VehicleInfo> recommendedVehicles = state.getRecommendedVehicles();
            if (recommendedVehicles != null && !recommendedVehicles.isEmpty()) {
                conversation += "\n\nRecommended Vehicles:";
                for (VehicleInfo vehicle : recommendedVehicles) {
                    conversation += "\n- " + vehicle.make().getDisplayName() + " " + vehicle.model() + 
                                  " ($" + String.format("%,.0f", vehicle.price()) + ")";
                }
            }
        }
        
        // Add any existing financing options
        List<FinancingOption> existingOptions = state.getFinancingOptions();
        if (existingOptions != null && !existingOptions.isEmpty()) {
            conversation += "\n\nPreviously calculated financing options available.";
        }
        
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