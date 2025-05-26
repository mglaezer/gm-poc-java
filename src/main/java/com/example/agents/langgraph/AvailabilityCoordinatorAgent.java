package com.example.agents.langgraph;

import com.example.agents.CommonRequirements.*;
import com.example.agents.ToolsImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Availability Coordinator Agent - Handles inventory and test drives
 */
public class AvailabilityCoordinatorAgent implements AgentNode {
    
    static class AvailabilityTools extends BaseToolLogger {
        private final ToolsImpl tools = new ToolsImpl();
        private CustomerState state;
        
        public void setState(CustomerState state) {
            this.state = state;
        }
        
        @Tool("Check vehicle availability at dealers")
        public List<VehicleAvailability> checkAvailability(
                @P("Vehicle ID") String vehicleId,
                @P("ZIP code") String zipCode) {
            List<VehicleAvailability> availability = tools.checkAvailability(vehicleId, zipCode);
            if (state != null) {
                state.set("lastAvailabilityCheck", availability);
                state.set("lastCheckedZipCode", zipCode);
            }
            return availability;
        }
        
        @Tool("Schedule a test drive appointment")
        public TestDriveAppointment scheduleTestDrive(
                @P("Vehicle ID") String vehicleId,
                @P("Dealer ID") String dealerId,
                @P("Date and time (yyyy-MM-dd HH:mm)") String dateTimeStr,
                @P("Customer name") String customerName,
                @P("Customer phone") String customerPhone) {
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, 
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            TestDriveAppointment appointment = tools.scheduleTestDrive(vehicleId, dealerId, dateTime, customerName, customerPhone);
            if (state != null) {
                state.set("testDriveAppointment", appointment);
            }
            return appointment;
        }
    }
    
    interface AvailabilityAssistant {
        @SystemMessage("""
            You are an availability coordinator for GM dealerships.
            You help customers find vehicles in stock and schedule test drives.
            
            IMPORTANT: Use the customer's selected vehicle or recommended vehicles from previous interactions.
            Don't ask which vehicle they're interested in if it's already been discussed.
            
            You can:
            - Check vehicle availability at nearby dealers
            - Provide inventory information
            - Schedule test drive appointments
            - Suggest alternative vehicles if the desired one is not available
            - Coordinate with multiple dealers
            
            Use the tools to check real-time availability.
            Be helpful in finding the best options for customers.
            Offer to schedule test drives when appropriate.
            """)
        String coordinateAvailability(@UserMessage String conversation);
    }
    
    private final AvailabilityAssistant assistant;
    private final AvailabilityTools tools;
    
    public AvailabilityCoordinatorAgent(ChatLanguageModel model) {
        this.tools = new AvailabilityTools();
        this.assistant = AiServices.builder(AvailabilityAssistant.class)
                .chatLanguageModel(model)
                .tools(tools)
                .build();
    }
    
    @Override
    public CustomerState process(CustomerState state) {
        // Pass state to tools so they can update it
        tools.setState(state);
        
        String query = state.getCurrentQuery();
        String conversation = String.join("\n", state.getConversationHistory());
        
        // Add selected vehicle context if available
        VehicleInfo selectedVehicle = state.getSelectedVehicle();
        if (selectedVehicle != null) {
            conversation += "\n\nSelected Vehicle:";
            conversation += "\n- ID: " + selectedVehicle.id();
            conversation += "\n- " + selectedVehicle.make().getDisplayName() + " " + selectedVehicle.model();
            conversation += "\n- Price: $" + String.format("%,.0f", selectedVehicle.price());
        } else {
            // If no selected vehicle, show recommended vehicles
            List<VehicleInfo> recommendedVehicles = state.getRecommendedVehicles();
            if (recommendedVehicles != null && !recommendedVehicles.isEmpty()) {
                conversation += "\n\nRecommended Vehicles:";
                for (VehicleInfo vehicle : recommendedVehicles) {
                    conversation += "\n- " + vehicle.id() + ": " + 
                                  vehicle.make().getDisplayName() + " " + vehicle.model() + 
                                  " ($" + String.format("%,.0f", vehicle.price()) + ")";
                }
            }
        }
        
        // Add any previous availability checks
        Object lastCheck = state.get("lastAvailabilityCheck");
        if (lastCheck != null) {
            conversation += "\n\nPrevious availability check performed for ZIP: " + state.get("lastCheckedZipCode");
        }
        
        // Add test drive appointment if scheduled
        Object appointment = state.get("testDriveAppointment");
        if (appointment != null) {
            conversation += "\n\nTest drive already scheduled.";
        }
        
        conversation += "\nUser: " + query;
        
        String response = assistant.coordinateAvailability(conversation);
        state.addToConversationHistory("Availability Coordinator: " + response);
        
        return state;
    }
    
    @Override
    public String getName() {
        return "AVAILABILITY_COORDINATOR";
    }
}