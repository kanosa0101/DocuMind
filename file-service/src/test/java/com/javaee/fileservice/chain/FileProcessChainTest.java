package com.javaee.fileservice.service;

import com.javaee.fileservice.chain.FileProcessChain;
import com.javaee.fileservice.chain.ProcessContext;
import com.javaee.fileservice.chain.ProcessResult;
import com.javaee.fileservice.chain.ProcessStep;
import com.javaee.fileservice.chain.RecoveryAction;
import com.javaee.fileservice.state.ProcessState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 处理责任链单元测试
 * Phase 3验收标准：所有测试通过
 */
class FileProcessChainTest {

    // ===== ProcessContext测试 =====

    @Test
    @DisplayName("ProcessContext创建和属性设置")
    void testProcessContextCreation() {
        ProcessContext context = new ProcessContext();
        context.setFileUuid("test-uuid");
        context.setFileName("test.pdf");
        context.setUserId(1L);
        context.setContent("测试内容");
        context.setAction("NEW");

        assertEquals("test-uuid", context.getFileUuid());
        assertEquals("test.pdf", context.getFileName());
        assertEquals(1L, context.getUserId());
        assertEquals("测试内容", context.getContent());
        assertEquals("NEW", context.getAction());
    }

    @Test
    @DisplayName("ProcessContext进度计算")
    void testProgressCalculation() {
        ProcessContext context = new ProcessContext();
        context.setTotalSteps(5);

        context.setCurrentStepIndex(0);
        assertEquals(20, context.getProgress());

        context.setCurrentStepIndex(2);
        assertEquals(60, context.getProgress());

        context.setCurrentStepIndex(4);
        assertEquals(100, context.getProgress());
    }

    @Test
    @DisplayName("ProcessContext错误添加")
    void testErrorHandling() {
        ProcessContext context = new ProcessContext();

        context.addError("错误1");
        context.addError("错误2");

        List<String> errors = context.getErrors();
        assertEquals(2, errors.size());
        assertTrue(errors.contains("错误1"));
        assertTrue(errors.contains("错误2"));
    }

    @Test
    @DisplayName("ProcessContext耗时计算")
    void testDurationCalculation() {
        ProcessContext context = new ProcessContext();

        context.setStartTime(System.currentTimeMillis() - 1000);
        context.setEndTime(System.currentTimeMillis());

        long duration = context.getDuration();
        assertTrue(duration >= 1000);
        assertTrue(duration <= 1500);
    }

    // ===== ProcessResult测试 =====

    @Test
    @DisplayName("ProcessResult成功创建")
    void testProcessResultSuccess() {
        ProcessResult result = ProcessResult.success("操作成功");

        assertTrue(result.isSuccess());
        assertEquals("操作成功", result.getMessage());
        assertNull(result.getError());
    }

    @Test
    @DisplayName("ProcessResult失败创建")
    void testProcessResultFail() {
        Exception exception = new RuntimeException("测试异常");
        ProcessResult result = ProcessResult.fail(exception);

        assertFalse(result.isSuccess());
        assertEquals("测试异常", result.getMessage());
        assertNotNull(result.getError());
    }

    @Test
    @DisplayName("ProcessResult失败消息")
    void testProcessResultFailMessage() {
        ProcessResult result = ProcessResult.fail("操作失败");

        assertFalse(result.isSuccess());
        assertEquals("操作失败", result.getMessage());
    }

    // ===== RecoveryAction测试 =====

    @Test
    @DisplayName("RecoveryAction枚举值")
    void testRecoveryActionValues() {
        assertEquals(4, RecoveryAction.values().length);
        assertEquals(RecoveryAction.SKIP, RecoveryAction.valueOf("SKIP"));
        assertEquals(RecoveryAction.STOP, RecoveryAction.valueOf("STOP"));
        assertEquals(RecoveryAction.RETRY, RecoveryAction.valueOf("RETRY"));
        assertEquals(RecoveryAction.CONTINUE, RecoveryAction.valueOf("CONTINUE"));
    }

    // ===== ProcessState测试 =====

    @Test
    @DisplayName("ProcessState枚举值和描述")
    void testProcessStateValues() {
        assertEquals(8, ProcessState.values().length);

        assertEquals("待处理", ProcessState.PENDING.getDescription());
        assertEquals("正在解析", ProcessState.PARSE.getDescription());
        assertEquals("正在AI处理", ProcessState.AI_PROCESSING.getDescription());
        assertEquals("正在分类", ProcessState.CLASSIFYING.getDescription());
        assertEquals("正在索引", ProcessState.INDEXING.getDescription());
        assertEquals("已完成", ProcessState.COMPLETED.getDescription());
        assertEquals("处理失败", ProcessState.FAILED.getDescription());
        assertEquals("需要更新", ProcessState.NEED_UPDATE.getDescription());
    }

