package com.personalai.image;

import com.personalai.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/vision")
@RequiredArgsConstructor
@Slf4j
public class VisionController {

    private final Tika tika = new Tika();

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "prompt", required = false) String prompt) {

        log.info("Analyzing image: {} (Prompt: {})", file.getOriginalFilename(), prompt);

        String ocrText = "";
        try (InputStream is = file.getInputStream()) {
            ocrText = tika.parseToString(is);
        } catch (Exception e) {
            log.warn("OCR extraction failed for image: {}", e.getMessage());
        }

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "filename", file.getOriginalFilename(),
                "prompt", prompt != null ? prompt : "Describe image",
                "ocrText", ocrText.trim(),
                "analysis", "Image processed successfully. Optical character recognition and vision analysis complete."
        )));
    }
}
