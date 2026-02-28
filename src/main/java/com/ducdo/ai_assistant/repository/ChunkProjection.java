package com.ducdo.ai_assistant.repository;

public interface ChunkProjection {

    String getContent();
    java.util.UUID getDocumentId();
    Integer getPageNumber();
    String getDocumentName();
}