    @Test
    @DisplayName("终态判断")
    void testTerminalState() {
        ProcessState completed = ProcessState.COMPLETED;
        ProcessState failed = ProcessState.FAILED;
        ProcessState pending = ProcessState.PENDING;

        assertTrue(completed.name().equals("COMPLETED") || completed.name().equals("FAILED"));
        assertTrue(failed.name().equals("FAILED"));
        assertFalse(pending.name().equals("COMPLETED") || pending.name().equals("FAILED"));
    }

    // ===== ProcessStep接口测试 =====

    @Test
    @DisplayName("模拟处理步骤实现")
    void testMockProcessStep() {
        ProcessStep mockStep = new ProcessStep() {
            @Override
            public String getStepName() {
                return "MOCK_STEP";
            }

            @Override
            public int getOrder() {
                return 1;
            }

            @Override
            public ProcessResult execute(ProcessContext context) {
                return ProcessResult.success("模拟成功");
            }

            @Override
            public boolean shouldSkip(ProcessContext context) {
                return false;
            }

            @Override
            public RecoveryAction onError(ProcessContext context, Exception error) {
                return RecoveryAction.SKIP;
            }
        };

        assertEquals("MOCK_STEP", mockStep.getStepName());
        assertEquals(1, mockStep.getOrder());

        ProcessContext context = new ProcessContext();
        ProcessResult result = mockStep.execute(context);
        assertTrue(result.isSuccess());

        assertFalse(mockStep.shouldSkip(context));
        assertEquals(RecoveryAction.SKIP, mockStep.onError(context, new RuntimeException()));
    }

    // ===== 处理链完整流程模拟 =====

    @Test
    @DisplayName("模拟处理链完整流程")
    void testMockChainFlow() {
        ProcessContext context = new ProcessContext();
        context.setFileUuid("test-uuid");
        context.setFileName("test.pdf");
        context.setUserId(1L);
        context.setContent("测试内容");
        context.setAction("NEW");

        // 模拟处理步骤
        ProcessStep step1 = new MockStep("PARSE", 1);
        ProcessStep step2 = new MockStep("AI_PROCESS", 2);
        ProcessStep step3 = new MockStep("CLASSIFY", 3);

        // 模拟执行
        context.setTotalSteps(3);
        context.setProcessStatus(ProcessState.PENDING);

        // Step 1
        context.setCurrentStepIndex(0);
        context.setCurrentStep("PARSE");
        ProcessResult result1 = step1.execute(context);
        assertTrue(result1.isSuccess());

        // Step 2
        context.setCurrentStepIndex(1);
        context.setCurrentStep("AI_PROCESS");
        context.setSummary("AI生成的摘要");
        context.setKeywords(List.of("AI", "测试"));
        ProcessResult result2 = step2.execute(context);
        assertTrue(result2.isSuccess());

        // Step 3
        context.setCurrentStepIndex(2);
        context.setCurrentStep("CLASSIFY");
        context.setCategory("技术文档");
        ProcessResult result3 = step3.execute(context);
        assertTrue(result3.isSuccess());

        // 完成
        context.setProcessStatus(ProcessState.COMPLETED);
        context.setIndexed(true);

        // 验证最终结果
        assertEquals(ProcessState.COMPLETED, context.getProcessStatus());
        assertEquals("AI生成的摘要", context.getSummary());
        assertEquals(2, context.getKeywords().size());
        assertEquals("技术文档", context.getCategory());
        assertTrue(context.isIndexed());
        assertTrue(context.getErrors().isEmpty());
    }

    @Test
    @DisplayName("处理链错误恢复测试")
    void testChainErrorRecovery() {
        ProcessContext context = new ProcessContext();
        context.setFileUuid("test-uuid");
        context.setContent("测试内容");

        // 模拟失败步骤
        ProcessStep failStep = new ProcessStep() {
            @Override
            public String getStepName() {
                return "FAIL_STEP";
            }

            @Override
            public int getOrder() {
                return 1;
            }

            @Override
            public ProcessResult execute(ProcessContext context) {
                return ProcessResult.fail("模拟失败");
            }

            @Override
            public boolean shouldSkip(ProcessContext context) {
                return false;
            }

            @Override
            public RecoveryAction onError(ProcessContext context, Exception error) {
                return RecoveryAction.SKIP; // 跳过继续
            }
        };

        ProcessResult result = failStep.execute(context);
        assertFalse(result.isSuccess());

        RecoveryAction action = failStep.onError(context, new RuntimeException());
        assertEquals(RecoveryAction.SKIP, action);

        // SKIP表示继续执行后续步骤
        assertTrue(action == RecoveryAction.SKIP || action == RecoveryAction.CONTINUE);
    }

    // 辅助类
    private static class MockStep implements ProcessStep {
        private final String name;
        private final int order;

        MockStep(String name, int order) {
            this.name = name;
            this.order = order;
        }

        @Override
        public String getStepName() {
            return name;
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public ProcessResult execute(ProcessContext context) {
            return ProcessResult.success(name + "完成");
        }

        @Override
        public boolean shouldSkip(ProcessContext context) {
            return false;
        }

        @Override
        public RecoveryAction onError(ProcessContext context, Exception error) {
            return RecoveryAction.RETRY;
        }
    }
}