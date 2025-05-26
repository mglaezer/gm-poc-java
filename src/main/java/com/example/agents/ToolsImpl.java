package com.example.agents;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.agents.CommonRequirements.*;
import com.example.agents.CommonRequirements.VehicleMake;

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
                    VehicleCategory vehicleCategory = VehicleCategory.fromString(vehicle.category());
                    if (vehicleCategory != criteria.category()) {
                        return false;
                    }
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
                if (criteria.requiredFeatures() != null && !criteria.requiredFeatures().isEmpty()) {
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
            .filter(vehicle -> vehicle.make() == vehicleMake && 
                             vehicle.model().equalsIgnoreCase(model))
            .findFirst()
            .orElse(null);
    }
    
    @Override
    public VehicleComparison compareVehicles(List<String> vehicleIds) {
        List<VehicleInfo> vehicles = vehicleIds.stream()
            .map(this::getVehicleDetails)
            .filter(v -> v != null)
            .collect(Collectors.toList());
        
        List<String> categories = List.of("Price", "MPG City", "MPG Highway", "Seating Capacity", 
                                        "Cargo Volume", "Horsepower", "Towing Capacity");
        
        List<ComparisonPoint> points = new ArrayList<>();
        for (VehicleInfo vehicle : vehicles) {
            points.add(new ComparisonPoint("Price", vehicle.id(), "$" + String.format("%,.0f", vehicle.price())));
            points.add(new ComparisonPoint("MPG City", vehicle.id(), String.valueOf(vehicle.mpgCity())));
            points.add(new ComparisonPoint("MPG Highway", vehicle.id(), String.valueOf(vehicle.mpgHighway())));
            points.add(new ComparisonPoint("Seating Capacity", vehicle.id(), String.valueOf(vehicle.seatingCapacity())));
            points.add(new ComparisonPoint("Cargo Volume", vehicle.id(), vehicle.cargoVolume() + " cu ft"));
            points.add(new ComparisonPoint("Horsepower", vehicle.id(), String.valueOf(vehicle.horsepower())));
            points.add(new ComparisonPoint("Towing Capacity", vehicle.id(), String.format("%,.0f lbs", vehicle.maxTowingCapacity())));
        }
        
        return new VehicleComparison(vehicles, categories, points);
    }
    
    @Override
    public List<VehicleAvailability> checkAvailability(String vehicleId, String zipCode) {
        List<VehicleAvailability> availability = new ArrayList<>();
        
        for (Dealer dealer : MockVehicleData.DEALERS) {
            boolean inStock = Math.random() > 0.3;
            int quantity = inStock ? (int)(Math.random() * 5) + 1 : 0;
            String delivery = inStock ? "Available Now" : "2-3 weeks";
            
            availability.add(new VehicleAvailability(
                vehicleId,
                dealer.id(),
                inStock,
                quantity,
                delivery
            ));
        }
        
        return availability;
    }
    
    @Override
    public FinancingOption calculateFinancing(String vehicleId, double downPayment, int termMonths, String creditScore) {
        VehicleInfo vehicle = getVehicleDetails(vehicleId);
        if (vehicle == null) {
            return null;
        }
        
        double rate = MockVehicleData.FINANCING_RATES.getOrDefault(creditScore.toLowerCase(), 7.9);
        double principal = vehicle.price() - downPayment;
        double monthlyRate = rate / 100 / 12;
        
        double monthlyPayment = principal * (monthlyRate * Math.pow(1 + monthlyRate, termMonths)) / 
                              (Math.pow(1 + monthlyRate, termMonths) - 1);
        
        double totalCost = (monthlyPayment * termMonths) + downPayment;
        
        return new FinancingOption(
            vehicleId,
            vehicle.price(),
            downPayment,
            termMonths,
            rate,
            monthlyPayment,
            totalCost
        );
    }
    
    @Override
    public TestDriveAppointment scheduleTestDrive(String vehicleId, String dealerId, 
                                                 LocalDateTime dateTime, String customerName, 
                                                 String customerPhone) {
        String confirmationNumber = "TD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        return new TestDriveAppointment(
            confirmationNumber,
            vehicleId,
            dealerId,
            dateTime,
            customerName,
            customerPhone
        );
    }
}