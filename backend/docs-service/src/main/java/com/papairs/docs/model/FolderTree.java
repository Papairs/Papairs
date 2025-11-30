package com.papairs.docs.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FolderTree {
    private String folderId;
    private String name;
    private LocalDateTime createdAt;
    private long pageCount;
    private long childCount;
    private List<FolderTree> children = new ArrayList<>();
    private List<Page> documents;

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public long getPageCount() {
        return pageCount;
    }

    public void setPageCount(long pageCount) {
        this.pageCount = pageCount;
    }

    public long getChildCount() {
        return childCount;
    }

    public void setChildCount(long childCount) {
        this.childCount = childCount;
    }

    public List<FolderTree> getChildren() {
        return children;
    }

    public void setChildren(List<FolderTree> children) {
        this.children = children;
    }


    public List<Page> getDocuments() {
        return documents;
    }

    public void setDocuments(List<Page> documents) {
        this.documents = documents;
    }
}
