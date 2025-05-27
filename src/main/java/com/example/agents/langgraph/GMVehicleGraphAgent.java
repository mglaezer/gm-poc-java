package com.example.agents.langgraph;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.HashMap;
import java.util.Map;

/**
 * Main GM Vehicle Graph Agent that orchestrates multiple specialized agents
 */
public class GMVehicleGraphAgent {
    
    private final IntentClassifierAgent router;
    private final CustomerProfilerAgent customerProfiler;
    private final TechnicalExpertAgent technicalExpert;
    private final FinancialAdvisorAgent financialAdvisor;
    private final AvailabilityCoordinatorAgent availabilityCoordinator;
    private final NegotiationCoachAgent negotiationCoach;
    private final EVSpecialistAgent evSpecialist;
    
    public GMVehicleGraphAgent() {
        this(OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4.1")
                .temperature(0.7)
                .build());
    }
    
    public GMVehicleGraphAgent(ChatModel model) {
        // Initialize all agents
        this.router = new IntentClassifierAgent(model);
        this.customerProfiler = new CustomerProfilerAgent(model);
        this.technicalExpert = new TechnicalExpertAgent(model);
        this.financialAdvisor = new FinancialAdvisorAgent(model);
        this.availabilityCoordinator = new AvailabilityCoordinatorAgent(model);
        this.negotiationCoach = new NegotiationCoachAgent(model);
        this.evSpecialist = new EVSpecialistAgent(model);
    }
    
    /**
     * Process a user query through the graph
     */
    public String processQuery(String userQuery, CustomerState state) {
        if (state == null) {
            state = new CustomerState();
        }
        
        // Add user query to messages
        state.addUserMessage(userQuery);
        
        // Route through intent classifier
        System.out.println("\n🔄 Routing: Intent Classifier analyzing query...");
        String nextAgentName = router.classifyIntent(state);
        
        // Execute the appropriate agent
        String response;
        switch (nextAgentName) {
            case "CUSTOMER_PROFILER":
                System.out.println("➡️  Agent: Customer Profiler");
                response = customerProfiler.execute(state, userQuery);
                break;
            case "TECHNICAL_EXPERT":
                System.out.println("➡️  Agent: Technical Expert");
                response = technicalExpert.execute(state, userQuery);
                break;
            case "FINANCIAL_ADVISOR":
                System.out.println("➡️  Agent: Financial Advisor");
                response = financialAdvisor.execute(state, userQuery);
                break;
            case "AVAILABILITY_COORDINATOR":
                System.out.println("➡️  Agent: Availability Coordinator");
                response = availabilityCoordinator.execute(state, userQuery);
                break;
            case "NEGOTIATION_COACH":
                System.out.println("➡️  Agent: Negotiation Coach");
                response = negotiationCoach.execute(state, userQuery);
                break;
            case "EV_SPECIALIST":
                System.out.println("➡️  Agent: EV Specialist");
                response = evSpecialist.execute(state, userQuery);
                break;
            default:
                System.out.println("➡️  Agent: Technical Expert (default)");
                response = technicalExpert.execute(state, userQuery);
                break;
        }
        
        return response;
    }
    
    /**
     * Create a new customer state
     */
    public CustomerState createNewState() {
        return new CustomerState();
    }
}