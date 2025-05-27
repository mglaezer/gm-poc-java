package com.example.agents.langgraph;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.List;

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
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4.1")
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
        
        // Show the conversation history
        System.out.println("📊 Context accumulated in Conversation History:");
        List<String> history = state.getConversationHistory();
        System.out.println("   Total entries: " + history.size());
        System.out.println("\n   Recent activity:");
        
        // Show last 10 entries
        int start = Math.max(0, history.size() - 10);
        for (int i = start; i < history.size(); i++) {
            System.out.println("   " + history.get(i));
        }
        
        // Extract some insights from the conversation
        System.out.println("\n   Key information extracted:");
        List<String> vehicleMentions = state.getRecentVehiclesMentioned();
        if (!vehicleMentions.isEmpty()) {
            System.out.println("   ✓ Vehicle searches performed: " + vehicleMentions.size());
        }
        
        if (state.hasRecentFinancingDiscussion()) {
            System.out.println("   ✓ Financing has been discussed");
        }
        
        System.out.println("\n✅ Notice how each agent built upon the previous agent's work!");
        System.out.println("   - Customer Profiler gathered initial requirements");
        System.out.println("   - Technical Expert used the profile to recommend vehicles");
        System.out.println("   - Financial Advisor used both profile and recommendations");
    }
}