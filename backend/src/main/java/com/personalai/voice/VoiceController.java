package com.personalai.voice;

import com.personalai.common.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
@Slf4j
public class VoiceController {

    @PostMapping("/stt")
    public ResponseEntity<ApiResponse<Map<String, String>>> speechToText(@RequestParam("file") MultipartFile file) {
        log.info("Received audio file for STT (Size: {} bytes)", file.getSize());
        // Abstraction for Whisper STT processing
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "transcription", "Speech-to-text preview: Ready for voice integration",
                "status", "COMPLETED"
        )));
    }

    @Data
    public static class TtsRequest {
        private String text;
        private String voice = "default";
    }

    @PostMapping("/tts")
    public ResponseEntity<ApiResponse<Map<String, String>>> textToSpeech(@RequestBody TtsRequest request) {
        log.info("Generating TTS audio for text length: {}", request.getText().length());
        // Abstraction for Text-to-speech engine
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "audioUrl", "/api/voice/audio/sample.mp3",
                "status", "GENERATED"
        )));
    }
}
