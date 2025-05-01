package com.example.sandboxspring;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ExecutionDTO {
    private String output;
    private String error;
    private boolean success;
    private LocalDateTime executedAt;
}
