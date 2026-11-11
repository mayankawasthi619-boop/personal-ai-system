package com.personalai.document;

import com.personalai.common.ApiResponse;
import com.personalai.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<DocumentService.DocumentDto>> uploadDocument(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam("file") MultipartFile file) {
        DocumentService.DocumentDto document = documentService.uploadDocument(userPrincipal.getId(), file);
        return ResponseEntity.ok(ApiResponse.success(document));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DocumentService.DocumentDto>>> getDocuments(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<DocumentService.DocumentDto> documents = documentService.getUserDocuments(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentService.DocumentDto>> getDocument(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id) {
        DocumentService.DocumentDto document = documentService.getDocument(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(document));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteDocument(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable UUID id) {
        documentService.deleteDocument(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully"));
    }
}
