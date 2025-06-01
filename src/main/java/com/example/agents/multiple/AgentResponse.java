package com.example.agents.multiple;

import org.llmtoolkit.core.annotations.Cue;

/**
 * Structured response from agents that includes psychological state analysis
 */
public record AgentResponse(
        @Cue("The main response to the customer's query") String response,
        @Cue(
                        "Brief assessment of customer's current psychological state (e.g., anxious, confused, excited, overwhelmed, decisive)")
                String overallCustomerPsychologicalState,
        @Cue("How the response accommodates the customer's psychological state") String howAnswerAccommodatesThat) {}
