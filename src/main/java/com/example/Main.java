package com.example;

import com.example.agents.langchain.GMVehicleAgentDemo;
import com.example.agents.langgraph.GMVehicleGraphDemo;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== GM POC Java - Agent Demonstrations ===\n");
        
        if (args.length == 0) {
            System.out.println("Please specify which agent to run:");
            System.out.println("  java -jar target/gm-poc-java-1.0-SNAPSHOT.jar langchain");
            System.out.println("  java -jar target/gm-poc-java-1.0-SNAPSHOT.jar langgraph");
            System.out.println("  java -jar target/gm-poc-java-1.0-SNAPSHOT.jar context-test");
            System.out.println("  java -jar target/gm-poc-java-1.0-SNAPSHOT.jar adk (coming soon)");
            return;
        }
        
        String agentType = args[0].toLowerCase();
        
        switch (agentType) {
            case "langchain":
                GMVehicleAgentDemo.main(new String[]{});
                break;
            case "langgraph":
                GMVehicleGraphDemo.main(new String[]{});
                break;
            case "context-test":
                ContextAwareTest.main(new String[]{});
                break;
            case "adk":
                System.out.println("ADK agent implementation coming soon!");
                break;
            default:
                System.out.println("Unknown agent type: " + agentType);
                System.out.println("Valid options: langchain, langgraph, context-test, adk");
        }
    }
}