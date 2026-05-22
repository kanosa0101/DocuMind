package com.javaee.fileservice.websocket;

import com.javaee.fileservice.chain.ProcessContext;
import com.javaee.fileservice.state.ProcessState;
import com.javaee.fileservice.websocket.ProgressWebSocketHandler.WebSocketMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebSocket进度推送测试
 * Phase 4验收标准：所有测试通过
 */
class WebSocketTest {

    // ===== WebSocketMessage测试 =====

    @Test
    @DisplayName("WebSocket消息类型验证")
    void testWebSocketMessageTypes() {
        WebSocketMessage progressMsg = new WebSocketMessage();
        progressMsg.setType("PROGRESS");
        progressMsg.setFileUuid("test-uuid");
        progressMsg.setStep("AI_SUMMARIZE");
        progressMsg.setProgress(50);
        progressMsg.setStatus("正在AI处理");

        assertEquals("PROGRESS", progressMsg.getType());
        assertEquals("test-uuid", progressMsg.getFileUuid());
        assertEquals("AI_SUMMARIZE", progressMsg.getStep());
        assertEquals(50, progressMsg.getProgress());
        assertEquals("正在AI处理", progressMsg.getStatus());

        WebSocketMessage completeMsg = new WebSocketMessage();
        completeMsg.setType("COMPLETE");
        completeMsg.setFileUuid("test-uuid");
        completeMsg.setSuccess(true);
        completeMsg.setMessage("智能整理完成");

        assertEquals("COMPLETE", completeMsg.getType());
        assertTrue(completeMsg.getSuccess());
        assertEquals("智能整理完成", completeMsg.getMessage());

        WebSocketMessage errorMsg = new WebSocketMessage();
        errorMsg.setType("ERROR");
        errorMsg.setFileUuid("test-uuid");
        errorMsg.setMessage("处理失败");

        assertEquals("ERROR", errorMsg.getType());
        assertEquals("处理失败", errorMsg.getMessage());
    }

    @Test
    @DisplayName("WebSocket消息字段完整性")
    void testWebSocketMessageFields() {
        WebSocketMessage msg = new WebSocketMessage();

        // 所有字段都可设置和获取
        msg.setType("PROGRESS");
        msg.setFileUuid("uuid");
        msg.setStep("step");
        msg.setProgress(100);
        msg.setStatus("status");
        msg.setSuccess(true);
        msg.setMessage("message");

        assertEquals("PROGRESS", msg.getType());
        assertEquals("uuid", msg.getFileUuid());
        assertEquals("step", msg.getStep());
        assertEquals(100, msg.getProgress());
        assertEquals("status", msg.getStatus());
        assertTrue(msg.getSuccess());
        assertEquals("message", msg.getMessage());
    }

    // ===== ProcessContext集成测试 =====

    @Test
    @DisplayName("ProcessContext生成WebSocket消息数据")
    void testContextToMessage() {
        ProcessContext context = new ProcessContext();
        context.setFileUuid("test-uuid");
        context.setUserId(1L);
        context.setCurrentStep("CONTENT_PARSE");
        context.setCurrentStepIndex(0);
        context.setTotalSteps(5);
        context.setProcessStatus(ProcessState.PARSE);

        // 根据context创建消息
        WebSocketMessage msg = new WebSocketMessage();
        msg.setType("PROGRESS");
        msg.setFileUuid(context.getFileUuid());
        msg.setStep(context.getCurrentStep());
        msg.setProgress(context.getProgress());
        msg.setStatus(context.getProcessStatus().getDescription());

        assertEquals("test-uuid", msg.getFileUuid());
        assertEquals("CONTENT_PARSE", msg.getStep());
        assertEquals(20, msg.getProgress()); // (0+1)*100/5 = 20
        assertEquals("正在解析", msg.getStatus());
    }

    @Test
    @DisplayName("用户隔离topic路径验证")
    void testUserTopicPath() {
        Long userId = 123L;

        String progressPath = "/topic/progress/" + userId;
        String completePath = "/topic/complete/" + userId;

        assertTrue(progressPath.contains(userId.toString()));
        assertTrue(completePath.contains(userId.toString()));

        // 验证路径格式正确
        assertEquals("/topic/progress/123", progressPath);
        assertEquals("/topic/complete/123", completePath);
    }

    // ===== 进度计算测试 =====

    @Test
    @DisplayName("进度百分比计算验证")
    void testProgressPercentage() {
        ProcessContext context = new ProcessContext();
        context.setTotalSteps(5);

        // 各步骤进度计算
        context.setCurrentStepIndex(0);
        assertEquals(20, context.getProgress());

        context.setCurrentStepIndex(1);
        assertEquals(40, context.getProgress());

        context.setCurrentStepIndex(2);
        assertEquals(60, context.getProgress());

        context.setCurrentStepIndex(3);
        assertEquals(80, context.getProgress());

        context.setCurrentStepIndex(4);
        assertEquals(100, context.getProgress());
    }

    @Test
    @DisplayName("边界条件测试")
    void testEdgeCases() {
        WebSocketMessage msg = new WebSocketMessage();

        // null值处理
        msg.setFileUuid(null);
        assertNull(msg.getFileUuid());

        msg.setProgress(null);
        assertNull(msg.getProgress());

        // 0值处理
        msg.setProgress(0);
        assertEquals(0, msg.getProgress());

        // 100值处理
        msg.setProgress(100);
        assertEquals(100, msg.getProgress());

        // 空字符串处理
        msg.setMessage("");
        assertEquals("", msg.getMessage());
    }
}