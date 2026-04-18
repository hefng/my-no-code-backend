package com.hefng.mynocodebackend.langgraph4j;

import com.hefng.mynocodebackend.ai.model.CodegenTypeEnum;
import com.hefng.mynocodebackend.langgraph4j.entity.QualityResult;
import com.hefng.mynocodebackend.langgraph4j.state.WorkflowContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CodeQualityRetryRoutingTest {

    @Test
    void shouldRouteToRetryWhenInvalidAndBelowMaxRetries() {
        CodeGenWorkflow workflow = new CodeGenWorkflow();
        WorkflowContext context = WorkflowContext.builder()
                .generationType(CodegenTypeEnum.HTML)
                .codeQualityRetryCount(1)
                .codeQualityMaxRetries(2)
                .qualityResult(QualityResult.builder().isValid(false).build())
                .build();

        String route = workflow.resolveCodeQualityRoute(context);

        Assertions.assertEquals(CodeGenWorkflow.RETRY_ROUTE, route);
    }

    @Test
    void shouldRouteToEndWhenRetryExhausted() {
        CodeGenWorkflow workflow = new CodeGenWorkflow();
        WorkflowContext context = WorkflowContext.builder()
                .generationType(CodegenTypeEnum.HTML)
                .codeQualityRetryCount(2)
                .codeQualityMaxRetries(2)
                .qualityResult(QualityResult.builder().isValid(false).build())
                .build();

        String route = workflow.resolveCodeQualityRoute(context);

        Assertions.assertEquals(CodeGenWorkflow.RETRY_EXHAUSTED_ROUTE, route);
    }

    @Test
    void shouldAllowCustomMaxRetriesOverrideDefaultValueTwo() {
        WorkflowContext defaultContext = WorkflowContext.builder().build();
        Assertions.assertEquals(2, defaultContext.getCodeQualityMaxRetries());

        CodeGenWorkflow workflow = new CodeGenWorkflow();
        WorkflowContext customContext = WorkflowContext.builder()
                .generationType(CodegenTypeEnum.HTML)
                .codeQualityRetryCount(4)
                .codeQualityMaxRetries(5)
                .qualityResult(QualityResult.builder().isValid(false).build())
                .build();

        String routeBeforeLimit = workflow.resolveCodeQualityRoute(customContext);
        Assertions.assertEquals(CodeGenWorkflow.RETRY_ROUTE, routeBeforeLimit);

        customContext.setCodeQualityRetryCount(5);
        String routeAtLimit = workflow.resolveCodeQualityRoute(customContext);
        Assertions.assertEquals(CodeGenWorkflow.RETRY_EXHAUSTED_ROUTE, routeAtLimit);
    }
}
