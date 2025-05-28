package com.example.agents.multiple;

import com.example.agents.CommonRequirements.*;
import com.example.agents.ToolsImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.llmtoolkit.core.JteTemplateProcessor;
import org.llmtoolkit.core.TemplatedLLMServiceFactory;
import org.llmtoolkit.core.annotations.PT;

/**
 * Technical Expert Agent - Provides detailed vehicle information and comparisons
 */
public class TechnicalExpertAgent {

    static class TechnicalTools {
        private final ToolsImpl tools = new ToolsImpl();
        private final ToolLogger logger = new ToolLogger();

        @Tool("Search vehicles by criteria")
        public List<VehicleInfo> searchVehicles(
                @P("Category like Truck, SUV, Sedan or null for all") String category,
                @P("Min price") Double minPrice,
                @P("Max price") Double maxPrice,
                @P("Min MPG") Integer minMpg,
                @P("Fuel type") String fuelType) {
            String priceRange = (minPrice != null ? "$" + minPrice : "$0") + "-"
                    + (maxPrice != null ? "$" + maxPrice : "unlimited");
            logger.logToolCall("searchVehicles", "category", category, "priceRange", priceRange);
            VehicleCategory cat = null;
            if (category != null && !category.equalsIgnoreCase("All") && !category.equalsIgnoreCase("Any")) {
                cat = VehicleCategory.fromString(category);
            }
            SearchCriteria criteria = new SearchCriteria(cat, minPrice, maxPrice, minMpg, fuelType, null);
            List<VehicleInfo> results = tools.searchVehicleInventory(criteria);

            // Log results as ToolExecutionResultMessage
            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: category=")
                    .append(category != null ? category : "all")
                    .append(", price=")
                    .append(priceRange)
                    .append(", minMPG=")
                    .append(minMpg != null ? minMpg : "any")
                    .append(", fuel=")
                    .append(fuelType != null ? fuelType : "any")
                    .append("\n");

            if (results.isEmpty()) {
                sb.append("No vehicles found matching criteria");
            } else {
                sb.append("Found ").append(results.size()).append(" vehicles:\n");
                for (VehicleInfo v : results) {
                    sb.append(String.format(
                            "- ID: %s | %s %s %s (%d) - %s, $%,.0f, %s, %dmpg city/%dmpg hwy\n",
                            v.id(),
                            v.make().getDisplayName(),
                            v.model(),
                            v.trim(),
                            v.year(),
                            v.bodyStyle(),
                            v.price(),
                            v.fuelType(),
                            v.mpgCity(),
                            v.mpgHighway()));
                }
            }
            // ChatMemory automatically tracks tool results
            return results;
        }

        @Tool("Search vehicles by make/brand")
        public List<VehicleInfo> searchVehiclesByMake(
                @P("Make like Chevrolet, GMC, Cadillac, Buick") String make, @P("Exclude EVs") boolean excludeEVs) {
            logger.logToolCall("searchVehiclesByMake", "make", make != null ? make : "null", "excludeEVs", excludeEVs);
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

            // Log detailed results to state as ToolExecutionResultMessage
            StringBuilder resultDetails = new StringBuilder();

            if (results.isEmpty()) {
                resultDetails.append("No vehicles found for make: ").append(make);
                if (excludeEVs) {
                    resultDetails.append(" (excluding EVs)");
                }
            } else {
                resultDetails.append(String.format("Found %d %s vehicles", results.size(), make));
                if (excludeEVs) {
                    resultDetails.append(" (excluding EVs)");
                }
                resultDetails.append(":\n");
                for (VehicleInfo vehicle : results) {
                    resultDetails.append(String.format(
                            "- ID: %s | %s %s %s (%d) - %s, $%,.0f, %s, %d MPG city/%d MPG hwy\n",
                            vehicle.id(),
                            vehicle.make().getDisplayName(),
                            vehicle.model(),
                            vehicle.trim(),
                            vehicle.year(),
                            vehicle.bodyStyle(),
                            vehicle.price(),
                            vehicle.fuelType(),
                            vehicle.mpgCity(),
                            vehicle.mpgHighway()));
                }
            }

            // ChatMemory automatically tracks tool results

            return results;
        }

