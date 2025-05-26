/**
 * LangGraph-style multi-agent implementation using LangChain4j.
 * 
 * This package implements a graph-based multi-agent system where:
 * - Different specialized agents handle specific aspects of car selection
 * - An intent classifier routes queries to appropriate experts
 * - State is maintained and passed between agents
 * - Each agent has access to specialized tools
 * 
 * Agents included:
 * - IntentClassifierAgent: Routes queries to appropriate experts
 * - CustomerProfilerAgent: Understands customer needs and preferences
 * - TechnicalExpertAgent: Provides detailed vehicle specifications
 * - FinancialAdvisorAgent: Handles financing and budgeting
 * - AvailabilityCoordinatorAgent: Manages inventory and test drives
 * - NegotiationCoachAgent: Helps with pricing strategy
 * - EVSpecialistAgent: Expert on electric vehicles
 * 
 * The graph structure allows for non-linear conversation flow where
 * customers can jump between different concerns naturally.
 */
package com.example.agents.langgraph;