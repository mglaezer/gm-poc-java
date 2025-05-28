package com.example.agents;

import static com.example.agents.CommonRequirements.*;

import com.example.agents.CommonRequirements.VehicleCategory;
import com.example.agents.CommonRequirements.VehicleMake;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the Tools interface providing mock functionality
 * for the GM vehicle selection agent.
 */
public class ToolsImpl implements Tools {

    @Override
    public List<VehicleInfo> searchVehicleInventory(SearchCriteria criteria) {
        return MockVehicleData.VEHICLES.stream()
                .filter(vehicle -> {
                    if (criteria.category() != null) {
                        // Check if vehicle category matches or contains the searched category
                        VehicleCategory vehicleCategory = VehicleCategory.fromString(vehicle.category());
                        if (vehicleCategory == criteria.category()) {
                            return true; // Exact match
                        }

                        // Check if the vehicle's category string contains the searched category
                        String searchedCategory = criteria.category().getDisplayName();
                        if (vehicle.category().toLowerCase().contains(searchedCategory.toLowerCase())) {
                            return true; // Partial match (e.g., "Luxury SUV" contains "SUV")
                        }

                        return false;
                    }
                    if (criteria.minPrice() != null && vehicle.price() < criteria.minPrice()) {
                        return false;
                    }
                    if (criteria.maxPrice() != null && vehicle.price() > criteria.maxPrice()) {
                        return false;
                    }
                    if (criteria.minMpg() != null) {
                        int avgMpg = (vehicle.mpgCity() + vehicle.mpgHighway()) / 2;
                        if (avgMpg < criteria.minMpg()) {
                            return false;
                        }
                    }
                    if (criteria.fuelType() != null && !vehicle.fuelType().equalsIgnoreCase(criteria.fuelType())) {
                        return false;
                    }
                    if (criteria.requiredFeatures() != null
                            && !criteria.requiredFeatures().isEmpty()) {
                        List<String> allFeatures = new ArrayList<>();
                        allFeatures.addAll(vehicle.safetyFeatures());
                        allFeatures.addAll(vehicle.infotainmentFeatures());

                        for (String required : criteria.requiredFeatures()) {
                            boolean hasFeature = allFeatures.stream()
                                    .anyMatch(feature -> feature.toLowerCase().contains(required.toLowerCase()));
                            if (!hasFeature) {
                                return false;
                            }
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    public VehicleInfo getVehicleDetails(String vehicleId) {
        return MockVehicleData.VEHICLES.stream()
                .filter(vehicle -> vehicle.id().equals(vehicleId))
                .findFirst()
                .orElse(null);
    }

    public VehicleInfo getVehicleByMakeAndModel(String make, String model) {
        VehicleMake vehicleMake = VehicleMake.fromString(make);
        if (vehicleMake == null) {
            return null;
        }
        return MockVehicleData.VEHICLES.stream()
                .filter(vehicle ->
                        vehicle.make() == vehicleMake && vehicle.model().equalsIgnoreCase(model))
                .findFirst()
                .orElse(null);
    }

    @Override
    public VehicleComparison compareVehicles(List<String> vehicleIds) {
        List<VehicleInfo> vehicles = vehicleIds.stream()
                .map(this::getVehicleDetails)
                .filter(v -> v != null)
                .collect(Collectors.toList());

        List<String> categories = List.of(
                "Price",
                "MPG City",
                "MPG Highway",
                "Seating Capacity",
                "Cargo Volume",
                "Horsepower",
                "Towing Capacity");

        List<ComparisonPoint> points = new ArrayList<>();
        for (VehicleInfo vehicle : vehicles) {
            points.add(new ComparisonPoint("Price", vehicle.id(), "$" + String.format("%,.0f", vehicle.price())));
            points.add(new ComparisonPoint("MPG City", vehicle.id(), String.valueOf(vehicle.mpgCity())));
            points.add(new ComparisonPoint("MPG Highway", vehicle.id(), String.valueOf(vehicle.mpgHighway())));
            points.add(
                    new ComparisonPoint("Seating Capacity", vehicle.id(), String.valueOf(vehicle.seatingCapacity())));
            points.add(new ComparisonPoint("Cargo Volume", vehicle.id(), vehicle.cargoVolume() + " cu ft"));
            points.add(new ComparisonPoint("Horsepower", vehicle.id(), String.valueOf(vehicle.horsepower())));
            points.add(new ComparisonPoint(
                    "Towing Capacity", vehicle.id(), String.format("%,.0f lbs", vehicle.maxTowingCapacity())));
        }

        return new VehicleComparison(vehicles, categories, points);
    }

    @Override
    public List<VehicleAvailability> checkAvailability(String vehicleId, String zipCode) {
        List<VehicleAvailability> availability = new ArrayList<>();

        for (Dealer dealer : MockVehicleData.DEALERS) {
            boolean inStock = Math.random() > 0.3;
            int quantity = inStock ? (int) (Math.random() * 5) + 1 : 0;
            String delivery = inStock ? "Available Now" : "2-3 weeks";

            availability.add(new VehicleAvailability(vehicleId, dealer.id(), inStock, quantity, delivery));
        }

        return availability;
    }

    @Override
    public FinancingOption calculateFinancing(
            String vehicleId, double downPayment, int termMonths, String creditScore) {
        VehicleInfo vehicle = getVehicleDetails(vehicleId);
        if (vehicle == null) {
            return null;
        }

        double rate = MockVehicleData.FINANCING_RATES.getOrDefault(creditScore.toLowerCase(), 7.9);
        double principal = vehicle.price() - downPayment;
        double monthlyRate = rate / 100 / 12;

        double monthlyPayment = principal
                * (monthlyRate * Math.pow(1 + monthlyRate, termMonths))
                / (Math.pow(1 + monthlyRate, termMonths) - 1);

        double totalCost = (monthlyPayment * termMonths) + downPayment;

        return new FinancingOption(
                vehicleId, vehicle.price(), downPayment, termMonths, rate, monthlyPayment, totalCost);
    }

    @Override
    public TestDriveAppointment scheduleTestDrive(
            String vehicleId, String dealerId, LocalDateTime dateTime, String customerName, String customerPhone) {
        String confirmationNumber =
                "TD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return new TestDriveAppointment(confirmationNumber, vehicleId, dealerId, dateTime, customerName, customerPhone);
    }

    // Customer Profiler Tools

    @Override
    public CustomerProfile analyzeCustomerNeeds(int familySize, String primaryUsage, List<String> preferences) {
        double budgetMin = 25000;
        double budgetMax = 60000;

        List<VehicleCategory> preferredCategories = new ArrayList<>();
        boolean needsTowing = preferences.stream().anyMatch(p -> p.toLowerCase().contains("towing"));
        boolean needsOffRoad =
                preferences.stream().anyMatch(p -> p.toLowerCase().contains("offroad"));

        if (familySize > 5) {
            preferredCategories.add(VehicleCategory.THREE_ROW_SUV);
            preferredCategories.add(VehicleCategory.FULL_SIZE_SUV);
            budgetMin = 35000;
        } else if (familySize > 3) {
            preferredCategories.add(VehicleCategory.SUV);
            preferredCategories.add(VehicleCategory.MIDSIZE);
        } else {
            preferredCategories.add(VehicleCategory.SEDAN);
            preferredCategories.add(VehicleCategory.COMPACT_SUV);
        }

        if (needsTowing) {
            preferredCategories.add(VehicleCategory.TRUCK);
            preferredCategories.add(VehicleCategory.PICKUP);
            budgetMin = 35000;
        }

        String fuelPreference = preferences.stream()
                .filter(p ->
                        p.toLowerCase().contains("electric") || p.toLowerCase().contains("hybrid"))
                .findFirst()
                .orElse("gasoline");

        return new CustomerProfile(
                familySize,
                primaryUsage,
                preferences,
                budgetMin,
                budgetMax,
                preferredCategories,
                needsTowing,
                needsOffRoad,
                fuelPreference);
    }

    @Override
    public CustomerProfile buildCustomerProfile(CustomerRequirements requirements) {
        List<VehicleCategory> categories = suggestVehicleCategories(new CustomerProfile(
                requirements.familySize(),
                requirements.dailyCommute(),
                requirements.mustHaveFeatures(),
                requirements.budget() * 0.8,
                requirements.budget(),
                new ArrayList<>(),
                false,
                false,
                "gasoline"));

        return new CustomerProfile(
                requirements.familySize(),
                requirements.dailyCommute() + " / " + requirements.weekendUsage(),
                requirements.mustHaveFeatures(),
                requirements.budget() * 0.8,
                requirements.budget(),
                categories,
                requirements.mustHaveFeatures().stream()
                        .anyMatch(f -> f.toLowerCase().contains("towing")),
                requirements.mustHaveFeatures().stream()
                        .anyMatch(f -> f.toLowerCase().contains("offroad")),
                "gasoline");
    }

    @Override
    public List<VehicleCategory> suggestVehicleCategories(CustomerProfile profile) {
        List<VehicleCategory> suggestions = new ArrayList<>();

        if (profile.familySize() > 5) {
            suggestions.add(VehicleCategory.THREE_ROW_SUV);
            suggestions.add(VehicleCategory.FULL_SIZE_SUV);
        } else if (profile.familySize() > 3) {
            suggestions.add(VehicleCategory.SUV);
            suggestions.add(VehicleCategory.COMPACT_SUV);
        } else {
            suggestions.add(VehicleCategory.SEDAN);
            suggestions.add(VehicleCategory.COUPE);
        }

        if (profile.needsTowing()) {
            suggestions.add(VehicleCategory.TRUCK);
            suggestions.add(VehicleCategory.PICKUP);
        }

        if ("electric".equals(profile.fuelPreference())) {
            suggestions.add(VehicleCategory.ELECTRIC);
        }

        return suggestions.stream().distinct().collect(Collectors.toList());
    }

    // Technical Expert Tools

    @Override
    public VehicleComparison compareToCompetitors(String vehicleId) {
        VehicleInfo vehicle = getVehicleDetails(vehicleId);
        if (vehicle == null) return null;

        List<VehicleInfo> competitors = new ArrayList<>();
        competitors.add(vehicle);

        // Add mock competitor data
        List<ComparisonPoint> points = new ArrayList<>();
        points.add(new ComparisonPoint("Price", vehicleId, "$" + String.format("%,.0f", vehicle.price())));
        points.add(new ComparisonPoint("Price", "COMP001", "$" + String.format("%,.0f", vehicle.price() * 1.05)));
        points.add(new ComparisonPoint(
                "MPG Combined", vehicleId, String.valueOf((vehicle.mpgCity() + vehicle.mpgHighway()) / 2)));
        points.add(new ComparisonPoint(
                "MPG Combined", "COMP001", String.valueOf(((vehicle.mpgCity() + vehicle.mpgHighway()) / 2) - 2)));

        return new VehicleComparison(competitors, List.of("Price", "MPG Combined", "Warranty", "Features"), points);
    }

    @Override
    public TotalCostOfOwnership calculateTotalCostOfOwnership(String vehicleId, int years) {
        VehicleInfo vehicle = getVehicleDetails(vehicleId);
        if (vehicle == null) return null;

        double purchasePrice = vehicle.price();
        double avgMpg = (vehicle.mpgCity() + vehicle.mpgHighway()) / 2.0;
        double milesPerYear = 12000;
        double gasPrice = 3.50;

        double totalFuelCost = (milesPerYear * years / avgMpg) * gasPrice;
        double totalMaintenanceCost = years * 1200; // $1200/year average
        double totalInsuranceCost = years * 1800; // $150/month average
        double depreciation = purchasePrice * 0.5; // 50% over ownership period

        double totalCost = purchasePrice + totalFuelCost + totalMaintenanceCost + totalInsuranceCost;
        double totalMiles = milesPerYear * years;
        double costPerMile = totalCost / totalMiles;

        return new TotalCostOfOwnership(
                vehicleId,
                purchasePrice,
                totalFuelCost,
                totalMaintenanceCost,
                totalInsuranceCost,
                depreciation,
                totalCost,
                costPerMile);
    }

    @Override
    public SafetyRatings checkSafetyRatings(String vehicleId) {
        VehicleInfo vehicle = getVehicleDetails(vehicleId);
        if (vehicle == null) return null;

        // Mock safety ratings based on vehicle category
        boolean isLuxury = vehicle.category().contains("Luxury");
        int baseRating = isLuxury ? 5 : 4;

        return new SafetyRatings(
                vehicleId,
                baseRating,
                baseRating,
                baseRating,
                Math.max(3, baseRating - 1),
                vehicle.safetyFeatures(),
                isLuxury || vehicle.price() > 40000);
    }

    // Financial Advisor Tools

    @Override
    public List<FinancingOption> compareFinancingOptions(String vehicleId, String creditScore) {
        VehicleInfo vehicle = getVehicleDetails(vehicleId);
        if (vehicle == null) return new ArrayList<>();

        List<FinancingOption> options = new ArrayList<>();

        // Different term options
        int[] terms = {36, 48, 60, 72};
        for (int term : terms) {
            options.add(calculateFinancing(vehicleId, vehicle.price() * 0.2, term, creditScore));
        }

        return options;
    }

    @Override
    public InsuranceCost calculateInsuranceCosts(String vehicleId, String zipCode, DriverProfile driverProfile) {
        VehicleInfo vehicle = getVehicleDetails(vehicleId);
        if (vehicle == null) return null;

        double baseRate = 100;

        // Adjust based on vehicle
        if (vehicle.category().contains("Sports")) baseRate *= 1.5;
        if (vehicle.category().contains("Luxury")) baseRate *= 1.3;
        if (vehicle.price() > 50000) baseRate *= 1.2;

        // Adjust based on driver
        if (driverProfile.age() < 25) baseRate *= 1.8;
        if (driverProfile.accidentsLast5Years() > 0) baseRate *= 1.3;
        if (driverProfile.ticketsLast5Years() > 2) baseRate *= 1.2;

        List<String> discounts = new ArrayList<>();
        if (driverProfile.yearsLicensed() > 10) {
            baseRate *= 0.9;
            discounts.add("Safe Driver");
        }
        if ("excellent".equals(driverProfile.creditScore())) {
            baseRate *= 0.95;
            discounts.add("Good Credit");
        }

        return new InsuranceCost(vehicleId, baseRate, baseRate * 12, "Full Coverage", discounts);
    }

    @Override
    public BudgetRecommendation suggestBudgetAllocation(double monthlyIncome, double monthlyExpenses) {
        double availableIncome = monthlyIncome - monthlyExpenses;
        double maxMonthlyPayment = availableIncome * 0.15; // 15% of available income

        double recommendedVehiclePrice = maxMonthlyPayment * 60 * 0.9; // 60 month loan at 90% financing
        double recommendedDownPayment = recommendedVehiclePrice * 0.1;

        String affordabilityRating = maxMonthlyPayment > 500 ? "Good" : maxMonthlyPayment > 300 ? "Moderate" : "Tight";

        List<String> suggestions = new ArrayList<>();
        if (maxMonthlyPayment < 400) {
            suggestions.add("Consider certified pre-owned vehicles");
            suggestions.add("Look for manufacturer incentives");
        }
        suggestions.add("Keep total transportation costs under 20% of income");

        return new BudgetRecommendation(
                recommendedVehiclePrice, recommendedDownPayment, maxMonthlyPayment, affordabilityRating, suggestions);
    }

    // Negotiation Coach Tools

    @Override
    public TradeInValue calculateTradeInValue(VehicleTradeIn tradeIn) {
        // Base value calculation (mock)
        double baseValue = 15000;

        // Adjust for age
        int age = 2024 - tradeIn.year();
        baseValue *= Math.pow(0.85, age); // 15% depreciation per year

        // Adjust for mileage
        if (tradeIn.mileage() > 100000) baseValue *= 0.7;
        else if (tradeIn.mileage() > 60000) baseValue *= 0.85;

        // Adjust for condition
        switch (tradeIn.condition().toLowerCase()) {
            case "excellent" -> baseValue *= 1.1;
            case "good" -> baseValue *= 1.0;
            case "fair" -> baseValue *= 0.85;
            case "poor" -> baseValue *= 0.7;
        }

        List<String> valueFactors = new ArrayList<>();
        valueFactors.add("Vehicle age: " + age + " years");
        valueFactors.add("Mileage: " + tradeIn.mileage());
        valueFactors.add("Condition: " + tradeIn.condition());

        return new TradeInValue(
                baseValue,
                baseValue * 0.9, // Dealer typically offers less
                baseValue * 1.1, // Private party typically more
                age < 3 ? "High" : age < 6 ? "Medium" : "Low",
                valueFactors);
    }

    @Override
    public NegotiationStrategy suggestNegotiationStrategy(String vehicleId, MarketConditions marketConditions) {
        VehicleInfo vehicle = getVehicleDetails(vehicleId);
        if (vehicle == null) return null;

        double msrp = vehicle.price();
        double targetPrice = msrp * 0.93; // Target 7% off MSRP
        double walkAwayPrice = msrp * 0.97; // Walk away if less than 3% off

        // Adjust based on market conditions
        if ("low".equals(marketConditions.inventoryLevel())) {
            targetPrice = msrp * 0.96;
            walkAwayPrice = msrp * 0.98;
        }
        if (marketConditions.endOfMonth() || marketConditions.endOfYear()) {
            targetPrice = msrp * 0.91;
            walkAwayPrice = msrp * 0.95;
        }

        List<String> negotiationPoints = new ArrayList<>();
        negotiationPoints.add("Research competitor pricing");
        negotiationPoints.add("Get pre-approved financing");
        negotiationPoints.add("Know invoice price");

        List<String> leveragePoints = new ArrayList<>();
        if (marketConditions.endOfMonth()) leveragePoints.add("End of month - dealers need to meet quotas");
        if ("high".equals(marketConditions.inventoryLevel()))
            leveragePoints.add("High inventory - dealer motivated to move units");

        return new NegotiationStrategy(
                targetPrice,
                walkAwayPrice,
                negotiationPoints,
                marketConditions.endOfMonth() ? "Now - End of Month" : "Wait for end of month",
                leveragePoints);
    }

    @Override
    public List<Incentive> findIncentivesAndRebates(String vehicleId, String zipCode) {
        VehicleInfo vehicle = getVehicleDetails(vehicleId);
        if (vehicle == null) return new ArrayList<>();

        List<Incentive> incentives = new ArrayList<>();

        // Cash rebate
        incentives.add(new Incentive(
                "Cash Rebate",
                1500,
                "Manufacturer cash back",
                LocalDateTime.now().plusDays(30),
                List.of("Must finance through GM Financial")));

        // Financing offer
        incentives.add(new Incentive(
                "Special Financing",
                0,
                "0% APR for 60 months",
                LocalDateTime.now().plusDays(45),
                List.of("Qualified buyers only", "Cannot combine with cash rebate")));

        // Loyalty bonus
        if (vehicle.make() == VehicleMake.CHEVROLET) {
            incentives.add(new Incentive(
                    "Loyalty Bonus",
                    500,
                    "Current Chevrolet owners",
                    LocalDateTime.now().plusDays(60),
                    List.of("Must own 2015 or newer Chevrolet")));
        }

        return incentives;
    }

    // EV Specialist Tools

    @Override
    public ChargingCost calculateChargingCosts(String vehicleId, String zipCode, double dailyMiles) {
        VehicleInfo vehicle = getVehicleDetails(vehicleId);
        if (vehicle == null || vehicle.range() == null) return null;

        double kwhPer100Miles = 30; // Average for EVs
        double homeRate = 0.12; // $/kWh home electricity
        double publicRate = 0.35; // $/kWh public charging

        double dailyKwh = (dailyMiles / 100) * kwhPer100Miles;
        double dailyCostHome = dailyKwh * homeRate;
        double dailyCostPublic = dailyKwh * publicRate;

        return new ChargingCost(
                dailyCostHome, dailyCostHome * 30, dailyCostHome / dailyMiles, dailyCostHome, dailyCostPublic);
    }

    @Override
    public List<ChargingStation> findChargingStations(String zipCode, double radiusMiles) {
        List<ChargingStation> stations = new ArrayList<>();

        // Mock charging stations
        stations.add(new ChargingStation(
                "ChargePoint Station #1", "123 Main St, San Jose, CA", 2.5, "Level 2", 4, 0.25, "ChargePoint"));

        stations.add(new ChargingStation(
                "Tesla Supercharger", "456 Electric Ave, San Jose, CA", 5.1, "DC Fast", 8, 0.40, "Tesla"));

        stations.add(new ChargingStation(
                "EVgo Fast Charging", "789 Power Blvd, San Jose, CA", 7.3, "DC Fast", 2, 0.35, "EVgo"));

        return stations.stream().filter(s -> s.distance() <= radiusMiles).collect(Collectors.toList());
    }

    @Override
    public RangeEstimate estimateRangeForTrip(String vehicleId, double tripDistance, String weatherCondition) {
        VehicleInfo vehicle = getVehicleDetails(vehicleId);
        if (vehicle == null || vehicle.range() == null) return null;

        double baseRange = vehicle.range();
        double adjustedRange = baseRange;
        List<String> factors = new ArrayList<>();

        // Weather adjustments
        switch (weatherCondition.toLowerCase()) {
            case "cold" -> {
                adjustedRange *= 0.7;
                factors.add("Cold weather (-30%)");
            }
            case "hot" -> {
                adjustedRange *= 0.85;
                factors.add("AC usage (-15%)");
            }
            case "rain" -> {
                adjustedRange *= 0.9;
                factors.add("Wet conditions (-10%)");
            }
        }

        // Highway driving typically reduces range
        if (tripDistance > 100) {
            adjustedRange *= 0.9;
            factors.add("Highway driving (-10%)");
        }

        double rangeReduction = baseRange - adjustedRange;
        boolean canComplete = adjustedRange >= tripDistance;

        return new RangeEstimate(baseRange, adjustedRange, rangeReduction, factors, canComplete);
    }
}
