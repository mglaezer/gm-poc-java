package com.example.agents.langchain;

import static com.example.agents.multiple.ToolLogger.logToolCall;

import com.example.agents.CommonRequirements.*;
import com.example.agents.ToolsImpl;
import dev.langchain4j.agent.tool.Tool;
import java.util.List;

/**
 * LangChain4j tool definitions for vehicle search and information retrieval.
 * These tools are used by the AI agent to help users find and learn about GM vehicles.
 */
public class VehicleSearchTools {

    private final ToolsImpl tools = new ToolsImpl();

    @Tool(
            "Search for vehicles by make and model. Returns detailed information about a specific vehicle. Returns null if  not found.")
    public VehicleInfo searchVehicleByMakeAndModel(String make, String model) {
        logToolCall("searchVehicleByMakeAndModel", "make", make, "model", model);
        return tools.getVehicleByMakeAndModel(make, model);
    }

    @Tool("Get detailed information about a vehicle by its ID")
    public VehicleInfo getVehicleById(String vehicleId) {
        logToolCall("getVehicleById", "vehicleId", vehicleId);
        VehicleInfo vehicle = tools.getVehicleDetails(vehicleId);
        if (vehicle == null) {
            throw new RuntimeException("No vehicle found with ID: " + vehicleId);
        }
        return vehicle;
    }

    @Tool(
            "Search vehicles by criteria. Category should be like 'Truck', 'SUV', 'Sedan', 'Sports Car', etc. NOT the make/brand. MinMpg is minimum miles per gallon (fuel efficiency), not year.")
    public List<VehicleInfo> searchVehicles(
            String category, Double minPrice, Double maxPrice, Integer minMpg, String fuelType) {

        logToolCall(
                "searchVehicles",
                "category",
                category,
                "minPrice",
                minPrice,
                "maxPrice",
                maxPrice,
                "minMpg",
                minMpg,
                "fuelType",
                fuelType);

        VehicleCategory vehicleCategory = null;
        if (category != null && !category.isEmpty()) {
            vehicleCategory = VehicleCategory.fromString(category);
        }

        SearchCriteria criteria = new SearchCriteria(vehicleCategory, minPrice, maxPrice, minMpg, fuelType, null);

        return tools.searchVehicleInventory(criteria);
    }

    @Tool("Compare multiple vehicles by their IDs")
    public VehicleComparison compareVehicles(List<String> vehicleIds) {
        logToolCall("compareVehicles", "vehicleIds", vehicleIds);
        return tools.compareVehicles(vehicleIds);
    }

    @Tool("Check dealer availability for a specific vehicle")
    public List<VehicleAvailability> checkAvailability(String vehicleId, String zipCode) {
        logToolCall("checkAvailability", "vehicleId", vehicleId, "zipCode", zipCode);
        return tools.checkAvailability(vehicleId, zipCode);
    }

    @Tool("Calculate financing options for a vehicle")
    public FinancingOption calculateFinancing(
            String vehicleId, double downPayment, int termMonths, String creditScore) {

        logToolCall(
                "calculateFinancing",
                "vehicleId",
                vehicleId,
                "downPayment",
                downPayment,
                "termMonths",
                termMonths,
                "creditScore",
                creditScore);

        return tools.calculateFinancing(vehicleId, downPayment, termMonths, creditScore);
    }

    @Tool("Search all vehicles by make (brand like Chevrolet, GMC, Cadillac)")
    public List<VehicleInfo> searchVehiclesByMake(String make) {
        logToolCall("searchVehiclesByMake", "make", make);
        VehicleMake vehicleMake = VehicleMake.fromString(make);
        if (vehicleMake == null) {
            return List.of();
        }
        SearchCriteria allVehiclesCriteria = new SearchCriteria(null, null, null, null, null, null);
        return tools.searchVehicleInventory(allVehiclesCriteria).stream()
                .filter(v -> v.make() == vehicleMake)
                .collect(java.util.stream.Collectors.toList());
    }
}
