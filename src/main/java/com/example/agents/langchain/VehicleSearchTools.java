package com.example.agents.langchain;

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
    
    @Tool("Search for vehicles by make and model. Returns detailed information about a specific vehicle. Returns null if  not found.")
    public VehicleInfo searchVehicleByMakeAndModel(String make, String model) {
        return tools.getVehicleByMakeAndModel(make, model);
    }
    
    @Tool("Get detailed information about a vehicle by its ID")
    public VehicleInfo getVehicleById(String vehicleId) {
        VehicleInfo vehicle = tools.getVehicleDetails(vehicleId);
        if (vehicle == null) {
            throw new RuntimeException("No vehicle found with ID: " + vehicleId);
        }
        return vehicle;
    }
    
    @Tool("Search vehicles by criteria like category, price range, fuel type, etc.")
    public List<VehicleInfo> searchVehicles(
            String category,
            Double minPrice,
            Double maxPrice,
            Integer minMpg,
            String fuelType) {
        
        SearchCriteria criteria = new SearchCriteria(
            category,
            minPrice,
            maxPrice,
            minMpg,
            fuelType,
            null
        );
        
        return tools.searchVehicleInventory(criteria);
    }
    
    @Tool("Compare multiple vehicles by their IDs")
    public VehicleComparison compareVehicles(List<String> vehicleIds) {
        return tools.compareVehicles(vehicleIds);
    }
    
    @Tool("Check dealer availability for a specific vehicle")
    public List<VehicleAvailability> checkAvailability(String vehicleId, String zipCode) {
        return tools.checkAvailability(vehicleId, zipCode);
    }
    
    @Tool("Calculate financing options for a vehicle")
    public FinancingOption calculateFinancing(
            String vehicleId,
            double downPayment,
            int termMonths,
            String creditScore) {
        
        return tools.calculateFinancing(vehicleId, downPayment, termMonths, creditScore);
    }
}