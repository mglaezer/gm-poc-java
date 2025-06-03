package com.example.agents.multiple;

import com.example.agents.CommonRequirements.*;
import com.example.agents.ToolsImpl;
import com.example.llmtoolkit.core.JacksonSourceResponseStructuringStrategy;
import com.example.llmtoolkit.core.JteTemplateProcessor;
import com.example.llmtoolkit.core.TemplatedLLMServiceFactory;
import com.example.llmtoolkit.core.annotations.PT;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Technical Expert Agent - Provides detailed vehicle information and comparisons
 */
public class TechnicalExpertAgent {

    static class TechnicalTools {
        private final ToolsImpl tools = new ToolsImpl();

        @Tool("Search vehicles by criteria")
        public List<VehicleInfo> searchVehicles(
                @P("Category like Truck, SUV, Sedan or null for all") String category,
                @P("Min price") Double minPrice,
                @P("Max price") Double maxPrice,
                @P("Min MPG") Integer minMpg,
                @P("Fuel type") String fuelType) {
            String priceRange = (minPrice != null ? "$" + minPrice : "$0") + "-"
                    + (maxPrice != null ? "$" + maxPrice : "unlimited");
            ToolLogger.logToolCall("searchVehicles", "category", category, "priceRange", priceRange);
            VehicleCategory cat = null;
            if (category != null && !category.equalsIgnoreCase("All") && !category.equalsIgnoreCase("Any")) {
                cat = VehicleCategory.fromString(category);
            }
            SearchCriteria criteria = new SearchCriteria(cat, minPrice, maxPrice, minMpg, fuelType, null);
            return tools.searchVehicleInventory(criteria);
        }

        @Tool("Search vehicles by make/brand")
        public List<VehicleInfo> searchVehiclesByMake(
                @P("Make like Chevrolet, GMC, Cadillac, Buick") String make, @P("Exclude EVs") boolean excludeEVs) {
            ToolLogger.logToolCall(
                    "searchVehiclesByMake", "make", make != null ? make : "null", "excludeEVs", excludeEVs);
            if (make == null) {
                return new ArrayList<>();
            }
            VehicleMake vehicleMake = VehicleMake.fromString(make);
            if (vehicleMake == null) {
                return new ArrayList<>();
            }
            List<VehicleInfo> results =
                    tools.searchVehicleInventory(new SearchCriteria(null, null, null, null, null, null)).stream()
                            .filter(v -> v.make() == vehicleMake)
                            .collect(Collectors.toList());
            if (excludeEVs) {
                results = results.stream()
                        .filter(v -> !"Electric".equalsIgnoreCase(v.fuelType()))
                        .collect(Collectors.toList());
            }
            // ChatMemory automatically tracks tool results
            return results;
        }

        @Tool("Get detailed vehicle information by ID")
        public VehicleInfo getVehicleDetails(@P("Vehicle ID") String vehicleId) {
            return tools.getVehicleDetails(vehicleId);
        }

        @Tool("Compare multiple vehicles")
        public VehicleComparison compareVehicles(@P("List of vehicle IDs") List<String> vehicleIds) {
            ToolLogger.logToolCall("compareVehicles", "vehicleIds", vehicleIds);

            return tools.compareVehicles(vehicleIds);
        }

        @Tool("Compare GM vehicle to competitors")
        public String compareToCompetitors(
                @P("GM vehicle ID") String gmVehicleId,
                @P("Competitor vehicles (e.g., 'Toyota Highlander, Honda Pilot')") String competitorList) {
            ToolLogger.logToolCall("compareToCompetitors", "gmVehicleId", gmVehicleId, "competitors", competitorList);
            VehicleComparison comparison = tools.compareToCompetitors(gmVehicleId);

            StringBuilder result = new StringBuilder();
            if (comparison == null) {
                result.append("Vehicle not found");
            } else {
                result.append("Comparing GM vehicle to competitors:\n\n");
                for (ComparisonPoint point : comparison.comparisonPoints()) {
                    result.append(point.category())
                            .append(": ")
                            .append(point.value())
                            .append("\n");
                }
            }

            return result.toString();
        }

        @Tool("Calculate total cost of ownership")
        public TotalCostOfOwnership calculateTCO(
                @P("Vehicle ID") String vehicleId,
                @P("Annual miles driven") int annualMiles,
                @P("Years of ownership") int years) {
            ToolLogger.logToolCall("calculateTCO", "vehicleId", vehicleId, "annualMiles", annualMiles, "years", years);

            return tools.calculateTotalCostOfOwnership(vehicleId, years);
        }

        @Tool("Check vehicle safety ratings")
        public SafetyRatings checkSafety(@P("Vehicle ID") String vehicleId) {
            ToolLogger.logToolCall("checkSafety", "vehicleId", vehicleId);

            return new SafetyRatings(
                    vehicleId,
                    5, // NHTSA overall
                    5, // frontal crash
                    5, // side crash
                    4, // rollover
                    List.of(
                            "Forward Collision Warning",
                            "Automatic Emergency Braking",
                            "Blind Spot Monitoring",
                            "Lane Keep Assist"),
                    true // IIHS Top Safety Pick
                    );
        }
    }

    interface TechnicalAssistant {
        @PT(templatePath = "technical_expert.jte")
        AgentResponse provideTechnicalInfo();
    }

    private final TechnicalAssistant assistant;
    private final ConversationState conversationState;

    public TechnicalExpertAgent(ChatModel model, ConversationState conversationState) {
        this.conversationState = conversationState;
        this.assistant = TemplatedLLMServiceFactory.builder()
                .serviceStrategy(new JacksonSourceResponseStructuringStrategy())
                .model(model)
                .templateProcessor(JteTemplateProcessor.create())
                .aiServiceCustomizer(aiServices -> {
                    aiServices.tools(new TechnicalTools(), new SharedVehicleSearchTools());
                    aiServices.chatMemory(conversationState.getChatMemory());
                })
                .build()
                .create(TechnicalAssistant.class);
    }

    public AgentResponse execute(String query) {
        conversationState.getChatMemory().add(UserMessage.from(query));
        return assistant.provideTechnicalInfo();
    }
}
