package com.example.agents.langgraph;

import com.example.agents.CommonRequirements.*;
import com.example.agents.ToolsImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.util.List;

/**
 * Customer Profiler Agent - Understands customer needs and builds profiles
 */
public class CustomerProfilerAgent implements AgentNode {
    
    static class ProfilerTools extends BaseToolLogger {
        private final ToolsImpl tools = new ToolsImpl();
        private CustomerState state;
        
        public void setState(CustomerState state) {
            this.state = state;
        }
        
        @Tool("Analyze customer needs based on family size, usage, and preferences")
        public CustomerProfile analyzeNeeds(
                @P("Family size") int familySize,
                @P("Primary usage (commute, family, adventure)") String primaryUsage,
                @P("List of preferences") List<String> preferences) {
            logToolCall("analyzeNeeds", "familySize", familySize, "primaryUsage", primaryUsage, "preferences", preferences);
            CustomerProfile profile = tools.analyzeCustomerNeeds(familySize, primaryUsage, preferences);
            if (state != null) {
                state.setCustomerProfile(profile);
            }
            return profile;
        }
        
        @Tool("Build complete customer profile from requirements")
        public CustomerProfile buildProfile(
                @P("Family size") int familySize,
                @P("Daily commute description") String dailyCommute,
                @P("Weekend usage") String weekendUsage,
                @P("Must have features") List<String> mustHaveFeatures,
                @P("Budget") double budget,
                @P("Credit score (excellent, good, fair, poor)") String creditScore) {
            logToolCall("buildProfile", "familySize", familySize, "budget", budget, "creditScore", creditScore);
            CustomerRequirements req = new CustomerRequirements(
                familySize, dailyCommute, weekendUsage, mustHaveFeatures, budget, creditScore
            );
            CustomerProfile profile = tools.buildCustomerProfile(req);
            if (state != null) {
                state.setCustomerProfile(profile);
                state.setCustomerRequirements(req);
            }
            return profile;
        }
        
        @Tool("Suggest vehicle categories based on customer profile")
        public List<VehicleCategory> suggestCategories(
                @P("Customer profile") CustomerProfile profile) {
            logToolCall("suggestCategories", "profile", profile.familySize() + " family, $" + profile.budgetMin() + "-$" + profile.budgetMax());
            List<VehicleCategory> categories = tools.suggestVehicleCategories(profile);
            if (state != null) {
                state.set("suggestedCategories", categories);
            }
            return categories;
        }
    }
    
    interface ProfilerAssistant {
        @SystemMessage("""
            You are a customer profiling specialist for GM vehicles.
            Your job is to understand customer needs and create detailed profiles.
            
            IMPORTANT: Check if a customer profile already exists before asking questions.
            If profile information is already available, acknowledge it and only ask for missing details.
            
            Information to gather (if not already provided):
            - Family size and composition
            - Daily commute and weekend usage
            - Must-have features
            - Budget constraints
            - Lifestyle and activities
            - Towing or cargo needs
            - Fuel preferences
            
            Use the tools to analyze needs and build profiles.
            Be friendly and conversational while gathering information.
            Always store the profile for other agents to use.
            """)
        String profileCustomer(@UserMessage String conversation);
    }
    
    private final ProfilerAssistant assistant;
    private final ProfilerTools tools;
    
    public CustomerProfilerAgent(ChatLanguageModel model) {
        this.tools = new ProfilerTools();
        this.assistant = AiServices.builder(ProfilerAssistant.class)
                .chatLanguageModel(model)
                .tools(tools)
                .build();
    }
    
    @Override
    public CustomerState process(CustomerState state) {
        // Pass state to tools so they can update it
        tools.setState(state);
        
        String query = state.getCurrentQuery();
        String conversation = String.join("\n", state.getConversationHistory());
        
        // Add existing profile information to conversation context
        CustomerProfile existingProfile = state.getCustomerProfile();
        if (existingProfile != null) {
            conversation += "\n\nExisting Customer Profile:";
            conversation += "\n- Family size: " + existingProfile.familySize();
            conversation += "\n- Primary usage: " + existingProfile.primaryUsage();
            conversation += "\n- Budget: $" + existingProfile.budgetMin() + "-$" + existingProfile.budgetMax();
            conversation += "\n- Preferred categories: " + existingProfile.preferredCategories();
            conversation += "\n- Fuel preference: " + existingProfile.fuelPreference();
            if (existingProfile.needsTowing()) {
                conversation += "\n- Needs towing capability";
            }
        }
        
        CustomerRequirements existingReq = state.getCustomerRequirements();
        if (existingReq != null) {
            conversation += "\n\nCustomer Requirements:";
            conversation += "\n- Daily commute: " + existingReq.dailyCommute();
            conversation += "\n- Weekend usage: " + existingReq.weekendUsage();
            conversation += "\n- Credit score: " + existingReq.creditScore();
        }
        
        conversation += "\nUser: " + query;
        
        String response = assistant.profileCustomer(conversation);
        state.addToConversationHistory("Customer Profiler: " + response);
        
        // The assistant should have used tools to set the profile
        // Check if we now have a profile
        if (state.getCustomerProfile() != null) {
            state.setNextAgent("TECHNICAL_EXPERT");
        }
        
        return state;
    }
    
    @Override
    public String getName() {
        return "CUSTOMER_PROFILER";
    }
}