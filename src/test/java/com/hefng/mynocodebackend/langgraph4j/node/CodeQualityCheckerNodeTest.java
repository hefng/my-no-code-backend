package com.hefng.mynocodebackend.langgraph4j.node;

import com.hefng.mynocodebackend.langgraph4j.entity.QualityResult;
import com.hefng.mynocodebackend.langgraph4j.state.WorkflowContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class CodeQualityCheckerNodeTest {

    @Test
    void shouldIncrementRetryWhenInvalidAndBelowMaxRetries() {
        WorkflowContext context = WorkflowContext.builder()
                .codeQualityRetryCount(0)
                .codeQualityMaxRetries(2)
                .build();
        QualityResult result = QualityResult.builder()
                .isValid(false)
                .errors(List.of("Quality issue found"))
                .build();

        CodeQualityCheckerNode.updateRetryState(context, result);

        Assertions.assertEquals(1, context.getCodeQualityRetryCount());
        Assertions.assertFalse(context.getCodeQualityRetryExhausted());
        Assertions.assertNull(context.getErrorMessage());
    }

    @Test
    void shouldMarkRetryExhaustedWhenReachingMaxRetries() {
        WorkflowContext context = WorkflowContext.builder()
                .codeQualityRetryCount(1)
                .codeQualityMaxRetries(2)
                .build();
        QualityResult result = QualityResult.builder()
                .isValid(false)
                .errors(List.of("Still failing after fix"))
                .build();

        CodeQualityCheckerNode.updateRetryState(context, result);

        Assertions.assertEquals(2, context.getCodeQualityRetryCount());
        Assertions.assertTrue(context.getCodeQualityRetryExhausted());
        Assertions.assertTrue(context.getErrorMessage().contains("retries exhausted"));
        Assertions.assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("retries exhausted")));
    }

    @Test
    void shouldKeepRetryCountWhenQualityPasses() {
        WorkflowContext context = WorkflowContext.builder()
                .codeQualityRetryCount(1)
                .codeQualityMaxRetries(2)
                .codeQualityRetryExhausted(true)
                .build();
        QualityResult result = QualityResult.builder()
                .isValid(true)
                .build();

        CodeQualityCheckerNode.updateRetryState(context, result);

        Assertions.assertEquals(1, context.getCodeQualityRetryCount());
        Assertions.assertFalse(context.getCodeQualityRetryExhausted());
    }
}
