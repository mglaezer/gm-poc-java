@param java.util.List<dev.langchain4j.data.message.ChatMessage> messages

@for(var message : messages)
    @if(message instanceof dev.langchain4j.data.message.UserMessage)
        User: ${((dev.langchain4j.data.message.UserMessage) message).singleText()}
    @elseif(message instanceof dev.langchain4j.data.message.AiMessage)
        Assistant: ${((dev.langchain4j.data.message.AiMessage) message).text()}
    @elseif(message instanceof dev.langchain4j.data.message.SystemMessage)
        System: ${((dev.langchain4j.data.message.SystemMessage) message).text()}
    @elseif(message instanceof dev.langchain4j.data.message.ToolExecutionResultMessage)
        Tool Result: ${((dev.langchain4j.data.message.ToolExecutionResultMessage) message).text()}
    @else
        @throw new IllegalArgumentException("Unknown message type: " + message.getClass().getName())
    @endif
@endfor