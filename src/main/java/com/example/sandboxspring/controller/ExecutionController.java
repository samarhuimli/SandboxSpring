package com.example.sandboxspring.controller;

import com.example.sandboxspring.ExecutionDTO;
import com.example.sandboxspring.ExecutionGroupDTO;
import com.example.sandboxspring.ExecutionResultDTO;
import com.example.sandboxspring.entity.ExecutionResult;
import com.example.sandboxspring.entity.Script;
import com.example.sandboxspring.exception.ResourceNotFoundException;
import com.example.sandboxspring.repository.ExecutionResultRepository;
import com.example.sandboxspring.repository.ScriptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionResultRepository executionResultRepository;
    private final ScriptRepository scriptRepository;

    @PostMapping("/save")
    public ResponseEntity<ExecutionResult> saveExecutionResult(
            @RequestBody ExecutionResultDTO resultDTO) {

        ExecutionResult result = ExecutionResult.builder()
                .output(resultDTO.getOutput())
                .error(resultDTO.getError())
                .executionTime(resultDTO.getExecutionTime())
                .status(resultDTO.getStatusEnum())
                .build();

        if (resultDTO.getScriptId() != null) {
            Script script = scriptRepository.findById(resultDTO.getScriptId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Script non trouvé avec l'ID: " + resultDTO.getScriptId()));
            result.setScript(script);
        }

        return ResponseEntity.ok(executionResultRepository.save(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExecutionResult> getExecutionResult(@PathVariable Long id) {
        return ResponseEntity.ok(executionResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Résultat non trouvé avec l'ID: " + id)));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExecution(@PathVariable Long id) {
        // Vérifie d'abord si l'exécution existe
        ExecutionResult execution = executionResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Exécution non trouvée avec l'ID: " + id));

        // Supprime l'exécution
        executionResultRepository.delete(execution);

        return ResponseEntity.noContent().build();
    }
    @GetMapping("/grouped")
    public List<ExecutionGroupDTO> getGroupedExecutions() {
        List<Script> scripts = scriptRepository.findAll();
        List<ExecutionGroupDTO> grouped = new ArrayList<>();

        for (Script script : scripts) {
            ExecutionGroupDTO groupDTO = new ExecutionGroupDTO();
            groupDTO.setScriptId(script.getId());
            groupDTO.setTitle(script.getTitle());
            groupDTO.setCreatedBy(script.getCreatedBy());

            List<ExecutionDTO> executions = script.getExecutionResults().stream()
                    .map(exec -> {
                        ExecutionDTO dto = new ExecutionDTO();
                        dto.setOutput(exec.getOutput());
                        dto.setError(exec.getError());
                        dto.setSuccess(exec.isSuccessful());
                        dto.setExecutedAt(exec.getExecutedAt());
                        return dto;
                    }).collect(Collectors.toList());

            groupDTO.setExecutions(executions);
            grouped.add(groupDTO);
        }

        return grouped;
    }
}