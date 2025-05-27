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
            
            if (state != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Parameters: vehicleId=").append(vehicleId)
                  .append(", zipCode=").append(zipCode)
                  .append(", radius=").append(radiusMiles).append(" miles\n");
                
                if (availability == null) {
                    sb.append("No availability information found for this vehicle");
                } else {
                    sb.append(String.format("Vehicle Availability:\n"));
                    sb.append(String.format("Dealer ID: %s\n", availability.dealerId()));
                    sb.append(String.format("In Stock: %s\n", availability.inStock() ? "Yes" : "No"));
                    if (availability.inStock()) {
                        sb.append(String.format("Quantity Available: %d\n", availability.quantity()));
                    } else {
                        sb.append(String.format("Estimated Delivery: %s\n", availability.estimatedDelivery()));
                    }
                }
                state.addToolResult("checkAvailability", sb.toString());
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
            
            if (state != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Parameters: vehicleId=").append(vehicleId)
                  .append(", dealerId=").append(dealerId)
                  .append(", dateTime=").append(dateTimeStr)
                  .append(", customer=").append(customerName)
                  .append(", phone=").append(customerPhone).append("\n");
                
                if (appointment == null) {
                    sb.append("Unable to schedule test drive - please check availability");
                } else {
                    sb.append(String.format("Test Drive Scheduled:\n"));
                    sb.append(String.format("Confirmation Number: %s\n", appointment.confirmationNumber()));
                    sb.append(String.format("Vehicle: %s\n", appointment.vehicleId()));
                    sb.append(String.format("Dealer: %s\n", appointment.dealerId()));
                    sb.append(String.format("Date/Time: %s\n", appointment.appointmentTime()));
                    sb.append(String.format("Customer: %s (%s)", appointment.customerName(), appointment.customerPhone()));
                }
                state.addToolResult("scheduleTestDrive", sb.toString());
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