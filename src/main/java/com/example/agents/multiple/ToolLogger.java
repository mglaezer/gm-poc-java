package com.example.agents.multiple;

/**
 * Logger for tool calls in agents using delegation pattern
 */
public class ToolLogger {

    public static void logToolCall(String toolName, Object... params) {
        StringBuilder log = new StringBuilder("    🔧 Tool: ").append(toolName).append("(");
        for (int i = 0; i < params.length; i += 2) {
            if (i > 0) log.append(", ");
            log.append(params[i]).append("=").append(params[i + 1]);
        }
        log.append(")");
        System.out.println(log);
    }
}
