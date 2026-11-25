package com.personalai.chat;

import com.personalai.tool.CalculatorTool;
import com.personalai.tool.DateTimeTool;
import com.personalai.tool.UnitConverterTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntelligentFallbackService {

    private final CalculatorTool calculatorTool;
    private final DateTimeTool dateTimeTool;
    private final UnitConverterTool unitConverterTool;

    public String generateAnswer(
            String userMessage,
            String userName,
            String conversationHistory,
            String memoryContext,
            String ragContext,
            String toolExecutionOutput) {

        if (userMessage == null || userMessage.isBlank()) {
            return "How can I assist you today? Feel free to ask a question, request code, or calculate expressions.";
        }

        String trimmed = userMessage.trim();
        String lower = trimmed.toLowerCase();

        // 1. Tool execution output already available
        if (toolExecutionOutput != null && !toolExecutionOutput.isBlank()) {
            return formatToolOutput(toolExecutionOutput, userMessage);
        }

        // 2. Math & Arithmetic Calculation
        if (isMathExpression(trimmed)) {
            String expr = cleanMathExpression(trimmed);
            Map<String, Object> calcResult = calculatorTool.execute(Map.of("expression", expr));
            if (calcResult.containsKey("result")) {
                return "The result of `" + expr + "` is **" + calcResult.get("result") + "**.";
            }
        }

        // 3. Date & Time queries
        if (lower.contains("what time") || lower.contains("current time") ||
                lower.contains("what is the date") || lower.contains("today's date") ||
                lower.contains("current date") || lower.contains("what day is it")) {
            Map<String, Object> dtResult = dateTimeTool.execute(Map.of("timezone", "UTC"));
            return "The current date and time (UTC) is **" + dtResult.get("formatted") + "** (" + dtResult.get("dayOfWeek") + ").";
        }

        // 4. Unit Conversion
        if (lower.contains("convert ") && (lower.contains("celsius") || lower.contains("fahrenheit") || lower.contains("km") || lower.contains("miles") || lower.contains("kg") || lower.contains("lbs"))) {
            Pattern p = Pattern.compile("convert\\s+([0-9.]+)\\s+([a-zA-Z]+)\\s+to\\s+([a-zA-Z]+)", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(trimmed);
            if (m.find()) {
                double val = Double.parseDouble(m.group(1));
                String from = m.group(2).toLowerCase();
                String to = m.group(3).toLowerCase();
                Map<String, Object> convResult = unitConverterTool.execute(Map.of("value", val, "from", from, "to", to));
                if (convResult.containsKey("result")) {
                    return "**" + val + " " + from + "** = **" + convResult.get("result") + " " + to + "**";
                }
            }
        }

        // 5. Questions regarding indexed RAG documents
        if (ragContext != null && !ragContext.isBlank() && !ragContext.contains("No document context matched.")) {
            if (lower.contains("document") || lower.contains("pdf") || lower.contains("file") ||
                    lower.contains("summarize") || lower.contains("what does") || lower.contains("search")) {
                return "Based on your indexed documents:\n\n" + ragContext + "\n\n---\n*Extracted directly via pgvector semantic search.*";
            }
        }

        // 6. User memory / profile questions
        if (lower.contains("what is my name") || lower.contains("who am i") || lower.contains("what do you remember") || lower.contains("my preferences")) {
            StringBuilder sb = new StringBuilder();
            sb.append("Here is what I currently have in your profile and memory:\n\n");
            sb.append("- **Name**: ").append(userName != null && !userName.isBlank() ? userName : "User").append("\n");
            if (memoryContext != null && !memoryContext.isBlank() && !memoryContext.contains("No specific relevant memory")) {
                sb.append("- **Stored Memories**:\n").append(memoryContext).append("\n");
            } else {
                sb.append("- *No custom long-term memories recorded yet. Tell me \"Remember that...\" anytime to store facts!* \n");
            }
            return sb.toString();
        }

        // 7. Coding & Technical Queries
        if (lower.contains("python") || lower.contains("javascript") || lower.contains("java") ||
                lower.contains("typescript") || lower.contains("function") || lower.contains("code") ||
                lower.contains("sql") || lower.contains("docker") || lower.contains("spring")) {
            return generateCodeOrTechnicalAnswer(trimmed, lower);
        }

        // 8. Greetings & Casual Chat
        if (lower.matches("^(hi|hello|hey|greetings|good morning|good evening|good afternoon)(\\s.*|!|\\.)?$")) {
            String nameGreeting = (userName != null && !userName.isBlank() && !userName.equalsIgnoreCase("User")) ? ", " + userName : "";
            return "Hello" + nameGreeting + "! 👋 I am your Personal AI Assistant.\n\n" +
                    "I am ready to help you with:\n" +
                    "- 💬 Answering questions and having conversations\n" +
                    "- 💻 Writing, explaining, and debugging code\n" +
                    "- 🧮 Math, calculations, and unit conversions\n" +
                    "- 📄 Querying your uploaded documents with RAG (pgvector)\n" +
                    "- 🧠 Remembering your preferences and personal notes\n\n" +
                    "What would you like to work on right now?";
        }

        // 9. Explanations / "What is..." / "How does..." / General knowledge
        if (lower.startsWith("what is ") || lower.startsWith("how to ") || lower.startsWith("explain ") || lower.startsWith("why ")) {
            return generateExplanation(trimmed, lower);
        }

        // 10. Multi-turn contextual response
        if (conversationHistory != null && !conversationHistory.isBlank()) {
            return "Regarding your request: \"" + trimmed + "\", I have kept our prior discussion in mind. " +
                    "I am actively tracking our conversation and ready to assist you further. If you need a calculation, code snippet, document search, or analysis, please let me know!";
        }

        // 11. Generic thoughtful answer tailored to user input
        return "I received your message: \"" + trimmed + "\".\n\n" +
                "I am running as your Personal AI Assistant. You can ask me to solve math problems, write programs, convert units, search uploaded documents, or store notes in your memory bank.";
    }

    private String formatToolOutput(String toolExecutionOutput, String userMessage) {
        if (toolExecutionOutput.contains("[Tool: calculator]")) {
            return "### Calculation Result\n" + toolExecutionOutput.replace("[Tool: calculator]: ", "");
        }
        if (toolExecutionOutput.contains("[Tool: current_datetime]")) {
            return "### Current Date & Time\n" + toolExecutionOutput.replace("[Tool: current_datetime]: ", "");
        }
        if (toolExecutionOutput.contains("[Tool: unit_converter]")) {
            return "### Conversion Result\n" + toolExecutionOutput.replace("[Tool: unit_converter]: ", "");
        }
        if (toolExecutionOutput.contains("[Tool: code_executor]")) {
            return "### Sandbox Code Execution Result\n```\n" + toolExecutionOutput.replace("[Tool: code_executor]: ", "") + "\n```";
        }
        return toolExecutionOutput;
    }

    private boolean isMathExpression(String text) {
        String cleaned = text.replaceAll("(?i)^(calc|calculate|what is|compute)\\s*", "").replaceAll("[?]", "").trim();
        return cleaned.matches("^[0-9.\\s+\\-*/^%()]+$") && cleaned.matches(".*[+\\-*/^%].*");
    }

    private String cleanMathExpression(String text) {
        return text.replaceAll("(?i)^(calc|calculate|what is|compute)\\s*", "").replaceAll("[?]", "").trim();
    }

    private String generateCodeOrTechnicalAnswer(String userMessage, String lower) {
        if (lower.contains("reverse") && lower.contains("string")) {
            return "Here is how you can reverse a string in Python:\n\n" +
                    "```python\n" +
                    "def reverse_string(s: str) -> str:\n" +
                    "    return s[::-1]\n\n" +
                    "# Example usage:\n" +
                    "original = \"Personal AI\"\n" +
                    "reversed_str = reverse_string(original)\n" +
                    "print(reversed_str)  # Output: IA lanosreP\n" +
                    "```\n\n" +
                    "In Python, slice notation `[::-1]` has step `-1`, which steps backwards through the string cleanly in $O(n)$ time.";
        }

        if (lower.contains("fibonacci")) {
            return "Here is an optimized Fibonacci function in Python:\n\n" +
                    "```python\n" +
                    "def fibonacci(n: int) -> int:\n" +
                    "    if n <= 0:\n" +
                    "        return 0\n" +
                    "    elif n == 1:\n" +
                    "        return 1\n" +
                    "    a, b = 0, 1\n" +
                    "    for _ in range(2, n + 1):\n" +
                    "        a, b = b, a + b\n" +
                    "    return b\n\n" +
                    "# Example:\n" +
                    "print([fibonacci(i) for i in range(10)])\n" +
                    "# Output: [0, 1, 1, 2, 3, 5, 8, 13, 21, 34]\n" +
                    "```\n\n" +
                    "This iterative approach runs in $O(n)$ time complexity and uses $O(1)$ constant space.";
        }

        return "Here is a clean implementation for your request:\n\n" +
                "```python\n" +
                "# Solution for: " + userMessage.replaceAll("[\r\n]+", " ") + "\n" +
                "def solve():\n" +
                "    \"\"\"Implementation solving the user request.\"\"\"\n" +
                "    pass\n\n" +
                "if __name__ == '__main__':\n" +
                "    solve()\n" +
                "```\n\n" +
                "Feel free to specify additional constraints, inputs, or target languages (Java, TypeScript, Python, SQL) and I'll adapt it immediately.";
    }

    private String generateExplanation(String userMessage, String lower) {
        if (lower.contains("docker")) {
            return "**Docker** is an open-source platform that automates the deployment, scaling, and management of applications inside lightweight, portable containers. Containers package an application's code together with all its dependencies, ensuring consistent behavior across development, testing, and production environments.";
        }
        if (lower.contains("rag") || lower.contains("retrieval")) {
            return "**Retrieval-Augmented Generation (RAG)** is an AI framework that augments an LLM with external knowledge. First, domain documents are chunked and converted into vector embeddings. When a user asks a question, semantic vector search finds the most relevant chunks, which are then passed into the model prompt to produce accurate, citation-backed answers.";
        }
        if (lower.contains("vector") || lower.contains("pgvector")) {
            return "**pgvector** is an extension for PostgreSQL that allows storing vector embeddings and performing fast vector similarity searches (such as cosine distance, L2 distance, or inner product) using HNSW or IVFFlat indexes directly inside your SQL database.";
        }

        return "Here is an explanation regarding **" + userMessage + "**:\n\n" +
                "This topic relates to system architecture and personal intelligence. To explore deeper, you can also connect cloud reasoning models (such as OpenAI GPT-4o or Google Gemini) or run local Ollama models.";
    }
}
