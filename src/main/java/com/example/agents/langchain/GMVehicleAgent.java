package com.example.agents.langchain;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;

/**
 * GM Vehicle Selection Agent using LangChain4j.
 * This agent helps users find and learn about General Motors vehicles.
 */
public class GMVehicleAgent {

    interface VehicleAssistant {
        @SystemMessage("""
                You are a helpful General Motors vehicle specialist assistant.
                You help customers find the right GM vehicle for their needs and answer questions about vehicle specifications.
                You have access to tools to search vehicles by make/model, compare vehicles, check availability, and calculate financing.
                
                When users ask about a vehicle, provide comprehensive information including:
                - Basic specs (price, year, category)
                - Performance (engine, horsepower, transmission)
                - Fuel economy (MPG city/highway)
                - Dimensions and capacity
                - Key features and technology
                - Safety features
                
                Be friendly, professional, and informative, but also humorous! If a user asks about a vehicle that doesn't exist in the database,
                politely let them know and suggest similar alternatives.
                """)
        String chat(String userMessage);
    }

    private final ChatLanguageModel model;
    private final ChatMemory chatMemory;
    private final VehicleAssistant assistant;
    private final VehicleSearchTools tools;


    public GMVehicleAgent(ChatLanguageModel model) {
        this.model = model;
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(100);
        this.tools = new VehicleSearchTools();

        this.assistant = AiServices.builder(VehicleAssistant.class)
                .chatLanguageModel(model)
                .chatMemory(chatMemory)
                .tools(tools)
                .build();
    }

    public String chat(String userMessage) {
        return assistant.chat(userMessage);
    }

    public void startConversation() {
        System.out.println("GM Vehicle Assistant: Hello! I'm here to help you find the perfect General Motors vehicle. " +
                           "You can ask me about any GM vehicle by make and model, or I can help you search for vehicles " +
                           "based on your preferences. What would you like to know?");
    }
}