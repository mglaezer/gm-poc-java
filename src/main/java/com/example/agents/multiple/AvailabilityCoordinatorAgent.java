package com.example.agents.multiple;

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

    static class AvailabilityTools {
        private final ToolsImpl tools = new ToolsImpl();
        private final ToolLogger logger = new ToolLogger();

        @Tool("Check vehicle availability at dealers")
        public VehicleAvailability checkAvailability(
                @P("Vehicle ID") String vehicleId,
                @P("ZIP code") String zipCode,
                @P("Search radius in miles") int radiusMiles) {
            logger.logToolCall("checkAvailability", "vehicleId", vehicleId, "zipCode", zipCode, "radius", radiusMiles);
            List<VehicleAvailability> availabilities = tools.checkAvailability(vehicleId, zipCode);
            VehicleAvailability availability = availabilities.isEmpty() ? null : availabilities.get(0);
            return availability;
        }

        @Tool("Schedule a test drive appointment")
        public TestDriveAppointment scheduleTestDrive(
                @P("Vehicle ID") String vehicleId,
                @P("Dealer ID") String dealerId,
                @P("Preferred date/time (YYYY-MM-DD HH:MM)") String dateTimeStr,
                @P("Customer name") String customerName,
                @P("Customer phone") String customerPhone) {
            logger.logToolCall(
                    "scheduleTestDrive",
                    "vehicleId",
                    vehicleId,
                    "dealerId",
                    dealerId,
                    "dateTime",
                    dateTimeStr,
                    "customer",
                    customerName);

            LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr.replace(" ", "T"));
            TestDriveAppointment appointment =
                    tools.scheduleTestDrive(vehicleId, dealerId, dateTime, customerName, customerPhone);
            return appointment;
        }
    }

    interface AvailabilityAssistant {
        @SystemMessage(
                """
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

    public AvailabilityCoordinatorAgent(ChatModel model, ConversationState conversationState) {
        this.tools = new AvailabilityTools();
        this.assistant = AiServices.builder(AvailabilityAssistant.class)
                .chatModel(model)
                .tools(tools)
                .chatMemory(conversationState.getChatMemory())
                .build();
    }

    public String execute(String query) {
        String response = assistant.assistWithAvailability(query);
        return response;
    }
}
