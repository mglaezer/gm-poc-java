package com.example.agents.langchain;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.Scanner;

/**
 * Demo application for the GM Vehicle Agent using LangChain4j.
 * This demonstrates how users can interact with the agent to find and learn about vehicles.
 */
public class GMVehicleAgentDemo {

    public static void main(String[] args) {
        System.out.println("=== GM Vehicle Selection Agent Demo ===");
        System.out.println("Initializing agent...\n");

        ChatLanguageModel model;
        String apiKey = System.getenv("OPENAI_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("Warning: OPENAI_API_KEY not found. Using mock responses for demo.");
        }
        model = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gpt-4")
                .temperature(0.7)
                .build();

        GMVehicleAgent agent = new GMVehicleAgent(model);
        agent.startConversation();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\nYou: ");
            String userInput = scanner.nextLine();

            if (userInput.equalsIgnoreCase("exit") || userInput.equalsIgnoreCase("quit")) {
                System.out.println("\nGM Vehicle Assistant: Thank you for using GM Vehicle Selection Agent. Goodbye!");
                break;
            }

            try {
                String response = agent.chat(userInput);
                System.out.println("\nGM Vehicle Assistant: " + response);
            } catch (Exception e) {
                System.err.println("\nError: " + e.getMessage());
                System.out.println("GM Vehicle Assistant: I apologize, but I encountered an error. Please try rephrasing your question.");
            }
        }

        scanner.close();
    }

}