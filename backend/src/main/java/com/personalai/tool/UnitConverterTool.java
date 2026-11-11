package com.personalai.tool;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class UnitConverterTool implements Tool {

    @Override
    public String getName() {
        return "unit_converter";
    }

    @Override
    public String getDescription() {
        return "Converts physical quantities between units (temperature, length, weight)";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "value", Map.of("type", "number", "description", "Numeric scalar value"),
                        "from", Map.of("type", "string", "description", "Source unit: 'celsius', 'fahrenheit', 'km', 'miles', 'kg', 'lbs'"),
                        "to", Map.of("type", "string", "description", "Target unit: 'celsius', 'fahrenheit', 'km', 'miles', 'kg', 'lbs'")
                ),
                "required", List.of("value", "from", "to")
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input) {
        double value = Double.parseDouble(input.get("value").toString());
        String from = input.get("from").toString().toLowerCase();
        String to = input.get("to").toString().toLowerCase();

        double converted = value;

        if (from.equals("celsius") && to.equals("fahrenheit")) {
            converted = (value * 9 / 5) + 32;
        } else if (from.equals("fahrenheit") && to.equals("celsius")) {
            converted = (value - 32) * 5 / 9;
        } else if (from.equals("km") && to.equals("miles")) {
            converted = value * 0.621371;
        } else if (from.equals("miles") && to.equals("km")) {
            converted = value / 0.621371;
        } else if (from.equals("kg") && to.equals("lbs")) {
            converted = value * 2.20462;
        } else if (from.equals("lbs") && to.equals("kg")) {
            converted = value / 2.20462;
        }

        return Map.of("result", converted, "from", from, "to", to, "original_value", value);
    }
}
