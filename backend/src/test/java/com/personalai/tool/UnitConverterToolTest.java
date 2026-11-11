package com.personalai.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UnitConverterToolTest {

    private UnitConverterTool tool;

    @BeforeEach
    void setUp() {
        tool = new UnitConverterTool();
    }

    @Test
    void testCelsiusToFahrenheit() {
        Map<String, Object> res = tool.execute(Map.of("value", 100.0, "from", "celsius", "to", "fahrenheit"));
        assertNotNull(res);
        assertEquals(212.0, (double) res.get("result"), 0.01);
    }

    @Test
    void testFahrenheitToCelsius() {
        Map<String, Object> res = tool.execute(Map.of("value", 32.0, "from", "fahrenheit", "to", "celsius"));
        assertNotNull(res);
        assertEquals(0.0, (double) res.get("result"), 0.01);
    }

    @Test
    void testKmToMiles() {
        Map<String, Object> res = tool.execute(Map.of("value", 10.0, "from", "km", "to", "miles"));
        assertNotNull(res);
        assertEquals(6.21371, (double) res.get("result"), 0.01);
    }

    @Test
    void testKgToLbs() {
        Map<String, Object> res = tool.execute(Map.of("value", 1.0, "from", "kg", "to", "lbs"));
        assertNotNull(res);
        assertEquals(2.20462, (double) res.get("result"), 0.01);
    }
}
