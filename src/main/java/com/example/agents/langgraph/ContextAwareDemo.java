package com.example.agents.langgraph;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * Demo showing how agents pass context between nodes
 */
public class ContextAwareDemo {
    
    public static void main(String[] args) {
        // Check for API key
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("⚠️  Please set OPENAI_API_KEY environment variable");
            System.out.println("   Example: export OPENAI_API_KEY='your-api-key'");
            return;
        }
        
        // Create model
        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4")
                .temperature(0.7)
                .build();
        
        // Create the graph agent
        GMVehicleGraphAgent agent = new GMVehicleGraphAgent(model);
        
        // Start a new conversation
        CustomerState state = agent.startNewConversation();
        
        System.out.println("=== Context-Aware Multi-Agent Demo ===\n");
        System.out.println("This demo shows how agents share context to avoid redundant questions.\n");
        
        // First interaction - Customer Profiler gathers information
        System.out.println("1️⃣  First, let's build a customer profile...\n");
        String query1 = "I'm looking for a family vehicle. We're a family of 5 with three kids. " +
                       "We need something reliable for daily commutes and weekend trips. " +
                       "Our budget is around $40,000 and we have good credit.";
        
        String response1 = agent.processQuery(query1, state);
        System.out.println("\n" + response1);
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // Second interaction - Technical Expert uses the profile
        System.out.println("2️⃣  Now asking for recommendations (Technical Expert should use existing profile)...\n");
        String query2 = "Based on what I've told you, what vehicles would you recommend?";
        
        String response2 = agent.processQuery(query2, state);
        System.out.println("\n" + response2);
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // Third interaction - Financial Advisor uses both profile and recommendations
        System.out.println("3️⃣  Asking about financing (Financial Advisor should use profile + recommendations)...\n");
        String query3 = "What would the monthly payments be for your top recommendation?";
        
        String response3 = agent.processQuery(query3, state);
        System.out.println("\n" + response3);
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // Show the state that's been built up
        System.out.println("📊 Context accumulated in CustomerState:");
        if (state.getCustomerProfile() != null) {
            var profile = state.getCustomerProfile();
            System.out.println("   ✓ Customer Profile:");
            System.out.println("     - Family size: " + profile.familySize());
            System.out.println("     - Budget: $" + profile.budgetMin() + "-$" + profile.budgetMax());
            System.out.println("     - Preferred categories: " + profile.preferredCategories());
        }
        
        if (state.getCustomerRequirements() != null) {
            var req = state.getCustomerRequirements();
            System.out.println("   ✓ Customer Requirements:");
            System.out.println("     - Credit score: " + req.creditScore());
            System.out.println("     - Daily commute: " + req.dailyCommute());
        }
        
        if (!state.getRecommendedVehicles().isEmpty()) {
            System.out.println("   ✓ Recommended Vehicles: " + state.getRecommendedVehicles().size() + " vehicles");
        }
        
        if (state.getSelectedVehicle() != null) {
            var vehicle = state.getSelectedVehicle();
            System.out.println("   ✓ Selected Vehicle: " + vehicle.make().getDisplayName() + " " + vehicle.model());
        }
        
        if (!state.getFinancingOptions().isEmpty()) {
            System.out.println("   ✓ Financing Options: " + state.getFinancingOptions().size() + " options calculated");
        }
        
        System.out.println("\n✅ Notice how each agent built upon the previous agent's work!");
        System.out.println("   - Customer Profiler gathered initial requirements");
        System.out.println("   - Technical Expert used the profile to recommend vehicles");
        System.out.println("   - Financial Advisor used both profile and recommendations");
    }
}