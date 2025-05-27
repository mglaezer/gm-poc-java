package com.example.agents.langgraph;

import dev.langchain4j.data.message.*;
import java.util.*;

/**
 * CustomerState that maintains conversation history as list of ChatMessage objects.
 * All agent interactions, tool results, and user responses are stored as properly typed messages.
 */
public class CustomerState {
    private final List<ChatMessage> messages;
    
    public CustomerState() {
        this.messages = new ArrayList<>();
    }
    
    public List<ChatMessage> getMessages() {
        return new ArrayList<>(messages);
    }
    
    public void addMessage(ChatMessage message) {
        messages.add(message);
    }
    
    public void addUserMessage(String content) {
        messages.add(UserMessage.from(content));
    }
    
    public void addAiMessage(String content) {
        messages.add(AiMessage.from(content));
    }
    
    public void addSystemMessage(String content) {
        messages.add(SystemMessage.from(content));
    }
    
    public ChatMessage getLastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }
    
    public String getLastMessageText() {
        ChatMessage lastMessage = getLastMessage();
        if (lastMessage == null) {
            return "";
        }
        
        if (lastMessage instanceof UserMessage) {
            return ((UserMessage) lastMessage).singleText();
        } else if (lastMessage instanceof AiMessage) {
            return ((AiMessage) lastMessage).text();
        } else if (lastMessage instanceof SystemMessage) {
            return ((SystemMessage) lastMessage).text();
        } else if (lastMessage instanceof ToolExecutionResultMessage) {
            return ((ToolExecutionResultMessage) lastMessage).text();
        }
        return "";
    }
    
    public void logAgentAction(String agentName, String action, String details) {
        String entry = String.format("[%s] %s: %s", agentName, action, details);
        addAiMessage(entry);
    }
    
    public void logToolCall(String agentName, String toolName, String parameters, String result) {
        String entry = String.format("[%s] Tool %s(%s) -> %s", agentName, toolName, parameters, result);
        addAiMessage(entry);
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
    
    public List<ChatMessage> getMessagesOfType(Class<? extends ChatMessage> messageType) {
        return messages.stream()
                .filter(messageType::isInstance)
                .toList();
    }
    
    public List<UserMessage> getUserMessages() {
        return messages.stream()
                .filter(UserMessage.class::isInstance)
                .map(UserMessage.class::cast)
                .toList();
    }
    
    public List<AiMessage> getAiMessages() {
        return messages.stream()
                .filter(AiMessage.class::isInstance)
                .map(AiMessage.class::cast)
                .toList();
    }
}