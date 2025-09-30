package com.papairs.docs.controller;

import com.papairs.docs.model.Document;
import com.papairs.docs.model.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/docs")
@CrossOrigin(origins = "http://localhost:3000")
public class DocsController {

    @GetMapping("/health")
    public ApiResponse health() {
        return new ApiResponse("success", "Docs service is running", 
                              Map.of("timestamp", LocalDateTime.now(),
                                     "service", "docs-service",
                                     "status", "healthy"));
    }

    @GetMapping("/all")
    public List<Document> getAllDocuments() {
        // Mock documents
        List<Document> documents = new ArrayList<>();
        documents.add(new Document(1L, "Getting Started", 
                                  "Learn how to get started with Papairs", 
                                  LocalDateTime.now().minusDays(5)));
        documents.add(new Document(2L, "API Documentation", 
                                  "Complete API reference for developers", 
                                  LocalDateTime.now().minusDays(3)));
        documents.add(new Document(3L, "User Guide", 
                                  "Comprehensive user guide for end users", 
                                  LocalDateTime.now().minusDays(1)));
        return documents;
    }

    @GetMapping("/{id}")
    public ApiResponse getDocument(@PathVariable Long id) {
        // Mock document retrieval
        Document doc = new Document(id, "Sample Document", 
                                   "This is a sample document content", 
                                   LocalDateTime.now());
        return new ApiResponse("success", "Document retrieved successfully", doc);
    }

    @PostMapping
    public ApiResponse createDocument(@RequestBody Document document) {
        // Mock document creation
        document.setId(System.currentTimeMillis());
        document.setCreatedAt(LocalDateTime.now());
        return new ApiResponse("success", "Document created successfully", document);
    }

    @PutMapping("/{id}")
    public ApiResponse updateDocument(@PathVariable Long id, @RequestBody Document document) {
        // Mock document update
        document.setId(id);
        return new ApiResponse("success", "Document updated successfully", document);
    }

    @DeleteMapping("/{id}")
    public ApiResponse deleteDocument(@PathVariable Long id) {
        // Mock document deletion
        return new ApiResponse("success", "Document deleted successfully", 
                              Map.of("deletedId", id));
    }
}