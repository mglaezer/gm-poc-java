package com.example.agents.langgraph;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.Scanner;

/**
 * Demo application for the GM Vehicle Graph Agent
 */
public class GMVehicleGraphDemo {
    
    public static void main(String[] args) {
        System.out.println("=== GM Vehicle Selection Graph Agent Demo ===");
        System.out.println("Using multi-agent system with specialized experts\n");
        
        ChatModel model;
        String apiKey = System.getenv("OPENAI_API_KEY");
        
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Error: OPENAI_API_KEY environment variable not set");
            System.err.println("Please set your OpenAI API key:");
            System.err.println("  export OPENAI_API_KEY=your-api-key");
            return;
        }
        
        model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4.1")
                .temperature(0.7)
                .build();
        
        GMVehicleGraphAgent agent = new GMVehicleGraphAgent(model);
        CustomerState state = agent.startNewConversation();
        
        // Print initial greeting
        System.out.println(state.getConversationHistory().get(0).replace("GM Vehicle Assistant: ", ""));
        
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.print("\nYou: ");
            String userInput = scanner.nextLine();
            
            if (userInput.equalsIgnoreCase("exit") || userInput.equalsIgnoreCase("quit")) {
                System.out.println("\nThank you for using GM Vehicle Selection Agent. Goodbye!");
                break;
            }
            
            if (userInput.equalsIgnoreCase("history")) {
                System.out.println("\n=== Conversation History ===");
                for (String msg : state.getConversationHistory()) {
                    System.out.println(msg);
                }
                System.out.println("=========================");
                continue;
            }
            
            try {
                // Process query with single agent routing (most queries only need one step)
                String response = agent.processQuery(userInput, state);
                
                // Extract just the agent response part
                String[] parts = response.split(": ", 2);
                if (parts.length > 1) {
                    System.out.println("\n" + parts[0] + ":");
                    System.out.println(parts[1]);
                } else {
                    System.out.println("\n" + response);
                }
                
            } catch (Exception e) {
                System.err.println("\nError: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        scanner.close();
    }
    
}