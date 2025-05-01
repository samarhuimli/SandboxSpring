package com.example.sandboxspring.service;

import com.example.sandboxspring.entity.ExecutionResult;
import com.example.sandboxspring.entity.Script;
import com.example.sandboxspring.repository.ExecutionResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final ExecutionResultRepository executionResultRepository;
    private final ThreadPoolTaskExecutor taskExecutor;

    public ExecutionResult executeScript(Script script) {
        ExecutionResult result = buildInitialResult(script);

        try {
            String output = taskExecutor.submit(() -> executeScriptContent(script))
                    .get(30, TimeUnit.SECONDS);
            handleSuccess(result, output);
        } catch (TimeoutException e) {
            handleTimeout(result);
        } catch (Exception e) {
            handleFailure(result, e);
        }

        return executionResultRepository.save(result);
    }

    private ExecutionResult buildInitialResult(Script script) {
        return ExecutionResult.builder()
                .script(script)
                .executedAt(LocalDateTime.now())
                .status(ExecutionResult.ExecutionStatus.PENDING)
                .build();
    }

    private void handleSuccess(ExecutionResult result, String output) {
        result.setOutput(output);
        result.setStatus(ExecutionResult.ExecutionStatus.SUCCESS);
        log.info("Execution succeeded for script ID: {}", result.getScript().getId());
    }

    private void handleTimeout(ExecutionResult result) {
        result.setError("Execution timeout after 30 seconds");
        result.setStatus(ExecutionResult.ExecutionStatus.TIMEOUT);
        log.warn("Execution timeout for script ID: {}", result.getScript().getId());
    }

    private void handleFailure(ExecutionResult result, Exception e) {
        result.setError(e.getClass().getSimpleName() + ": " + e.getMessage());
        result.setStatus(ExecutionResult.ExecutionStatus.FAILED);
        log.error("Execution failed for script ID: {}", result.getScript().getId(), e);
    }

    private String executeScriptContent(Script script) throws Exception {
        return switch (script.getType()) {
            case PYTHON -> executePythonScript(script.getContent());
            case R -> executeRScript(script.getContent());
            case SQL -> executeSQLScript(script.getContent());
            default -> throw new IllegalArgumentException("Unsupported type: " + script.getType());
        };
    }

    private String executePythonScript(String content) {
        return "Python execution result:\n" + content;
    }

    private String executeRScript(String content) {
        return "R execution result:\n" + content;
    }

    private String executeSQLScript(String content) {
        return "SQL execution result:\n" + content;
    }
}