        @Tool("Get detailed vehicle information by ID")
        public VehicleInfo getVehicleDetails(@P("Vehicle ID") String vehicleId) {
            VehicleInfo vehicle = tools.getVehicleDetails(vehicleId);

            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: vehicleId=").append(vehicleId).append("\n");

            if (vehicle == null) {
                sb.append("Vehicle not found with ID: ").append(vehicleId);
            } else {
                sb.append(String.format(
                        "Vehicle Details:\nID: %s | %s %s %s (%d)\n",
                        vehicle.id(),
                        vehicle.make().getDisplayName(),
                        vehicle.model(),
                        vehicle.trim(),
                        vehicle.year()));
                sb.append(String.format(
                        "Price: $%,.0f | Body: %s | Engine: %s %.1fL\n",
                        vehicle.price(), vehicle.bodyStyle(), vehicle.engineType(), vehicle.engineDisplacement()));
                sb.append(String.format(
                        "Power: %dhp/%dlb-ft | Drivetrain: %s %s | Fuel: %s\n",
                        vehicle.horsepower(),
                        vehicle.torque(),
                        vehicle.drivetrain(),
                        vehicle.transmissionType(),
                        vehicle.fuelType()));
                sb.append(String.format(
                        "Economy: %d city/%d hwy MPG | Seats: %d | Cargo: %.1f cu ft\n",
                        vehicle.mpgCity(), vehicle.mpgHighway(), vehicle.seatingCapacity(), vehicle.cargoVolume()));
                if (vehicle.range() != null) {
                    sb.append(String.format("Electric Range: %d miles\n", vehicle.range()));
                }
                sb.append("Safety: ")
                        .append(String.join(", ", vehicle.safetyFeatures()))
                        .append("\n");
                sb.append("Tech: ").append(String.join(", ", vehicle.infotainmentFeatures()));
            }
            // ChatMemory automatically tracks tool results
            return vehicle;
        }

        @Tool("Compare multiple vehicles")
        public VehicleComparison compareVehicles(@P("List of vehicle IDs") List<String> vehicleIds) {
            logger.logToolCall("compareVehicles", "vehicleIds", vehicleIds);
            VehicleComparison comparison = tools.compareVehicles(vehicleIds);

            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: vehicleIds=").append(vehicleIds).append("\n");

            if (comparison == null || comparison.vehicles().isEmpty()) {
                sb.append("Unable to compare vehicles - invalid IDs");
            } else {
                sb.append("Comparing ").append(comparison.vehicles().size()).append(" vehicles:\n");
                for (VehicleInfo v : comparison.vehicles()) {
                    sb.append(String.format(
                            "- ID: %s | %s %s %s (%d) - $%,.0f\n",
                            v.id(), v.make().getDisplayName(), v.model(), v.trim(), v.year(), v.price()));
                }
                sb.append("\nComparison Points:\n");
                for (ComparisonPoint point : comparison.comparisonPoints()) {
                    sb.append(String.format(
                            "- %s: %s (Vehicle: %s)\n", point.category(), point.value(), point.vehicleId()));
                }
            }
            // ChatMemory automatically tracks tool results
            return comparison;
        }

        @Tool("Compare GM vehicle to competitors")
        public String compareToCompetitors(
                @P("GM vehicle ID") String gmVehicleId,
                @P("Competitor vehicles (e.g., 'Toyota Highlander, Honda Pilot')") String competitorList) {
            logger.logToolCall("compareToCompetitors", "gmVehicleId", gmVehicleId, "competitors", competitorList);
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

            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: gmVehicleId=")
                    .append(gmVehicleId)
                    .append(", competitors=")
                    .append(competitorList)
                    .append("\n");
            sb.append(result.toString());
            // ChatMemory automatically tracks tool results

            return result.toString();
        }

