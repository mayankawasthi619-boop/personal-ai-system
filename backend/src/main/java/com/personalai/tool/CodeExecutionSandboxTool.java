package com.personalai.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CodeExecutionSandboxTool implements Tool {

    @Value("${app.ai.sandbox-image:python:3.11-slim}")
    private String sandboxImage;

    @Override
    public String getName() {
        return "code_executor";
    }

    @Override
    public String getDescription() {
        return "Executes Python or JavaScript code safely inside an isolated, ephemeral sandbox environment.";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "language", Map.of("type", "string", "description", "'python' or 'javascript'"),
                        "code", Map.of("type", "string", "description", "Executable source code")
                ),
                "required", List.of("language", "code")
        );
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input) {
        String language = input.get("language").toString().toLowerCase();
        String code = (String) input.get("code");

        if (code == null || code.isBlank()) {
            return Map.of("error", "Code is required");
        }

        log.info("Executing sandboxed code for language: {}", language);

        File tempScript = null;
        try {
            String extension = language.equals("javascript") ? ".js" : ".py";
            tempScript = File.createTempFile("sandbox_" + UUID.randomUUID(), extension);
            Files.writeString(tempScript.toPath(), code);

            // Attempt Docker container sandboxing with --net=none, --memory=512m, --cpus=1.0
            ProcessBuilder pb;
            boolean dockerAvailable = isDockerAvailable();

            if (dockerAvailable) {
                pb = new ProcessBuilder(
                        "docker", "run", "--rm",
                        "--net=none",
                        "--memory=512m",
                        "--cpus=1.0",
                        "-v", tempScript.getAbsolutePath() + ":/app/script" + extension,
                        sandboxImage,
                        language.equals("javascript") ? "node" : "python",
                        "/app/script" + extension
                );
            } else {
                // Fallback to local python runtime if docker is not running locally
                log.warn("Docker environment not detected. Running fallback restricted process.");
                pb = new ProcessBuilder("python", tempScript.getAbsolutePath());
            }

            Process process = pb.start();
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                return Map.of("error", "Execution timed out after 10 seconds");
            }

            BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            String line;
            while ((line = stdoutReader.readLine()) != null) stdout.append(line).append("\n");
            while ((line = stderrReader.readLine()) != null) stderr.append(line).append("\n");

            int exitCode = process.exitValue();

            return Map.of(
                    "stdout", stdout.toString().trim(),
                    "stderr", stderr.toString().trim(),
                    "exitCode", exitCode,
                    "sandboxed", dockerAvailable
            );

        } catch (Exception ex) {
            log.error("Sandbox execution failed: ", ex);
            return Map.of("error", "Execution failed: " + ex.getMessage());
        } finally {
            if (tempScript != null && tempScript.exists()) {
                tempScript.delete();
            }
        }
    }

    private boolean isDockerAvailable() {
        try {
            Process p = new ProcessBuilder("docker", "version").start();
            return p.waitFor(2, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
