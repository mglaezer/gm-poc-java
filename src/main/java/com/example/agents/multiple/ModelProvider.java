package com.example.agents.multiple;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 * Centralized model provider for the langgraph package
 */
public class ModelProvider {
    
    private static final String DEFAULT_MODEL = "gpt-4.1";
    private static final double DEFAULT_TEMPERATURE = 0.7;
    
    /**
     * Get the default chat model using environment variables
     */
    public static ChatModel getDefaultModel() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable not set");
        }
        
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(DEFAULT_MODEL)
                .temperature(DEFAULT_TEMPERATURE)
                .build();
    }
    

}
