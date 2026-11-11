package com.personalai.tool;

import java.util.Map;

public interface Tool {
    String getName();
    String getDescription();
    Map<String, Object> getInputSchema();
    Map<String, Object> execute(Map<String, Object> input);
}
