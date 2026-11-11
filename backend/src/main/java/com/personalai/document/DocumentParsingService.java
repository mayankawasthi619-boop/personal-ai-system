package com.personalai.document;

import com.personalai.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@Slf4j
public class DocumentParsingService {

    private final Tika tika = new Tika();

    public String extractText(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            String text = tika.parseToString(inputStream);
            if (text == null || text.isBlank()) {
                throw new ApiException("Document is empty or could not extract readable text", "DOCUMENT_EMPTY");
            }
            return text.trim();
        } catch (Exception ex) {
            log.error("Failed to parse document: {}", file.getOriginalFilename(), ex);
            throw new ApiException("Failed to parse document content: " + ex.getMessage(), "DOCUMENT_PARSING_FAILED");
        }
    }
}
