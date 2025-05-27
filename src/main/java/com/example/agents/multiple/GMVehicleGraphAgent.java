package com.example.agents.multiple;

import dev.langchain4j.model.chat.ChatModel;

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
        this(ModelProvider.getDefaultModel());
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
        IntentClassifierAgent.IntentClassification classification = router.classifyIntentWithReason(state);
        String nextAgentName = classification.agent();
        String reason = classification.reason();
        
        // Execute the appropriate agent
        String response;
        switch (nextAgentName) {
            case "CUSTOMER_PROFILER":
                System.out.println("➡️  Agent: Customer Profiler (" + reason + ")");
                response = customerProfiler.execute(state, userQuery);
                break;
            case "TECHNICAL_EXPERT":
                System.out.println("➡️  Agent: Technical Expert (" + reason + ")");
                response = technicalExpert.execute(state, userQuery);
                break;
            case "FINANCIAL_ADVISOR":
                System.out.println("➡️  Agent: Financial Advisor (" + reason + ")");
                response = financialAdvisor.execute(state, userQuery);
                break;
            case "AVAILABILITY_COORDINATOR":
                System.out.println("➡️  Agent: Availability Coordinator (" + reason + ")");
                response = availabilityCoordinator.execute(state, userQuery);
                break;
            case "NEGOTIATION_COACH":
                System.out.println("➡️  Agent: Negotiation Coach (" + reason + ")");
                response = negotiationCoach.execute(state, userQuery);
                break;
            case "EV_SPECIALIST":
                System.out.println("➡️  Agent: EV Specialist (" + reason + ")");
                response = evSpecialist.execute(state, userQuery);
                break;
            default:
                System.out.println("➡️  Agent: Technical Expert (default - " + reason + ")");
                response = technicalExpert.execute(state, userQuery);
                break;
        }

        state.printMessages();
        
        return response;
    }
    
    /**
     * Create a new customer state
     */
    public CustomerState createNewState() {
        return new CustomerState();
    }
}