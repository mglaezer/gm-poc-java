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
import java.util.stream.Collectors;
import com.example.agents.MockVehicleData;

/**
 * Customer Profiler Agent - Understands customer needs and builds profiles
 */
public class CustomerProfilerAgent {
    
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
            
            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: familySize=").append(familySize)
              .append(", primaryUsage=").append(primaryUsage)
              .append(", preferences=").append(preferences).append("\n");
            sb.append(String.format("Customer Profile Created:\n"));
            sb.append(String.format("Budget Range: $%,.0f - $%,.0f\n", profile.budgetMin(), profile.budgetMax()));
            sb.append(String.format("Preferred Categories: %s\n", 
                profile.preferredCategories().stream().map(VehicleCategory::getDisplayName).collect(Collectors.joining(", "))));
            sb.append(String.format("Fuel Preference: %s\n", profile.fuelPreference()));
            sb.append(String.format("Needs Towing: %s | Needs Off-Road: %s", 
                profile.needsTowing() ? "Yes" : "No", profile.needsOffRoad() ? "Yes" : "No"));
            state.addToolResult("analyzeNeeds", sb.toString());
            return profile;
        }
        
        @Tool("Build complete customer profile from requirements")
        public CustomerProfile buildProfile(
                @P("Family size") int familySize,
                @P("Daily commute description") String dailyCommute,
                @P("Weekend usage description") String weekendUsage,
                @P("Must-have features") List<String> mustHaveFeatures,
                @P("Budget") double budget,
                @P("Credit score (excellent, good, fair, poor)") String creditScore) {
            logToolCall("buildProfile", "familySize", familySize, "dailyCommute", dailyCommute, 
                      "weekendUsage", weekendUsage, "budget", budget);
            
            // Create preferences list combining all usage patterns
            List<String> preferences = new ArrayList<>();
            if (dailyCommute.toLowerCase().contains("highway") || dailyCommute.toLowerCase().contains("long")) {
                preferences.add("fuel efficiency");
                preferences.add("comfortable seats");
            }
            if (weekendUsage.toLowerCase().contains("camping") || weekendUsage.toLowerCase().contains("outdoor")) {
                preferences.add("cargo space");
                preferences.add("all-wheel drive");
            }
            if (mustHaveFeatures != null) {
                preferences.addAll(mustHaveFeatures);
            }
            
            String primaryUsage = dailyCommute + " / " + weekendUsage;
            CustomerProfile baseProfile = tools.analyzeCustomerNeeds(familySize, primaryUsage, preferences);
            
            // Adjust budget range (80% to 100% of stated budget)
            double adjustedBudgetMin = budget * 0.8;
            double adjustedBudgetMax = budget;
            
            CustomerProfile profile = new CustomerProfile(
                familySize,
                primaryUsage,
                preferences,
                adjustedBudgetMin,
                adjustedBudgetMax,
                baseProfile.preferredCategories(),
                baseProfile.needsTowing(),
                baseProfile.needsOffRoad(),
                baseProfile.fuelPreference()
            );
            
            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: familySize=").append(familySize)
              .append(", dailyCommute=").append(dailyCommute)
              .append(", weekendUsage=").append(weekendUsage)
              .append(", budget=$").append(String.format("%.0f", budget))
              .append(", creditScore=").append(creditScore).append("\n");
            sb.append("Complete Profile Built:\n");
            sb.append(String.format("Budget Range: $%,.0f - $%,.0f\n", profile.budgetMin(), profile.budgetMax()));
            sb.append(String.format("Primary Usage: %s\n", profile.primaryUsage()));
            sb.append(String.format("Categories: %s\n", 
                profile.preferredCategories().stream().map(VehicleCategory::getDisplayName).collect(Collectors.joining(", "))));
            sb.append("Preferences: ").append(String.join(", ", profile.preferences())).append("\n");
            sb.append("Must-Have Features: ").append(String.join(", ", mustHaveFeatures));
            state.addToolResult("buildProfile", sb.toString());
            
            return profile;
        }
        
        @Tool("Suggest vehicle categories based on profile")
        public List<String> suggestCategories(@P("Customer profile") CustomerProfile profile) {
            logToolCall("suggestCategories", "profile", profile);
            List<String> categories = profile.preferredCategories().stream()
                .map(VehicleCategory::getDisplayName)
                .collect(Collectors.toList());
            
            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: budget=$").append(String.format("%.0f-%.0f", profile.budgetMin(), profile.budgetMax()))
              .append(", familySize=").append(profile.familySize())
              .append(", primaryUsage=").append(profile.primaryUsage()).append("\n");
            sb.append("Suggested Vehicle Categories: ").append(String.join(", ", categories));
            state.addToolResult("suggestCategories", sb.toString());
            
            return categories;
        }
        
        @Tool("Filter vehicles based on customer preferences")
        public List<VehicleInfo> filterVehicles(
                @P("List of vehicle IDs to filter") List<String> vehicleIds,
                @P("Customer profile") CustomerProfile profile) {
            logToolCall("filterVehicles", "vehicleIds", vehicleIds, "profile", profile);
            
            List<VehicleInfo> filtered = MockVehicleData.VEHICLES.stream()
                .filter(v -> vehicleIds.contains(v.id()))
                .filter(v -> v.price() >= profile.budgetMin() && v.price() <= profile.budgetMax())
                .filter(v -> {
                    String vehicleCategory = v.category();
                    return profile.preferredCategories().stream()
                        .anyMatch(cat -> cat.getDisplayName().equalsIgnoreCase(vehicleCategory));
                })
                .collect(Collectors.toList());
            
            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: filteringVehicleIds=").append(vehicleIds)
              .append(", budget=$").append(String.format("%.0f-%.0f", profile.budgetMin(), profile.budgetMax()))
              .append(", categories=").append(profile.preferredCategories().stream()
                  .map(VehicleCategory::getDisplayName).collect(Collectors.joining(", "))).append("\n");
            
            if (filtered.isEmpty()) {
                sb.append("No vehicles match the profile criteria");
            } else {
                sb.append("Filtered to ").append(filtered.size()).append(" matching vehicles:\n");
                for (VehicleInfo v : filtered) {
                    sb.append(String.format("- %s %s %s - $%,.0f\n",
                        v.make().getDisplayName(), v.model(), v.trim(), v.price()));
                }
            }
            state.addToolResult("filterVehicles", sb.toString());
            
            return filtered;
        }
        
        @Tool("Create a quick profile with minimal information")
        public CustomerProfile createQuickProfile(
                @P("Budget") double budget,
                @P("Vehicle type preference (SUV, Truck, Sedan, etc)") String vehicleType) {
            logToolCall("createQuickProfile", "budget", budget, "vehicleType", vehicleType);
            
            int familySize = vehicleType.toLowerCase().contains("suv") || 
                           vehicleType.toLowerCase().contains("truck") ? 4 : 2;
            
            List<String> preferences = new ArrayList<>();
            VehicleCategory category = VehicleCategory.fromString(vehicleType);
            List<VehicleCategory> categories = category != null ? 
                List.of(category) : List.of(VehicleCategory.SUV, VehicleCategory.SEDAN);
            
            CustomerProfile profile = new CustomerProfile(
                familySize,
                "General use",
                preferences,
                budget * 0.8,
                budget,
                categories,
                false,
                false,
                "gasoline"
            );
            
            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: budget=$").append(String.format("%.0f", budget))
              .append(", vehicleType=").append(vehicleType).append("\n");
            sb.append("Quick Profile Created:\n");
            sb.append(String.format("Budget Range: $%,.0f - $%,.0f\n", profile.budgetMin(), profile.budgetMax()));
            sb.append(String.format("Vehicle Categories: %s\n", 
                categories.stream().map(VehicleCategory::getDisplayName).collect(Collectors.joining(", "))));
            sb.append(String.format("Assumed Family Size: %d", familySize));
            state.addToolResult("createQuickProfile", sb.toString());
            
            return profile;
        }
    }
    
    interface ProfilerAssistant {
        @SystemMessage("""
            You are a customer profiling specialist for GM vehicles.
            Your role is to understand customer needs and build comprehensive profiles.
            
            You should:
            - Ask minimal questions to understand needs
            - Extract information from conversation context
            - Build profiles that capture budget, family size, usage patterns
            - Suggest appropriate vehicle categories
            - Help narrow down vehicle choices
            
            Focus on:
            - Family size and passenger needs
            - Daily commute and weekend activities
            - Budget constraints
            - Must-have features
            - Fuel preferences
            
            Always try to extract this information from the conversation history first
            before asking new questions. Be efficient and helpful.
            
            When user seems overwhelmed with too many choices, use filterVehicles to narrow down.
            When starting fresh, use buildProfile or createQuickProfile based on available info.
            """)
        String assistCustomer(@UserMessage String conversation);
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
    
    public String execute(CustomerState state, String query) {
        // Pass state to tools so they can update it
        tools.setState(state);
        
        // Include conversation history
        String conversation = state.getConversationContext();
        
        // Let the LLM process the request with tools
        String response = assistant.assistCustomer(conversation);
        
        // Log the agent response (user message already added by GMVehicleGraphAgent)
        state.addAiMessage(response);
        
        return response;
    }
}