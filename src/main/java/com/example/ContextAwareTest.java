package com.example;

import com.example.agents.langgraph.*;
import com.example.agents.CommonRequirements.*;
import java.util.Arrays;

/**
 * Quick test of context-aware routing
 */
public class ContextAwareTest {
    
    public static void main(String[] args) {
        System.out.println("=== Context-Aware Routing Test ===\n");
        
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Error: OPENAI_API_KEY environment variable not set");
            System.err.println("Please set your OpenAI API key:");
            System.err.println("  export OPENAI_API_KEY=your-api-key");
            return;
        }
        
        // Create agent with real OpenAI model
        GMVehicleGraphAgent agent = new GMVehicleGraphAgent();
        CustomerState state = new CustomerState();
        
        // Test 1: Technical question without profile
        System.out.println("📋 Test 1: Technical question WITHOUT customer profile");
        System.out.println("Query: \"What's the towing capacity of the Silverado?\"");
        String response1 = agent.processQuery("What's the towing capacity of the Silverado?", state);
        System.out.println("Response preview: " + response1.substring(0, Math.min(200, response1.length())) + "...\n");
        
        // Test 2: Add customer profile
        System.out.println("📋 Test 2: Adding customer profile to state");
        CustomerProfile profile = new CustomerProfile(
            4, // familySize
            "commute and weekend trips", // primaryUsage
            Arrays.asList("towing", "safety", "technology"), // preferences
            40000.0, // budgetMin
            55000.0, // budgetMax
            Arrays.asList(VehicleCategory.TRUCK), // preferredCategories
            true, // needsTowing
            false, // needsOffRoad
            "gasoline" // fuelPreference
        );
        state.setCustomerProfile(profile);
        System.out.println("Profile set: Family of 4, needs towing, budget $40-55k\n");
        
        // Test 3: Same technical question with profile
        System.out.println("📋 Test 3: Same technical question WITH customer profile");
        System.out.println("Query: \"What's the towing capacity of the Silverado?\"");
        String response2 = agent.processQuery("What's the towing capacity of the Silverado?", state);
        System.out.println("Response preview: " + response2.substring(0, Math.min(200, response2.length())) + "...\n");
        
        // Test 4: Financial question
        System.out.println("📋 Test 4: Financial question");
        System.out.println("Query: \"What would my monthly payment be for a Silverado?\"");
        String response3 = agent.processQuery("What would my monthly payment be for a Silverado?", state);
        System.out.println("Response preview: " + response3.substring(0, Math.min(200, response3.length())) + "...\n");
        
        // Show routing history
        System.out.println("📜 Routing History:");
        state.getConversationHistory().stream()
            .filter(msg -> msg.startsWith("Router:"))
            .forEach(msg -> System.out.println("  " + msg));
    }
}