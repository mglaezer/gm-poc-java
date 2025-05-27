package com.example.agents.langgraph;

import com.example.agents.CommonRequirements.*;
import com.example.agents.ToolsImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Availability Coordinator Agent - Checks inventory and schedules test drives
 */
public class AvailabilityCoordinatorAgent {
    
    static class AvailabilityTools extends BaseToolLogger {
        private final ToolsImpl tools = new ToolsImpl();
        private CustomerState state;
        
        public void setState(CustomerState state) {
            this.state = state;
        }
        
        @Tool("Check vehicle availability at dealers")
        public VehicleAvailability checkAvailability(
                @P("Vehicle ID") String vehicleId,
                @P("ZIP code") String zipCode,
                @P("Search radius in miles") int radiusMiles) {
            logToolCall("checkAvailability", "vehicleId", vehicleId, "zipCode", zipCode, "radius", radiusMiles);
            List<VehicleAvailability> availabilities = tools.checkAvailability(vehicleId, zipCode);
            VehicleAvailability availability = availabilities.isEmpty() ? null : availabilities.get(0);
            if (state != null && availability != null) {
                state.logToolCall("AVAILABILITY_COORDINATOR", "checkAvailability",
                    String.format("vehicleId=%s, zipCode=%s", vehicleId, zipCode),
                    availability.inStock() ? "In stock" : "Not in stock - " + availability.estimatedDelivery());
            }
            return availability;
        }
        
        @Tool("Schedule a test drive appointment")
        public TestDriveAppointment scheduleTestDrive(
                @P("Vehicle ID") String vehicleId,
                @P("Dealer ID") String dealerId,
                @P("Preferred date/time (YYYY-MM-DD HH:MM)") String dateTimeStr,
                @P("Customer name") String customerName,
                @P("Customer phone") String customerPhone) {
            logToolCall("scheduleTestDrive", "vehicleId", vehicleId, "dealerId", dealerId, 
                       "dateTime", dateTimeStr, "customer", customerName);
            
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr.replace(" ", "T"));
            TestDriveAppointment appointment = tools.scheduleTestDrive(
                vehicleId, dealerId, dateTime, customerName, customerPhone);
            
            if (state != null && appointment != null) {
                state.logToolCall("AVAILABILITY_COORDINATOR", "scheduleTestDrive",
                    String.format("vehicleId=%s, dealer=%s, time=%s", vehicleId, dealerId, dateTimeStr),
                    "Confirmation: " + appointment.confirmationNumber());
            }
            
            return appointment;
        }
    }
    
    interface AvailabilityAssistant {
        @SystemMessage("""
            You are a vehicle availability coordinator for GM dealerships.
            Your responsibilities include:
            - Checking real-time inventory at local dealers
            - Finding vehicles in stock or with upcoming availability
            - Scheduling test drive appointments
            - Providing dealer location information
            
            Always:
            - Check availability before scheduling test drives
            - Confirm customer contact information
            - Provide clear next steps
            - Set realistic expectations for vehicle availability
            - Be helpful in finding alternative options if the desired vehicle isn't available
            """)
        String assistWithAvailability(@UserMessage String conversation);
    }
    
    private final AvailabilityAssistant assistant;
    private final AvailabilityTools tools;
    
    public AvailabilityCoordinatorAgent(ChatModel model) {
        this.tools = new AvailabilityTools();
        this.assistant = AiServices.builder(AvailabilityAssistant.class)
                .chatModel(model)
                .tools(tools)
                .build();
    }
    
    public String execute(CustomerState state, String query) {
        // Pass state to tools so they can update it
        tools.setState(state);
        
        // Include conversation history
        String conversation = state.getConversationContext();
        
        // Let the LLM process the request with tools
        String response = assistant.assistWithAvailability(conversation);
        
        // Log the agent response (user message already added by GMVehicleGraphAgent)
        state.addAiMessage(response);
        
        return response;
    }
}