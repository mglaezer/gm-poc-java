package com.example.agents.multiple;

import com.example.llmtoolkit.core.annotations.Cue;

/**
 * Structured response from agents that includes psychological state analysis
 */
public record AgentResponse(
        @Cue("The main response to the customer's query") String response,
        @Cue(
                        "Brief assessment of customer's current psychological state (e.g., anxious, confused, excited, overwhelmed, decisive)"
                                + "analyze and summarize the psychological state of the customer. Indicate their overall sentiment, any notable emotions, and possible personality traits (using the Big Five framework if possible)")
                String overallCustomerPsychologicalState,
        @Cue("Brief description of How the response accommodates the customer's psychological state")
                String howAnswerAccommodatesThat) {}
