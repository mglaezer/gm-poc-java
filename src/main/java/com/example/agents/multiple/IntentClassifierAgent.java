package com.example.agents.multiple;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import java.util.List;
import org.llmtoolkit.core.JacksonSourceResponseStructuringStrategy;
import org.llmtoolkit.core.JteTemplateProcessor;
import org.llmtoolkit.core.TemplatedLLMServiceFactory;
import org.llmtoolkit.core.annotations.Cue;
import org.llmtoolkit.core.annotations.PP;
import org.llmtoolkit.core.annotations.PT;

/**
 * Intent Classifier Agent - Routes user queries to appropriate expert agents
 */
public class IntentClassifierAgent {

    public record IntentClassification(
            @Cue("Agent name all caps including underscores") String agent, String reasonForChoosing) {}

    interface IntentClassifierStructured {
        @PT(templatePath = "classify_intent.jte")
        IntentClassification classifyIntent(@PP("messages") List<ChatMessage> messages);
    }

    private final IntentClassifierStructured classifier;

    public IntentClassifierAgent(ChatModel model) {
        this.classifier = TemplatedLLMServiceFactory.builder()
                .serviceStrategy(new JacksonSourceResponseStructuringStrategy())
                .model(model)
                .templateProcessor(JteTemplateProcessor.create())
                .build()
                .create(IntentClassifierStructured.class);
    }

    public IntentClassification classifyIntentWithReason(CustomerState state) {
        var lastUserMessage = state.getLastUserMessage();

        if (lastUserMessage == null) {
            return new IntentClassification("TECHNICAL_EXPERT", "No user query found");
        }

        try {
            IntentClassification result = classifier.classifyIntent(state.getMessages());

            state.logAgentAction(
                    "INTENT_CLASSIFIER",
                    "Routing",
                    "Directing to " + result.agent() + " - " + result.reasonForChoosing());

            return result;

        } catch (Exception e) {
            System.err.println("Error with structured output: " + e.getMessage());
            return new IntentClassification("TECHNICAL_EXPERT", "Classification error occurred");
        }
    }
}
