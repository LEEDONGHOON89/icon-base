package com.itmasters.icon.api.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ErrorResponse {
    private final int status;
    private final String message;
    @Schema(description = "상세 메세지")
    private List<String> trace;
    private final LocalDateTime timestamp;

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(int status, String message, List<String> trace) {
        this.status = status;
        this.message = message;
        this.trace = trace;
        this.timestamp = LocalDateTime.now();
    }
}
