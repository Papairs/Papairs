package com.papairs.docs.dto.request;

public class UpdatePageRequest {
    private String content;

    public UpdatePageRequest() {
    }

    public UpdatePageRequest(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
