package com.personalai.chat;

import com.personalai.tool.CalculatorTool;
import com.personalai.tool.DateTimeTool;
import com.personalai.tool.UnitConverterTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IntelligentFallbackServiceTest {

    private IntelligentFallbackService fallbackService;

    @BeforeEach
    void setUp() {
        CalculatorTool calculatorTool = new CalculatorTool();
        DateTimeTool dateTimeTool = new DateTimeTool();
        UnitConverterTool unitConverterTool = new UnitConverterTool();
        fallbackService = new IntelligentFallbackService(calculatorTool, dateTimeTool, unitConverterTool);
    }

    @Test
    void testMathCalculationAnswer() {
        String answer = fallbackService.generateAnswer(
                "what is 25 * 4",
                "Alice",
                "",
                "",
                "",
                ""
        );
        assertTrue(answer.contains("100"), "Answer should compute the math correctly: " + answer);
    }

    @Test
    void testDateTimeAnswer() {
        String answer = fallbackService.generateAnswer(
                "what time is it right now?",
                "Bob",
                "",
                "",
                "",
                ""
        );
        assertTrue(answer.toLowerCase().contains("current date and time"), "Answer should provide datetime: " + answer);
    }

    @Test
    void testUnitConversionAnswer() {
        String answer = fallbackService.generateAnswer(
                "convert 100 kg to lbs",
                "Charlie",
                "",
                "",
                "",
                ""
        );
        assertTrue(answer.contains("kg") && answer.contains("lbs"), "Answer should provide unit conversion: " + answer);
    }

    @Test
    void testCodingQueryAnswer() {
        String answer = fallbackService.generateAnswer(
                "write a python function to reverse a string",
                "Dev",
                "",
                "",
                "",
                ""
        );
        assertTrue(answer.contains("```python") && answer.contains("def reverse_string"), "Answer should contain python code: " + answer);
    }

    @Test
    void testGreetingAnswer() {
        String answer = fallbackService.generateAnswer(
                "Hello there!",
                "Mayank",
                "",
                "",
                "",
                ""
        );
        assertTrue(answer.contains("Mayank"), "Answer should greet the user by name: " + answer);
    }

    @Test
    void testRagDocumentAnswer() {
        String answer = fallbackService.generateAnswer(
                "summarize my document",
                "Alice",
                "",
                "",
                "[Doc: resume.pdf]\nAlice is a senior AI engineer.",
                ""
        );
        assertTrue(answer.contains("Alice is a senior AI engineer"), "Answer should synthesize document context: " + answer);
    }
}
