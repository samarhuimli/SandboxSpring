package com.example.sandboxspring;

import com.example.sandboxspring.entity.ExecutionResult.ExecutionStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResultDTO {
    private Long scriptId;
    private String output;
    private String error;
    private String status;
    private String executionTime;

    /**
     * Convertit le statut String en Enum ExecutionStatus
     * @throws IllegalArgumentException si le statut est invalide
     */
    public ExecutionStatus getStatusEnum() {
        try {
            return ExecutionStatus.valueOf(this.status.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Statut invalide: " + this.status +
                    ". Les valeurs valides sont: " + java.util.Arrays.toString(ExecutionStatus.values()));
        }
    }
}