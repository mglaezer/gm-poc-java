package com.example.agents.multiple;

import com.example.agents.CommonRequirements.*;
import com.example.agents.ToolsImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import java.time.LocalDateTime;
import java.util.List;
import org.llmtoolkit.core.JteTemplateProcessor;
import org.llmtoolkit.core.TemplatedLLMServiceFactory;
import org.llmtoolkit.core.annotations.PT;

/**
 * Availability Coordinator Agent - Checks inventory and schedules test drives
 */
public class AvailabilityCoordinatorAgent {

    static class AvailabilityTools {
        private final ToolsImpl tools = new ToolsImpl();

        @Tool("Check vehicle availability at dealers")
        public VehicleAvailability checkAvailability(
                @P("Vehicle ID") String vehicleId,
                @P("ZIP code") String zipCode,
                @P("Search radius in miles") int radiusMiles) {
            ToolLogger.logToolCall(
                    "checkAvailability", "vehicleId", vehicleId, "zipCode", zipCode, "radius", radiusMiles);
            List<VehicleAvailability> availabilities = tools.checkAvailability(vehicleId, zipCode);
            return availabilities.isEmpty() ? null : availabilities.getFirst();
        }

        @Tool("Schedule a test drive appointment")
        public TestDriveAppointment scheduleTestDrive(
                @P("Vehicle ID") String vehicleId,
                @P("Dealer ID") String dealerId,
                @P("Preferred date/time (YYYY-MM-DD HH:MM)") String dateTimeStr,
                @P("Customer name") String customerName,
                @P("Customer phone") String customerPhone) {
            ToolLogger.logToolCall(
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
            return tools.scheduleTestDrive(vehicleId, dealerId, dateTime, customerName, customerPhone);
        }
    }

    interface AvailabilityAssistant {
        @PT(templatePath = "availability_coordinator.jte")
        String assistWithAvailability();
    }

    private final AvailabilityAssistant assistant;
    private final ConversationState conversationState;

    public AvailabilityCoordinatorAgent(ChatModel model, ConversationState conversationState) {
        this.conversationState = conversationState;
        this.assistant = TemplatedLLMServiceFactory.builder()
                .model(model)
                .templateProcessor(JteTemplateProcessor.create())
                .aiServiceCustomizer(aiServices -> {
                    aiServices.tools(new AvailabilityTools(), new SharedVehicleSearchTools());
                    aiServices.chatMemory(conversationState.getChatMemory());
                })
                .build()
                .create(AvailabilityAssistant.class);
    }

    public String execute(String query) {
        conversationState.getChatMemory().add(UserMessage.from(query));
        return assistant.assistWithAvailability();
    }
}
