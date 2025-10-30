package com.papairs.docs.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/docs")
@CrossOrigin(origins = "http://localhost:3000")
public class FolderController {
    @GetMapping("/folders")
    public String getFolders() {
        return "List of document folders";
    }

    @PostMapping("/folders")
    public String createFolder() {
        return "Folder created";
    }

    @GetMapping("/folders/{id}")
    public String getFolderById() {
        return "Get folder by ID";
    }

    @PostMapping("/folders/{id}")
    public String updateFolder() {
        return "Folder updated";
    }

    @DeleteMapping("/folders/{id}")
    public String deleteFolder() {
        return "Folder deleted";
    }

    @GetMapping("/folders/{id}/tree")
    public String getFolderTree() {
        return "Folder tree structure";
    }
}
