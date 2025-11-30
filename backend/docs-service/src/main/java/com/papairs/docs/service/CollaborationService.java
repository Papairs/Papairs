package com.papairs.docs.service;

import com.papairs.docs.exception.ResourceNotFoundException;
import com.papairs.docs.exception.UnauthorizedAccessException;
import com.papairs.docs.model.DocumentSession;
import com.papairs.docs.model.OT.Op;
import com.papairs.docs.security.HtmlSanitizer;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Service for managing collaborative document editing sessions.
 * Handles document state, operations, and user management.
 */
@Service
public class CollaborationService {
    private static final Logger logger = Logger.getLogger(CollaborationService.class.getName());
    
    private final Map<String, DocumentSession> documentSessions = new ConcurrentHashMap<>();
    private final PageService pageService;
    private final AutoSaveManager autoSaveManager;
    private final HtmlSanitizer htmlSanitizer;
    private final PermissionService permissionService;

    public CollaborationService(PageService pageService, AutoSaveManager autoSaveManager, HtmlSanitizer htmlSanitizer, PermissionService permissionService) {
        this.pageService = pageService;
        this.autoSaveManager = autoSaveManager;
        this.htmlSanitizer = htmlSanitizer;
        this.permissionService = permissionService;
    }

    /**
     * Join a user to a document session
     * Validates that the page exists and the user has access
     * @throws ResourceNotFoundException if page doesn't exist
     * @throws UnauthorizedAccessException if user doesn't have access
     */
    public DocumentSession joinDocument(String docId, String userId, WebSocketSession session) {
        // Validate page exists and user has access
        permissionService.requirePageAccess(docId, userId);
        
        DocumentSession document = getOrCreateSession(docId, userId);
        document.addSession(session, userId);
        logger.info("User " + userId + " joined document " + docId);
        return document;
    }

    /**
     * Apply an operation to a document
     */
    public String applyOperation(DocumentSession document, Op operation, String userId) {
        if (operation.htmlContent == null) {
            throw new IllegalArgumentException("Missing HTML content");
        }

        String sanitizedHtml = htmlSanitizer.sanitize(operation.htmlContent);
        document.setContent(sanitizedHtml);
        document.addOperation(operation);
        document.incrementVersion();
        
        autoSaveManager.scheduleDelayedSave(document);
        
        logger.info("User " + userId + " updated document " + document.getDocumentId() + 
                   " (length: " + sanitizedHtml.length() + ")");
        
        return sanitizedHtml;
    }

    /**
     * Handle user disconnect
     */
    public void disconnectUser(WebSocketSession session) {
        documentSessions.values().forEach(document -> {
            String userId = document.getUserForSession(session);
            document.removeSession(session);
            
            if (userId != null) {
                logger.info("User " + userId + " disconnected from document " + document.getDocumentId());
            }
            
            if (!document.hasActiveSessions() && document.isDirty()) {
                autoSaveManager.saveImmediately(document);
                logger.info("Auto-saved document " + document.getDocumentId() + " on disconnect");
            }
        });
        
        cleanupEmptySessions();
    }

    /**
     * Get existing session or null
     */
    public DocumentSession getSession(String docId) {
        return documentSessions.get(docId);
    }

    /**
     * Shutdown and cleanup
     */
    public void shutdown() {
        documentSessions.values().stream()
            .filter(DocumentSession::isDirty)
            .forEach(autoSaveManager::saveImmediately);
            
        autoSaveManager.shutdown();
        logger.info("Collaboration service shut down successfully");
    }

    private DocumentSession getOrCreateSession(String docId, String userId) {
        return documentSessions.computeIfAbsent(docId, k -> {
            try {
                var page = pageService.getPage(k, userId);
                String content = page.content();
                
                if (content == null || content.isEmpty()) {
                    content = "<p></p>";
                }
                
                logger.info("Loaded content for document: " + k + " (length: " + content.length() + ")");
                return new DocumentSession(k, content);
            } catch (Exception e) {
                logger.info("Starting with default content for new document: " + k);
                return new DocumentSession(k, "<p></p>");
            }
        });
    }

    private void cleanupEmptySessions() {
        documentSessions.entrySet().removeIf(entry -> {
            DocumentSession document = entry.getValue();
            if (!document.hasActiveSessions()) {
                logger.info("Removing empty document session: " + entry.getKey());
                return true;
            }
            return false;
        });
    }
}
