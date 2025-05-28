package com.example.agents.multiple;

import dev.langchain4j.data.message.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CustomerState that maintains conversation history as list of ChatMessage objects.
 * All agent interactions, tool results, and user responses are stored as properly typed messages.
 */
public class CustomerState {
    private final List<ChatMessage> messages;

    public CustomerState() {
        this.messages = new ArrayList<>();
    }

    public void addUserMessage(String content) {
        messages.add(UserMessage.from(content));
    }

    public void addAiMessage(String content) {
        messages.add(AiMessage.from(content));
    }

    public void addToolExecutionResult(String toolName, String executionResult) {
        messages.add(ToolExecutionResultMessage.from(null, toolName, executionResult));
    }

    public void logAgentAction(String agentName, String action, String details) {
        String entry = String.format("[%s] %s: %s", agentName, action, details);
        addAiMessage(entry);
    }

    public void addToolResult(String toolName, String result) {
        addToolExecutionResult(toolName, result);
    }

    public String getConversationContext() {
        if (messages.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        for (ChatMessage message : messages) {
            String prefix = "";
            if (message instanceof UserMessage) {
                prefix = "User: ";
            } else if (message instanceof AiMessage) {
                prefix = "Assistant: ";
            } else if (message instanceof SystemMessage) {
                prefix = "System: ";
            } else if (message instanceof ToolExecutionResultMessage) {
                prefix = "Tool Result: ";
            }
            String text = "";
            if (message instanceof UserMessage) {
                text = ((UserMessage) message).singleText();
            } else if (message instanceof AiMessage) {
                text = ((AiMessage) message).text();
            } else if (message instanceof SystemMessage) {
                text = ((SystemMessage) message).text();
            } else if (message instanceof ToolExecutionResultMessage) {
                text = ((ToolExecutionResultMessage) message).text();
            }
            context.append(prefix).append(text).append("\n");
        }
        return context.toString().trim();
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public UserMessage getLastUserMessage() {
        List<UserMessage> userMessages = messages.stream()
                .filter(UserMessage.class::isInstance)
                .map(UserMessage.class::cast)
                .toList();

        return userMessages.isEmpty() ? null : userMessages.getLast();
    }

    public void printMessages() {
        /*
                System.out.println("\n=== Conversation History ===");
                System.out.println(getConversationContext());
                System.out.println();
        */
    }
}
