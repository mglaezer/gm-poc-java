package com.example.agents;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Common Requirements for General Motors Car Selection Agent Demos
 * 
 * This document outlines the shared requirements for all three agent implementations:
 * - LangChain
 * - LangGraph  
 * - ADK
 * 
 * AGENT PURPOSE:
 * The General Motors agent helps users choose the right car for them based on their
 * preferences, needs, and requirements.
 * 
 * IMPLEMENTATION CONSTRAINTS:
 * 1. No real databases - all data should be mocked/hardcoded
 * 2. No actual MCP (Model Context Protocol) usage
 * 3. Tools should be implemented as plain Java methods
 * 4. No RAG (Retrieval Augmented Generation) with real data sources
 * 
 * REQUIRED TOOLS (Mock Implementations):
 * 1. SearchVehicleInventory - Search available GM vehicles by criteria
 * 2. GetVehicleDetails - Get detailed information about a specific vehicle
 * 3. CompareVehicles - Compare multiple vehicles side by side
 * 4. CheckAvailability - Check dealer availability for a specific vehicle
 * 5. CalculateFinancing - Calculate financing options and monthly payments
 * 6. ScheduleTestDrive - Schedule a test drive appointment
 * 
 * AGENT CAPABILITIES:
 * - Understand user preferences (size, budget, fuel type, features)
 * - Recommend suitable GM vehicles based on requirements
 * - Provide detailed vehicle information
 * - Compare different models
 * - Assist with financing calculations
 * - Help schedule test drives
 * 
 * MOCK DATA REQUIREMENTS:
 * - Hardcoded inventory of GM vehicles (at least 10 models)
 * - Vehicle details including: make, model, year, price, MPG, features
 * - Dealer locations and availability
 * - Financing rates and terms
 * 
 * USER INTERACTION FLOW:
 * 1. Greet user and ask about their vehicle needs
 * 2. Gather requirements (budget, size, usage, preferences)
 * 3. Search and recommend suitable vehicles
 * 4. Provide detailed information on selected vehicles
 * 5. Compare options if requested
 * 6. Discuss financing if interested
 * 7. Offer to schedule test drive
 */
public interface CommonRequirements {
    
    enum VehicleMake {
        CHEVROLET("Chevrolet"),
        GMC("GMC"),
        CADILLAC("Cadillac"),
        BUICK("Buick");
        
        private final String displayName;
        
        VehicleMake(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static VehicleMake fromString(String text) {
            for (VehicleMake make : VehicleMake.values()) {
                if (make.displayName.equalsIgnoreCase(text)) {
                    return make;
                }
            }
            return null;
        }
    }
    
    enum VehicleCategory {
        TRUCK("Truck"),
        SUV("SUV"),
        SEDAN("Sedan"),
        SPORTS_CAR("Sports Car"),
        ELECTRIC("Electric"),
        LUXURY_SUV("Luxury SUV"),
        LUXURY_SEDAN("Luxury Sedan"),
        FULL_SIZE_SUV("Full-Size SUV"),
        THREE_ROW_SUV("3-Row SUV"),
        COMPACT_SUV("Compact SUV"),
        PICKUP("Pickup"),
        HATCHBACK("Hatchback"),
        COUPE("Coupe"),
        MIDSIZE("Midsize"),
        LARGE_SUV("Large SUV");
        
        private final String displayName;
        
        VehicleCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static VehicleCategory fromString(String text) {
            for (VehicleCategory category : VehicleCategory.values()) {
                if (category.displayName.equalsIgnoreCase(text)) {
                    return category;
                }
            }
            return null;
        }
    }
    
    record VehicleInfo(
        // Basic identification
        String id,
        VehicleMake make,
        String model,
        int year,
        String trim,
        String bodyStyle,
        String vin,
        
        // Pricing and classification
        double price,
        String category,
        String subcategory,
        
        // Fuel economy and emissions
        int mpgCity,
        int mpgHighway,
        double fuelConsumptionL100km,
        double co2Emissions,
        
        // Engine and performance
        String engineType,
        double engineDisplacement,
        int horsepower,
        int torque,
        String transmissionType,
        String drivetrain,
        String fuelType,
        double fuelTankCapacity,
        Integer range, // For electric vehicles
        
        // Dimensions and weight
        double length,
        double width,
        double height,
        double wheelbase,
        double curbWeight,
        double gvwr,
        double maxPayload,
        double maxTowingCapacity,
        double loadCapacity,
        double maxWeightTons,
        
        // Capacity
        int seatingCapacity,
        int numberOfDoors,
        double cargoVolume,
        
        // Appearance
        String exteriorColor,
        String interiorColor,
        
        // Features and technology
        List<String> safetyFeatures,
        List<String> infotainmentFeatures,
        boolean navigationSystem,
        boolean bluetoothConnectivity,
        String audioSystemType,
        String climateControlType,
        boolean cruiseControl,
        boolean parkingSensors,
        boolean parkingCamera,
        boolean adaptiveCruiseControl,
        boolean laneAssist,
        boolean blindSpotMonitoring,
        
        // Fleet management
        String ownerName,
        String referenceNumber,
        String operatorUserId,
        
        // Telemetry and diagnostics
        String gpsCoordinates,
        List<String> diagnosticTroubleCodes,
        String maintenanceStatus,
        double idleTime,
        String drivingPatterns,
        
        // Trip information
        double tripDistance,
        double tripDuration,
        double accelerationData,
        double brakingData
    ) {}
    
    record Dealer(
        String id,
        String name,
        String location
    ) {}
    
    record SearchCriteria(
        VehicleCategory category,
        Double minPrice,
        Double maxPrice,
        Integer minMpg,
        String fuelType,
        List<String> requiredFeatures
    ) {}
    
    record VehicleAvailability(
        String vehicleId,
        String dealerId,
        boolean inStock,
        int quantity,
        String estimatedDelivery
    ) {}
    
    record FinancingOption(
        String vehicleId,
        double vehiclePrice,
        double downPayment,
        int termMonths,
        double interestRate,
        double monthlyPayment,
        double totalCost
    ) {}
    
    record TestDriveAppointment(
        String confirmationNumber,
        String vehicleId,
        String dealerId,
        LocalDateTime appointmentTime,
        String customerName,
        String customerPhone
    ) {}
    
    record VehicleComparison(
        List<VehicleInfo> vehicles,
        List<String> comparisonCategories,
        List<ComparisonPoint> comparisonPoints
    ) {}
    
    record ComparisonPoint(
        String category,
        String vehicleId,
        String value
    ) {}
    
    interface Tools {
        List<VehicleInfo> searchVehicleInventory(SearchCriteria criteria);
        VehicleInfo getVehicleDetails(String vehicleId);
        VehicleComparison compareVehicles(List<String> vehicleIds);
        List<VehicleAvailability> checkAvailability(String vehicleId, String zipCode);
        FinancingOption calculateFinancing(String vehicleId, double downPayment, int termMonths, String creditScore);
        TestDriveAppointment scheduleTestDrive(String vehicleId, String dealerId, LocalDateTime dateTime, String customerName, String customerPhone);
    }
    
    interface AgentCapabilities {
        String startConversation();
        SearchCriteria gatherUserRequirements();
        List<VehicleInfo> recommendVehicles(SearchCriteria criteria);
        String provideVehicleDetails(VehicleInfo vehicle);
        String compareOptions(List<VehicleInfo> vehicles);
        String discussFinancing(FinancingOption option);
        String scheduleTestDrive(TestDriveAppointment appointment);
    }
}