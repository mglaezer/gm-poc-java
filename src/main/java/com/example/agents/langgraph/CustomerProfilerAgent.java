package com.example.agents.langgraph;

import com.example.agents.CommonRequirements.*;
import com.example.agents.ToolsImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

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
        
        @Tool("Create a basic profile with minimal information")
        public CustomerProfile createQuickProfile(
                @P("Budget range or 0 if not specified") double budget,
                @P("Vehicle size preference or 'any'") String vehicleSize) {
            logToolCall("createQuickProfile", "budget", budget, "vehicleSize", vehicleSize);
            
            // Determine budget range
            double budgetMin = budget > 0 ? budget * 0.8 : 25000;
            double budgetMax = budget > 0 ? budget * 1.2 : 80000;
            
            // Determine categories based on size preference
            List<VehicleCategory> categories = new ArrayList<>();
            if (vehicleSize.toLowerCase().contains("small") || vehicleSize.toLowerCase().contains("compact")) {
                categories.add(VehicleCategory.SEDAN);
            } else if (vehicleSize.toLowerCase().contains("large") || vehicleSize.toLowerCase().contains("full")) {
                categories.add(VehicleCategory.TRUCK);
                categories.add(VehicleCategory.SUV);
            } else if (vehicleSize.toLowerCase().contains("suv")) {
                categories.add(VehicleCategory.SUV);
            } else if (vehicleSize.toLowerCase().contains("truck")) {
                categories.add(VehicleCategory.TRUCK);
            } else {
                // Default to showing variety
                categories.add(VehicleCategory.SUV);
                categories.add(VehicleCategory.TRUCK);
                categories.add(VehicleCategory.SEDAN);
            }
            
            CustomerProfile profile = new CustomerProfile(
                0, // unknown family size
                "general", // general usage
                Arrays.asList("value", "reliability"), // default preferences
                budgetMin,
                budgetMax,
                categories,
                false, // no towing by default
                false, // no off-road by default
                "gasoline" // default fuel preference
            );
            
            if (state != null) {
                state.setCustomerProfile(profile);
            }
            return profile;
        }
    }
    
    interface ProfilerAssistant {
        @SystemMessage("""
            You are a customer profiling specialist for GM vehicles.
            Your job is to understand customer needs with MINIMAL questions.
            
            IMPORTANT RULES:
            1. If a customer profile already exists, DO NOT ask any questions - just confirm and move on
            2. Ask MAXIMUM 2 questions at a time
            3. Focus on the MOST essential information first:
               - What's your budget range?
               - What size vehicle do you need (compact, midsize, full-size)?
            4. Always offer: "Or if you'd prefer, I can show you some popular options right away"
            5. If the user seems reluctant or gives short answers, immediately offer to skip profiling
            6. Extract information from context clues rather than asking directly when possible
            
            Essential information only:
            - Budget range (most important)
            - Vehicle size preference
            - Primary use (only if not obvious)
            
            Use the tools to create a basic profile even with minimal information.
            Be concise and respectful of the user's time.
            NEVER ask more than 2 questions in one response.
            """)
        String profileCustomer(@UserMessage String conversation);
    }
    
    private final ProfilerAssistant assistant;
    private final ProfilerTools tools;
    
    public CustomerProfilerAgent(ChatModel model) {
        this.tools = new ProfilerTools();
        this.assistant = AiServices.builder(ProfilerAssistant.class)
                .chatModel(model)
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