package com.personalai.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculatorToolTest {

    private CalculatorTool calculatorTool;

    @BeforeEach
    void setUp() {
        calculatorTool = new CalculatorTool();
    }

    @Test
    void testBasicArithmetic() {
        Map<String, Object> result = calculatorTool.execute(Map.of("expression", "10 + 5 * 2"));
        assertNotNull(result);
        assertEquals(20.0, (double) result.get("result"), 0.001);
    }

    @Test
    void testParenthesesAndDivision() {
        Map<String, Object> result = calculatorTool.execute(Map.of("expression", "(100 - 20) / 4"));
        assertNotNull(result);
        assertEquals(20.0, (double) result.get("result"), 0.001);
    }

    @Test
    void testPowerOperator() {
        Map<String, Object> result = calculatorTool.execute(Map.of("expression", "2 ^ 3"));
        assertNotNull(result);
        assertEquals(8.0, (double) result.get("result"), 0.001);
    }

    @Test
    void testInvalidExpression() {
        Map<String, Object> result = calculatorTool.execute(Map.of("expression", "invalid_syntax"));
        assertTrue(result.containsKey("error"));
    }
}
