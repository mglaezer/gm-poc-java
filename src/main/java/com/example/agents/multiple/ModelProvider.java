package com.example.agents.multiple;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.llmtoolkit.util.Env;

/**
 * Centralized model provider for the langgraph package
 */
public class ModelProvider {

    public enum Model {
        GEMINI_25_FLASH,
        GPT_41,
        GROQ_LLAMA_3_3_70B
    }

    private static final double DEFAULT_TEMPERATURE = 0.7;

    public static ChatModel getDefaultModel() {
        return getModel(Model.GPT_41);
    }

    @SuppressWarnings("SameParameterValue")
    private static ChatModel getModel(Model model) {
        return switch (model) {
            case GEMINI_25_FLASH -> {
                String geminiApiKey = Env.getRequired("GEMINI_API_KEY");
                yield GoogleAiGeminiChatModel.builder()
                        .apiKey(geminiApiKey)
                        .modelName("gemini-2.5-flash-preview-05-20")
                        .temperature(DEFAULT_TEMPERATURE)
                        .build();
            }
            case GPT_41 -> {
                String openaiApiKey = Env.getRequired("OPENAI_API_KEY");
                yield OpenAiChatModel.builder()
                        .apiKey(openaiApiKey)
                        .modelName("gpt-4.1")
                        .temperature(DEFAULT_TEMPERATURE)
                        .build();
            }
            case GROQ_LLAMA_3_3_70B -> {
                String groqApiKey = Env.getRequired("GROQ_API_KEY");
                yield OpenAiChatModel.builder()
                        .apiKey(groqApiKey)
                        .modelName("llama-3.3-70b-versatile")
                        .baseUrl("https://api.groq.com/openai/v1")
                        .temperature(DEFAULT_TEMPERATURE)
                        .build();
            }
        };
    }
}
