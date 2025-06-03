package com.example.agents.multiple;

import com.example.llmtoolkit.core.JacksonSourceResponseStructuringStrategy;
import com.example.llmtoolkit.core.JteTemplateProcessor;
import com.example.llmtoolkit.core.TemplatedLLMServiceFactory;
import com.example.llmtoolkit.core.annotations.Cue;
import com.example.llmtoolkit.core.annotations.PT;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Intent Classifier Agent - Routes user queries to appropriate expert agents
 */
public class IntentClassifierAgent {

    public record IntentClassification(
            @Cue("Agent name all caps including underscores") String agent,
            @Cue("Very short, but informative reason") String reasonForChoosing) {}

    interface IntentClassifierStructured {
        @PT(templatePath = "classify_intent.jte")
        IntentClassification classifyIntent();
    }

    private final IntentClassifierStructured classifier;
    private final ConversationState conversationState;

    public IntentClassifierAgent(ChatModel model, ConversationState conversationState) {
        this.conversationState = conversationState;
        this.classifier = TemplatedLLMServiceFactory.builder()
                .serviceStrategy(new JacksonSourceResponseStructuringStrategy())
                .model(model)
                .templateProcessor(JteTemplateProcessor.create())
                .aiServiceCustomizer(aiServices -> aiServices.chatMemory(conversationState.getChatMemory()))
                .build()
                .create(IntentClassifierStructured.class);
    }

    public IntentClassification classifyIntentWithReason(String userMessage) {
        try {
            conversationState.getChatMemory().add(UserMessage.from(userMessage));
            return classifier.classifyIntent();
        } catch (Exception e) {
            System.err.println("Error with structured output: " + e.getMessage());
            return new IntentClassification("TECHNICAL_EXPERT", "Classification error occurred");
        }
    }
}
