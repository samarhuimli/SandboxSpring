package com.example.sandboxspring;

import lombok.Data;
import java.util.List;

@Data
public class ExecutionGroupDTO {
    private Long scriptId;
    private String title;
    private String createdBy;
    private List<ExecutionDTO> executions;
}
