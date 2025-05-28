package com.example.agents.multiple;

import dev.langchain4j.model.chat.ChatModel;
import java.util.Scanner;

/**
 * Demo application for the GM Vehicle Graph Agent
 */
public class GMVehicleGraphDemo {

    public static void main(String[] args) {
        System.out.println("=== GM Vehicle Selection Graph Agent Demo ===");
        System.out.println("Using multi-agent system with specialized experts\n");

        ChatModel model = ModelProvider.getDefaultModel();

        GMVehicleGraphAgent agent = new GMVehicleGraphAgent(model);
        CustomerState state = agent.createNewState();

        // Print initial greeting
        System.out.println(
                "Hello! I'm your GM Vehicle Assistant. I can help you find the perfect vehicle. What are you looking for today?");

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
                System.out.println(state.getConversationContext());
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
