package com.example.agents.multiple;

/**
 * Base class for logging tool calls in agents
 */
public class BaseToolLogger {
    
    protected void logToolCall(String toolName, Object... params) {
        StringBuilder log = new StringBuilder("    🔧 Tool: ").append(toolName).append("(");
        for (int i = 0; i < params.length; i += 2) {
            if (i > 0) log.append(", ");
            log.append(params[i]).append("=").append(params[i + 1]);
        }
        log.append(")");
        System.out.println(log);
    }
}