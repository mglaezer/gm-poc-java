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
                state.logToolCall("CUSTOMER_PROFILER", "analyzeCustomerNeeds",
                    String.format("familySize=%d, usage=%s", familySize, primaryUsage),
                    String.format("Profile created: budget=$%.0f-%.0f, categories=%s", 
                        profile.budgetMin(), profile.budgetMax(), profile.preferredCategories()));
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
                state.logToolCall("CUSTOMER_PROFILER", "buildProfile",
                    String.format("budget=%.0f, familySize=%d", budget, familySize),
                    String.format("Complete profile: budget=$%.0f-%.0f, categories=%s, creditScore=%s", 
                        profile.budgetMin(), profile.budgetMax(), profile.preferredCategories(), creditScore));
            }
            return profile;
        }
        
        @Tool("Suggest vehicle categories based on customer profile")
        public List<VehicleCategory> suggestCategories(
                @P("Customer profile") CustomerProfile profile) {
            logToolCall("suggestCategories", "profile", profile.familySize() + " family, $" + profile.budgetMin() + "-$" + profile.budgetMax());
            List<VehicleCategory> categories = tools.suggestVehicleCategories(profile);
            if (state != null) {
                state.logToolCall("CUSTOMER_PROFILER", "suggestCategories",
                    "profile", 
                    "Suggested categories: " + categories);
            }
            return categories;
        }
        
        @Tool("Filter existing vehicle recommendations based on user preferences")
        public List<VehicleInfo> filterVehicles(
                @P("Budget maximum or 0 for no limit") double maxBudget,
                @P("Vehicle category preference or 'any'") String preferredCategory,
                @P("Must-have feature or 'none'") String mustHaveFeature) {
            logToolCall("filterVehicles", "maxBudget", maxBudget, "category", preferredCategory, "feature", mustHaveFeature);
            
            // Get vehicles from mock data for now - in a real system this would parse from conversation
            List<VehicleInfo> currentVehicles = MockVehicleData.VEHICLES;
            if (currentVehicles == null || currentVehicles.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<VehicleInfo> filtered = new ArrayList<>();
            for (VehicleInfo vehicle : currentVehicles) {
                boolean matches = true;
                
                // Filter by budget
                if (maxBudget > 0 && vehicle.price() > maxBudget) {
                    matches = false;
                }
                
                // Filter by category (rough matching)
                if (!preferredCategory.equalsIgnoreCase("any")) {
                    String vehicleCategory = vehicle.category().toLowerCase();
                    String preferred = preferredCategory.toLowerCase();
                    if (!vehicleCategory.contains(preferred) && !preferred.contains(vehicleCategory)) {
                        matches = false;
                    }
                }
                
                if (matches) {
                    filtered.add(vehicle);
                }
            }
            
            // Log filtered results to conversation
            if (state != null) {
                String result = filtered.isEmpty() ? "No vehicles match the criteria" :
                    String.format("Filtered to %d vehicles: %s", filtered.size(),
                        filtered.stream().limit(3)
                            .map(v -> v.make().getDisplayName() + " " + v.model())
                            .collect(Collectors.joining(", ")) + (filtered.size() > 3 ? "..." : ""));
                state.logToolCall("CUSTOMER_PROFILER", "filterVehicles", 
                    String.format("budget=%.0f, category=%s", maxBudget, preferredCategory), result);
            }
            return filtered;
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
                state.logToolCall("CUSTOMER_PROFILER", "createQuickProfile",
                    String.format("budget=%.0f, size=%s", budget, vehicleSize),
                    String.format("Quick profile: budget=$%.0f-%.0f, categories=%s", 
                        budgetMin, budgetMax, categories));
            }
            return profile;
        }
    }
    
    interface ProfilerAssistant {
        @SystemMessage("""
            You are a customer profiling specialist for GM vehicles.
            Your job is to understand customer needs with MINIMAL questions.
            
            IMPORTANT RULES:
            1. DETECT THE MODE:
               - NARROWING MODE: If user just saw 4+ vehicle options and needs help choosing
               - INITIAL MODE: If no profile exists and user needs basic profiling
            2. For NARROWING MODE:
               - Ask ONLY 1-2 key filtering questions to reduce options
               - Focus on: budget range, vehicle size, or key must-have feature
               - Say: "I can help narrow this down with 1-2 quick questions"
            3. For INITIAL MODE:
               - Ask MAXIMUM 2 questions at a time
               - Focus on budget and size preference
            4. Always offer: "Or if you'd prefer, I can show you some popular options right away"
            5. If user seems reluctant, immediately offer to skip profiling
            6. Extract information from context clues rather than asking directly
            7. If user asks to \"show me\" or \"see\" vehicles, redirect to TechnicalExpert
            
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
        
        // Check conversation history to determine context
        List<String> recentVehicles = state.getRecentVehiclesMentioned();
        boolean isNarrowingMode = false;
        
        // Look for recent searches that found many vehicles
        for (String entry : recentVehicles) {
            if (entry.contains("Found") && entry.contains("vehicles")) {
                // Extract number if possible
                String[] parts = entry.split(" ");
                for (int i = 0; i < parts.length - 1; i++) {
                    if (parts[i].equals("Found")) {
                        try {
                            int count = Integer.parseInt(parts[i + 1]);
                            if (count >= 4) {
                                isNarrowingMode = true;
                                conversation += "\n\nNARROWING MODE: User saw " + count + " vehicles and needs help choosing.";
                                conversation += "\n" + entry;
                                break;
                            }
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }
                }
            }
        }
        
        // The conversation history contains all user preferences and context
        
        // All context is in conversation history
        
        conversation += "\nUser: " + query;
        
        String response = assistant.profileCustomer(conversation);
        
        // Log the response
        state.logAgentAction("CUSTOMER_PROFILER", "Response", response);
        state.addToConversationHistory("Customer Profiler: " + response);
        
        // Usually route to technical expert after profiling to show vehicles
        state.setNextAgent("TECHNICAL_EXPERT");
        
        return state;
    }
    
    @Override
    public String getName() {
        return "CUSTOMER_PROFILER";
    }
}