        @Tool("Calculate total cost of ownership")
        public TotalCostOfOwnership calculateTCO(
                @P("Vehicle ID") String vehicleId,
                @P("Annual miles driven") int annualMiles,
                @P("Years of ownership") int years) {
            logger.logToolCall("calculateTCO", "vehicleId", vehicleId, "annualMiles", annualMiles, "years", years);
            TotalCostOfOwnership tco = tools.calculateTotalCostOfOwnership(vehicleId, years);

            StringBuilder sb = new StringBuilder();
            sb.append("Parameters: vehicleId=")
                    .append(vehicleId)
                    .append(", annualMiles=")
                    .append(annualMiles)
                    .append(", years=")
                    .append(years)
                    .append("\n");

            if (tco == null) {
                sb.append("Unable to calculate TCO - vehicle not found");
            } else {
                sb.append(String.format("Total Cost of Ownership (%d years):\n", years));
                sb.append(String.format("Purchase Price: $%,.0f\n", tco.purchasePrice()));
                sb.append(String.format("Fuel Cost: $%,.0f\n", tco.totalFuelCost()));
                sb.append(String.format("Maintenance: $%,.0f\n", tco.totalMaintenanceCost()));
                sb.append(String.format("Insurance: $%,.0f\n", tco.totalInsuranceCost()));
                sb.append(String.format("Depreciation: $%,.0f\n", tco.depreciation()));
                sb.append(String.format("Total Cost: $%,.0f\n", tco.totalCost()));
                sb.append(String.format("Cost per Mile: $%.2f", tco.costPerMile()));
            }
            // ChatMemory automatically tracks tool results

            return tco;
        }

        @Tool("Check vehicle safety ratings")
        public SafetyRatings checkSafety(@P("Vehicle ID") String vehicleId) {
            logger.logToolCall("checkSafety", "vehicleId", vehicleId);
            SafetyRatings ratings = new SafetyRatings(
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

            String sb = "Parameters: vehicleId=" + vehicleId + "\n" + "Safety Ratings:\n"
                    + String.format("NHTSA Overall: %d/5 stars\n", ratings.overallRating())
                    + String.format(
                            "Frontal Crash: %d/5 | Side Crash: %d/5 | Rollover: %d/5\n",
                            ratings.frontalCrashRating(), ratings.sideCrashRating(), ratings.rolloverRating())
                    + "Safety Features: "
                    + String.join(", ", ratings.safetyFeatures())
                    + "\n"
                    + "IIHS Top Safety Pick: "
                    + (ratings.topSafetyPick() ? "Yes" : "No");
            // ChatMemory automatically tracks tool results

            return ratings;
        }
    }

    interface TechnicalAssistant {
        @PT(templatePath = "technical_expert.jte")
        String provideTechnicalInfo();
    }

    private final TechnicalAssistant assistant;
    private final TechnicalTools tools;
    private final SharedVehicleSearchTools sharedSearchTools;
    private final ConversationState conversationState;

    public TechnicalExpertAgent(ChatModel model, ConversationState conversationState) {
        this.conversationState = conversationState;
        this.tools = new TechnicalTools();
        this.sharedSearchTools = new SharedVehicleSearchTools();
        this.assistant = TemplatedLLMServiceFactory.builder()
                .model(model)
                .templateProcessor(JteTemplateProcessor.create())
                .aiServiceCustomizer(aiServices -> {
                    aiServices.tools(tools, sharedSearchTools);
                    aiServices.chatMemory(conversationState.getChatMemory());
                })
                .build()
                .create(TechnicalAssistant.class);
    }

    public String execute(String query) {
        conversationState.getChatMemory().add(UserMessage.from(query));
        return assistant.provideTechnicalInfo();
    }
}
