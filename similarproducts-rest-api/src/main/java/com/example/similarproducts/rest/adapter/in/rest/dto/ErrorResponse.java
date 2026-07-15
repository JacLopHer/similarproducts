package com.example.similarproducts.rest.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Standard error response DTO for REST endpoints.
 * Provides consistent error structure across the API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    String error,
    String message,
    Instant timestamp,
    String path,
    Integer status
) {
    /**
     * Creates an ErrorResponse with common fields.
     *
     * @param error Error code identifier
     * @param message Human-readable error message
     * @return ErrorResponse with current timestamp
     */
    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, Instant.now(), null, null);
    }

    /**
     * Creates a complete ErrorResponse with all details.
     *
     * @param error Error code identifier
     * @param message Human-readable error message
     * @param path Request path that failed
     * @param status HTTP status code
     * @return Complete ErrorResponse
     */
    public static ErrorResponse of(String error, String message, String path, Integer status) {
        return new ErrorResponse(error, message, Instant.now(), path, status);
    }
}

