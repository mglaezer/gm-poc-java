package com.example.agents.multiple;

import com.example.agents.CommonRequirements.*;
import com.example.agents.MockVehicleData;
import com.example.agents.ToolsImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.llmtoolkit.core.JacksonSourceResponseStructuringStrategy;
import org.llmtoolkit.core.JteTemplateProcessor;
import org.llmtoolkit.core.TemplatedLLMServiceFactory;
import org.llmtoolkit.core.annotations.PT;

/**
 * Customer Profiler Agent - Understands customer needs and builds profiles
 */
public class CustomerProfilerAgent {

    static class ProfilerTools {
        private final ToolsImpl tools = new ToolsImpl();

        @Tool("Analyze customer needs based on family size, usage, and preferences")
        public CustomerProfile analyzeNeeds(
                @P("Family size") int familySize,
                @P("Primary usage (commute, family, adventure)") String primaryUsage,
                @P("List of preferences") List<String> preferences) {
            ToolLogger.logToolCall(
                    "analyzeNeeds", "familySize", familySize, "primaryUsage", primaryUsage, "preferences", preferences);
            return tools.analyzeCustomerNeeds(familySize, primaryUsage, preferences);
        }

        @Tool("Build complete customer profile from requirements")
        public CustomerProfile buildProfile(
                @P("Family size") int familySize,
                @P("Daily commute description") String dailyCommute,
                @P("Weekend usage description") String weekendUsage,
                @P("Must-have features") List<String> mustHaveFeatures,
                @P("Budget") double budget,
                @P("Credit score (excellent, good, fair, poor)") String creditScore) {
            ToolLogger.logToolCall(
                    "buildProfile",
                    "familySize",
                    familySize,
                    "dailyCommute",
                    dailyCommute,
                    "weekendUsage",
                    weekendUsage,
                    "budget",
                    budget);

            List<String> preferences = new ArrayList<>();
            if (dailyCommute.toLowerCase().contains("highway")
                    || dailyCommute.toLowerCase().contains("long")) {
                preferences.add("fuel efficiency");
                preferences.add("comfortable seats");
            }
            if (weekendUsage.toLowerCase().contains("camping")
                    || weekendUsage.toLowerCase().contains("outdoor")) {
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

            return new CustomerProfile(
                    familySize,
                    primaryUsage,
                    preferences,
                    adjustedBudgetMin,
                    budget,
                    baseProfile.preferredCategories(),
                    baseProfile.needsTowing(),
                    baseProfile.needsOffRoad(),
                    baseProfile.fuelPreference());
        }

        @Tool("Suggest vehicle categories based on profile")
        public List<String> suggestCategories(@P("Customer profile") CustomerProfile profile) {
            ToolLogger.logToolCall("suggestCategories", "profile", profile);
            return profile.preferredCategories().stream()
                    .map(VehicleCategory::getDisplayName)
                    .collect(Collectors.toList());
        }

        @Tool("Filter vehicles based on customer preferences")
        public List<VehicleInfo> filterVehicles(
                @P("List of vehicle IDs to filter") List<String> vehicleIds,
                @P("Customer profile") CustomerProfile profile) {
            ToolLogger.logToolCall("filterVehicles", "vehicleIds", vehicleIds, "profile", profile);

            return MockVehicleData.VEHICLES.stream()
                    .filter(v -> vehicleIds.contains(v.id()))
                    .filter(v -> v.price() >= profile.budgetMin() && v.price() <= profile.budgetMax())
                    .filter(v -> {
                        String vehicleCategory = v.category();
                        return profile.preferredCategories().stream()
                                .anyMatch(cat -> cat.getDisplayName().equalsIgnoreCase(vehicleCategory));
                    })
                    .collect(Collectors.toList());
        }

        @Tool("Create a quick profile with minimal information")
        public CustomerProfile createQuickProfile(
                @P("Budget") double budget, @P("Vehicle type preference (SUV, Truck, Sedan, etc)") String vehicleType) {
            ToolLogger.logToolCall("createQuickProfile", "budget", budget, "vehicleType", vehicleType);

            int familySize = vehicleType.toLowerCase().contains("suv")
                            || vehicleType.toLowerCase().contains("truck")
                    ? 4
                    : 2;

            List<String> preferences = new ArrayList<>();
            VehicleCategory category = VehicleCategory.fromString(vehicleType);
            List<VehicleCategory> categories =
                    category != null ? List.of(category) : List.of(VehicleCategory.SUV, VehicleCategory.SEDAN);

            return new CustomerProfile(
                    familySize, "General use", preferences, budget * 0.8, budget, categories, false, false, "gasoline");
        }
    }

    interface ProfilerAssistant {
        @PT(templatePath = "customer_profiler.jte")
        AgentResponse assistCustomer();
    }

    private final ProfilerAssistant assistant;
    private final ConversationState conversationState;

    public CustomerProfilerAgent(ChatModel model, ConversationState conversationState) {
        this.conversationState = conversationState;
        this.assistant = TemplatedLLMServiceFactory.builder()
                .serviceStrategy(new JacksonSourceResponseStructuringStrategy())
                .model(model)
                .templateProcessor(JteTemplateProcessor.create())
                .aiServiceCustomizer(aiServices -> {
                    aiServices.tools(new ProfilerTools(), new SharedVehicleSearchTools());
                    aiServices.chatMemory(conversationState.getChatMemory());
                })
                .build()
                .create(ProfilerAssistant.class);
    }

    public AgentResponse execute(String query) {
        conversationState.getChatMemory().add(UserMessage.from(query));
        return assistant.assistCustomer();
    }
}
