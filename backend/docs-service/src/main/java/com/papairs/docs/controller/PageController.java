package com.papairs.docs.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/docs")
@CrossOrigin(origins = "http://localhost:3000")
public class PageController {
    @GetMapping("/pages")
    public String getPages() {
        return "List of document folders";
    }

    @PostMapping("/pages")
    public String createPage() {
        return "Page created";
    }

    @GetMapping("/pages/{id}")
    public String getPageById() {
        return "Get page by ID";
    }

    @PutMapping("/pages/{id}")
    public String updatePage() {
        return "Page updated";
    }

    @DeleteMapping("/pages/{id}")
    public String deletePage() {
        return "Page deleted";
    }

    @GetMapping("/pages/{id}/members")
    public String getPageMembers() {
        return "List of page members";
    }
}
