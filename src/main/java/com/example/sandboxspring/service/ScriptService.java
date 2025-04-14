package com.example.sandboxspring.service;

import com.example.sandboxspring.entity.ExecutionResult;
import com.example.sandboxspring.entity.Script;
import com.example.sandboxspring.repository.ScriptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScriptService {

    private final ScriptRepository scriptRepository;
    private final ExecutionService executionService;

    public List<Script> getAllScripts() {
        return scriptRepository.findAll();
    }

    public Optional<Script> getScriptById(Long id) {
        return scriptRepository.findById(id);
    }

    public Script createScript(Script script) {
        script.setCreatedAt(LocalDateTime.now());
        return scriptRepository.save(script);
    }

    public Script updateScript(Long id, Script scriptDetails) {
        return scriptRepository.findById(id)
                .map(script -> {
                    script.setTitle(scriptDetails.getTitle());
                    script.setContent(scriptDetails.getContent());
                    script.setType(scriptDetails.getType());
                    script.setCreatedBy(scriptDetails.getCreatedBy());

                    return scriptRepository.save(script);
                })
                .orElseThrow(() -> new RuntimeException("Script not found with id " + id));
    }

    public void deleteScript(Long id) {
        scriptRepository.deleteById(id);
    }

    public ExecutionResult executeScript(Long scriptId) {
        Script script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("Script not found with id " + scriptId));

        return executionService.executeScript(script);
    }
}
