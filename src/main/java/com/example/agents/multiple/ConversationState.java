package com.example.agents.multiple;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

/**
 * Simple conversation state that holds ChatMemory for sharing across agents
 */
public class ConversationState {
    private final ChatMemory chatMemory;

    public ConversationState() {
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(100);
    }

    public ChatMemory getChatMemory() {
        return chatMemory;
    }
}
