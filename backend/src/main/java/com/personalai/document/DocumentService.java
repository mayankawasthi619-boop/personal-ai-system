package com.personalai.document;

import com.personalai.exception.ApiException;
import com.personalai.rag.RagService;
import com.personalai.user.User;
import com.personalai.user.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentParsingService documentParsingService;
    private final RagService ragService;

    @Value("${app.ai.storage-path:./data/documents}")
    private String storagePath;

    @Data
    @Builder
    @AllArgsConstructor
    public static class DocumentDto {
        private UUID id;
        private String filename;
        private String mimeType;
        private Long fileSize;
        private String checksum;
        private Instant createdAt;
    }

    @Transactional
    public DocumentDto uploadDocument(UUID userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (file.isEmpty()) {
            throw new ApiException("Cannot upload empty file", "FILE_EMPTY");
        }

        // Path Traversal Security Protection
        String originalFilename = FilenameUtils.getName(file.getOriginalFilename());
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "unnamed_document";
        }

        try {
            Path uploadDir = Paths.get(storagePath, userId.toString()).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            String storedFileName = UUID.randomUUID() + "_" + originalFilename;
            Path targetPath = uploadDir.resolve(storedFileName).normalize();

            if (!targetPath.startsWith(uploadDir)) {
                throw new ApiException("Path traversal security check failed", "INVALID_FILE_PATH", HttpStatus.BAD_REQUEST);
            }

            // Calculate MD5 Checksum during write
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream is = file.getInputStream();
                 DigestInputStream dis = new DigestInputStream(is, md)) {
                Files.copy(dis, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            String checksum = HexFormat.of().formatHex(md.digest());

            Document document = Document.builder()
                    .user(user)
                    .filename(originalFilename)
                    .mimeType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                    .filePath(targetPath.toString())
                    .fileSize(file.getSize())
                    .checksum(checksum)
                    .build();

            Document savedDocument = documentRepository.save(document);

            // Parse text and trigger RAG chunking & vector embedding
            String extractedText = documentParsingService.extractText(file);
            ragService.processAndStoreDocument(savedDocument, extractedText, 500, 50);

            return mapToDto(savedDocument);

        } catch (Exception ex) {
            log.error("File upload failed: {}", originalFilename, ex);
            throw new ApiException("File upload failed: " + ex.getMessage(), "UPLOAD_FAILED");
        }
    }

    public List<DocumentDto> getUserDocuments(UUID userId) {
        return documentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public DocumentDto getDocument(UUID userId, UUID documentId) {
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ApiException("Document not found", "DOCUMENT_NOT_FOUND", HttpStatus.NOT_FOUND));
        return mapToDto(document);
    }

    @Transactional
    public void deleteDocument(UUID userId, UUID documentId) {
        Document document = documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ApiException("Document not found", "DOCUMENT_NOT_FOUND", HttpStatus.NOT_FOUND));

        // Delete underlying file on disk
        try {
            File file = new File(document.getFilePath());
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception ex) {
            log.warn("Could not delete physical file: {}", document.getFilePath());
        }

        documentRepository.deleteByIdAndUserId(documentId, userId);
    }

    private DocumentDto mapToDto(Document doc) {
        return DocumentDto.builder()
                .id(doc.getId())
                .filename(doc.getFilename())
                .mimeType(doc.getMimeType())
                .fileSize(doc.getFileSize())
                .checksum(doc.getChecksum())
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
