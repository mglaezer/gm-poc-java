package com.example.agents.multiple;

import com.example.agents.CommonRequirements.VehicleInfo;
import com.example.agents.ToolsImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * Shared vehicle search tools that can be used by multiple agents
 */
public class SharedVehicleSearchTools extends BaseToolLogger {
    private final ToolsImpl tools = new ToolsImpl();
    private CustomerState state;

    public void setState(CustomerState state) {
        this.state = state;
    }

    @Tool("Search vehicle by make and model")
    public VehicleInfo searchByMakeModel(
            @P("Make (Chevrolet, GMC, Cadillac, Buick)") String make, @P("Model name") String model) {
        logToolCall("searchByMakeModel", "make", make, "model", model);
        VehicleInfo vehicle = tools.getVehicleByMakeAndModel(make, model);

        StringBuilder sb = new StringBuilder();
        sb.append("Parameters: make=")
                .append(make)
                .append(", model=")
                .append(model)
                .append("\n");

        if (vehicle == null) {
            sb.append("No vehicle found for ").append(make).append(" ").append(model);
        } else {
            sb.append(String.format(
                    "Found: ID: %s | %s %s %s (%d) - %s, $%,.0f, %s, %d/%d MPG",
                    vehicle.id(),
                    vehicle.make().getDisplayName(),
                    vehicle.model(),
                    vehicle.trim(),
                    vehicle.year(),
                    vehicle.bodyStyle(),
                    vehicle.price(),
                    vehicle.fuelType(),
                    vehicle.mpgCity(),
                    vehicle.mpgHighway()));
        }

        if (state != null) {
            state.addToolResult("searchByMakeModel", sb.toString());
        }

        return vehicle;
    }
}
