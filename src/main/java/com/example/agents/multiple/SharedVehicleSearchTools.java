package com.example.agents.multiple;

import com.example.agents.CommonRequirements.VehicleInfo;
import com.example.agents.ToolsImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * Shared vehicle search tools that can be used by multiple agents
 */
public class SharedVehicleSearchTools {
    private final ToolsImpl tools = new ToolsImpl();

    @Tool("Search vehicle by make and model")
    public VehicleInfo searchByMakeModel(
            @P("Make (Chevrolet, GMC, Cadillac, Buick)") String make, @P("Model name") String model) {
        ToolLogger.logToolCall("searchByMakeModel", "make", make, "model", model);
        return tools.getVehicleByMakeAndModel(make, model);
    }
}
