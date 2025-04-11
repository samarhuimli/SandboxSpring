package com.example.sandboxspring.service;

import com.example.sandboxspring.entity.ExecutionResult;
import com.example.sandboxspring.entity.Script;
import com.example.sandboxspring.repository.ExecutionResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ExecutionService {
    private final ExecutionResultRepository executionResultRepository;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    public ExecutionResult executeScript(Script script) {
        log.info("Starting execution for script ID: {} (Type: {})", script.getId(), script.getType());

        ExecutionResult result = ExecutionResult.builder()
                .script(script)
                .executedAt(LocalDateTime.now())
                .status(ExecutionResult.ExecutionStatus.PENDING)
                .build();

        try {
            Future<String> future = taskExecutor.submit(() -> executeScriptContent(script));
            String output = future.get(30, TimeUnit.SECONDS);

            result.setOutput(output);
            result.setStatus(ExecutionResult.ExecutionStatus.SUCCESS);
            log.info("Execution succeeded for script ID: {}", script.getId());

        } catch (TimeoutException e) {
            result.setError("Execution timeout after 30 seconds");
            result.setStatus(ExecutionResult.ExecutionStatus.TIMEOUT);
            log.warn("Execution timeout for script ID: {}", script.getId());

        } catch (Exception e) {
            result.setError(e.getClass().getSimpleName() + ": " + e.getMessage());
            result.setStatus(ExecutionResult.ExecutionStatus.FAILED);
            log.error("Execution failed for script ID: {} - Reason: {}",
                    script.getId(), e.getMessage());
        }

        return executionResultRepository.save(result);
    }

    private String executeScriptContent(Script script) throws Exception {
        switch (script.getType()) {
            case PYTHON:
                return executePythonScript(script.getContent());
            case R:
                return executeRScript(script.getContent());
            case SQL:
                return executeSQLScript(script.getContent());
            default:
                throw new IllegalArgumentException("Unsupported script type: " + script.getType());
        }
    }

    private String executePythonScript(String content) throws Exception {
        // Implémentation réelle à compléter
        return "Python execution result:\n" + content.substring(0, Math.min(100, content.length()));
    }

    private String executeRScript(String content) throws Exception {
        // Implémentation réelle à compléter
        return "R execution result:\n" + content.substring(0, Math.min(100, content.length()));
    }

    private String executeSQLScript(String content) throws Exception {
        // Implémentation réelle à compléter
        return "SQL execution result:\n" + content.substring(0, Math.min(100, content.length()));
    }
}