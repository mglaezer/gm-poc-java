package com.example.agents.langgraph;

/**
 * Base interface for all agent nodes in the graph.
 * Each agent processes the state and returns an updated state.
 */
public interface AgentNode {
    /**
     * Process the current state and return updated state
     * @param state Current customer state
     * @return Updated state after agent processing
     */
    CustomerState process(CustomerState state);
    
    /**
     * Get the name of this agent node
     * @return Agent name
     */
    String getName();
}