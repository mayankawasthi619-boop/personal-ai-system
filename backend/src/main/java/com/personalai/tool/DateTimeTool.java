package com.personalai.tool;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class DateTimeTool implements Tool {

    @Override
    public String getName() {
        return "current_datetime";
    }

    @Override
    public String getDescription() {
        return "Returns current date, time, and timezone information";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "timezone", Map.of("type", "string", "description", "Optional timezone e.g. 'UTC', 'America/New_York', 'Asia/Kolkata'")
                )
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input) {
        String tzStr = (input != null && input.containsKey("timezone")) ? (String) input.get("timezone") : "UTC";
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(tzStr);
        } catch (Exception ex) {
            zoneId = ZoneId.of("UTC");
        }

        ZonedDateTime now = ZonedDateTime.now(zoneId);
        String formatted = now.format(DateTimeFormatter.RFC_1123_DATE_TIME);

        return Map.of(
                "iso8601", now.toString(),
                "formatted", formatted,
                "timezone", zoneId.toString(),
                "timestamp_epoch_sec", Instant.now().getEpochSecond()
        );
    }
}
