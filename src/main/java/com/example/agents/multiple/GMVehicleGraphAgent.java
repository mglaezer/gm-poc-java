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
    private final ConversationState conversationState;

    public GMVehicleGraphAgent() {
        this(ModelProvider.getDefaultModel());
    }

    public GMVehicleGraphAgent(ChatModel model) {
        // Create shared conversation state
        this.conversationState = new ConversationState();

        // Initialize all agents - only router and technicalExpert use ConversationState for now
        this.router = new IntentClassifierAgent(model, conversationState);
        this.customerProfiler = new CustomerProfilerAgent(model, conversationState);
        this.technicalExpert = new TechnicalExpertAgent(model, conversationState);
        this.financialAdvisor = new FinancialAdvisorAgent(model, conversationState);
        this.availabilityCoordinator = new AvailabilityCoordinatorAgent(model, conversationState);
        this.negotiationCoach = new NegotiationCoachAgent(model, conversationState);
        this.evSpecialist = new EVSpecialistAgent(model, conversationState);
    }

    /**
     * Process a user query through the graph
     */
    public String processQuery(String userQuery) {
        // Route through intent classifier
        System.out.println("\n🔄 Routing: Intent Classifier analyzing query...");
        IntentClassifierAgent.IntentClassification classification = router.classifyIntentWithReason(userQuery);
        String nextAgentName = classification.agent();
        String reason = classification.reasonForChoosing();

        // Execute the appropriate agent
        String response;
        switch (nextAgentName) {
            case "CUSTOMER_PROFILER":
                System.out.println("➡️  Agent: Customer Profiler (" + reason + ")");
                response = customerProfiler.execute(userQuery);
                break;
            case "TECHNICAL_EXPERT":
                System.out.println("➡️  Agent: Technical Expert (" + reason + ")");
                response = technicalExpert.execute(userQuery);
                break;
            case "FINANCIAL_ADVISOR":
                System.out.println("➡️  Agent: Financial Advisor (" + reason + ")");
                response = financialAdvisor.execute(userQuery);
                break;
            case "AVAILABILITY_COORDINATOR":
                System.out.println("➡️  Agent: Availability Coordinator (" + reason + ")");
                response = availabilityCoordinator.execute(userQuery);
                break;
            case "NEGOTIATION_COACH":
                System.out.println("➡️  Agent: Negotiation Coach (" + reason + ")");
                response = negotiationCoach.execute(userQuery);
                break;
            case "EV_SPECIALIST":
                System.out.println("➡️  Agent: EV Specialist (" + reason + ")");
                response = evSpecialist.execute(userQuery);
                break;
            default:
                System.out.println("➡️  Agent: Technical Expert (default - " + reason + ")");
                response = technicalExpert.execute(userQuery);
                break;
        }

        return response;
    }

    /**
     * Get the conversation state
     */
    public ConversationState getConversationState() {
        return conversationState;
    }
}
