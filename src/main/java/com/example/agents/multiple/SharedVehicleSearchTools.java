package com.example.agents.multiple;

import com.example.agents.CommonRequirements;
import com.example.agents.CommonRequirements.VehicleInfo;
import com.example.agents.ToolsImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.List;

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

    @Tool("Search vehicles by criteria")
    public List<VehicleInfo> searchVehicles(
            @P("Category like Truck, SUV, Sedan or null for all") String category,
            @P("Min price") Double minPrice,
            @P("Max price") Double maxPrice,
            @P("Min MPG") Integer minMpg,
            @P("Fuel type") String fuelType) {
        String priceRange =
                (minPrice != null ? "$" + minPrice : "$0") + "-" + (maxPrice != null ? "$" + maxPrice : "unlimited");
        ToolLogger.logToolCall("searchVehicles", "category", category, "priceRange", priceRange);
        CommonRequirements.VehicleCategory cat = null;
        if (category != null && !category.equalsIgnoreCase("All") && !category.equalsIgnoreCase("Any")) {
            cat = CommonRequirements.VehicleCategory.fromString(category);
        }
        CommonRequirements.SearchCriteria criteria =
                new CommonRequirements.SearchCriteria(cat, minPrice, maxPrice, minMpg, fuelType, null);
        return tools.searchVehicleInventory(criteria);
    }
}
