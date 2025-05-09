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
import org.renjin.script.RenjinScriptEngineFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionResultRepository executionResultRepository;
    private final ScriptRepository scriptRepository;

    @PostMapping("/executeR")
    @CrossOrigin(origins = "http://localhost:4200")
    public ResponseEntity<ExecutionResultDTO> executeRCode(
            @RequestBody Map<String, Object> request) {
        String code = (String) request.get("code");
        Long scriptId = request.get("scriptId") != null ? Long.valueOf(request.get("scriptId").toString()) : null;

        // Validation
        ExecutionResultDTO errorDTO = new ExecutionResultDTO();
        if (code == null || code.trim().isEmpty()) {
            errorDTO.setError("Erreur: Le code R est vide ou absent.");
            errorDTO.setStatus("FAILED");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDTO);
        }

        System.out.println("Script R reçu : " + code + ", scriptId: " + scriptId);

        ExecutionResultDTO resultDTO = new ExecutionResultDTO();
        try {
            RenjinScriptEngineFactory factory = new RenjinScriptEngineFactory();
            ScriptEngine engine = factory.getScriptEngine();

            if (engine == null) {
                System.out.println("Erreur : Renjin ScriptEngine est null");
                resultDTO.setError("Erreur: Échec de l'initialisation de Renjin ScriptEngine");
                resultDTO.setStatus("FAILED");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resultDTO);
            }

            System.out.println("Exécution du script R...");
            Object result = engine.eval(code);
            String output = result != null ? result.toString() : "No output";
            System.out.println("Résultat du script R : " + output);

            resultDTO.setOutput(output);
            resultDTO.setStatus("SUCCESS");
        } catch (ScriptException e) {
            System.out.println("Erreur d'exécution R : " + e.getMessage());
            e.printStackTrace();
            resultDTO.setError("Erreur d'exécution R: " + e.getMessage());
            resultDTO.setStatus("FAILED");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resultDTO);
        } catch (Exception e) {
            System.out.println("Erreur serveur inattendue : " + e.getMessage());
            e.printStackTrace();
            resultDTO.setError("Erreur serveur: " + e.getMessage());
            resultDTO.setStatus("FAILED");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resultDTO);
        }

        resultDTO.setExecutionTime(String.valueOf(System.currentTimeMillis()));
        resultDTO.setScriptId(scriptId);

        // Ne pas sauvegarder automatiquement ici
        return ResponseEntity.ok(resultDTO);
    }

    @PostMapping("/save")
    public ResponseEntity<ExecutionResultDTO> saveExecutionResult(
            @RequestBody ExecutionResultDTO resultDTO) {
        ExecutionResult result = new ExecutionResult();
        result.setOutput(resultDTO.getOutput());
        result.setError(resultDTO.getError());
        Long executionTime = resultDTO.getExecutionTime() != null ? Long.parseLong(resultDTO.getExecutionTime()) : 0L;
        result.setExecutionTime(String.valueOf(executionTime));
        result.setStatus(resultDTO.getStatusEnum());
        result.setExecutedAt(LocalDateTime.now());

        if (resultDTO.getScriptId() != null) {
            Script script = scriptRepository.findById(resultDTO.getScriptId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Script non trouvé avec l'ID: " + resultDTO.getScriptId()));
            result.setScript(script);
        } else {
            result.setScript(null);
        }

        ExecutionResult saved = executionResultRepository.save(result);
        return ResponseEntity.ok(convertToDTO(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExecution(@PathVariable Long id) {
        ExecutionResult execution = executionResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Exécution non trouvée avec l'ID: " + id));
        executionResultRepository.delete(execution);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/grouped")
    public List<ExecutionGroupDTO> getGroupedExecutions() {
        return scriptRepository.findAll().stream()
                .map(script -> {
                    ExecutionGroupDTO dto = new ExecutionGroupDTO();
                    dto.setScriptId(script.getId());
                    dto.setTitle(script.getTitle());
                    dto.setCreatedBy(script.getCreatedBy());

                    List<ExecutionDTO> executions = script.getExecutionResults().stream()
                            .map(this::convertToExecutionDTO)
                            .collect(Collectors.toList());

                    dto.setExecutions(executions);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private ExecutionResultDTO convertToDTO(ExecutionResult entity) {
        ExecutionResultDTO dto = new ExecutionResultDTO();
        dto.setScriptId(entity.getScript() != null ? entity.getScript().getId() : null);
        dto.setOutput(entity.getOutput());
        dto.setError(entity.getError());
        dto.setStatus(entity.getStatus().name());
        dto.setExecutionTime(String.valueOf(entity.getExecutionTime()));
        return dto;
    }

    private ExecutionDTO convertToExecutionDTO(ExecutionResult entity) {
        ExecutionDTO dto = new ExecutionDTO();
        dto.setId(entity.getId());
        dto.setOutput(entity.getOutput());
        dto.setError(entity.getError());
        dto.setSuccess(entity.isSuccessful());
        dto.setExecutedAt(entity.getExecutedAt());
        return dto;
    }
}