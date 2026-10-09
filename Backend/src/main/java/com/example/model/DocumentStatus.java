package com.example.model;

/**
 * Represents the lifecycle status of an uploaded document.
 */
public enum DocumentStatus {
    UPLOADING,
    PROCESSING,
    COMPLETED,
    FAILED,
    DELETING
}
