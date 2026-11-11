package com.personalai.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeToolTest {

    private DateTimeTool tool;

    @BeforeEach
    void setUp() {
        tool = new DateTimeTool();
    }

    @Test
    void testCurrentDateTimeUtc() {
        Map<String, Object> res = tool.execute(Map.of("timezone", "UTC"));
        assertNotNull(res);
        assertTrue(res.containsKey("iso8601"));
        assertTrue(res.containsKey("formatted"));
        assertEquals("UTC", res.get("timezone"));
        assertTrue((long) res.get("timestamp_epoch_sec") > 0);
    }

    @Test
    void testInvalidTimezoneFallback() {
        Map<String, Object> res = tool.execute(Map.of("timezone", "Invalid/Timezone"));
        assertNotNull(res);
        assertEquals("UTC", res.get("timezone"));
    }
}
