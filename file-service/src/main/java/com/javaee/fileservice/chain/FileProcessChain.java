package com.javaee.fileservice.chain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaee.fileservice.entity.FileInfo;
import com.javaee.fileservice.mapper.FileInfoMapper;
import com.javaee.fileservice.registry.ProcessStepRegistry;
import com.javaee.fileservice.state.ProcessState;
import com.javaee.fileservice.state.ProcessStateMachine;
import com.javaee.fileservice.websocket.ProgressWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件处理责任链 (v3.0)
 * 协调各个处理步骤有序执行
 */
@Component
public class FileProcessChain {

    private static final Logger log = LoggerFactory.getLogger(FileProcessChain.class);
    private static final int MAX_RETRY_COUNT = 3;

    @Autowired
    private ProcessStepRegistry stepRegistry;

    @Autowired
    private ProcessStateMachine stateMachine;

    @Autowired(required = false)
    private ProgressWebSocketHandler websocketHandler;

    @Autowired
    private FileInfoMapper fileInfoMapper;

    /**
     * 执行处理链
     * @param context 处理上下文
     */
    public void execute(ProcessContext context) {
        List<ProcessStep> steps = stepRegistry.getOrderedSteps();
        context.setTotalSteps(steps.size());
        context.setStartTime(System.currentTimeMillis());
        context.setProcessStatus(ProcessState.PENDING);

        log.info("开始执行处理链: fileUuid={}, userId={}, totalSteps={}",
                context.getFileUuid(), context.getUserId(), steps.size());

        for (int i = 0; i < steps.size(); i++) {
            ProcessStep step = steps.get(i);
            context.setCurrentStepIndex(i);
            context.setCurrentStep(step.getStepName());

            if (step.shouldSkip(context)) {
                log.info("跳过步骤: {}", step.getStepName());
                continue;
            }

            // 推送进度
            pushProgress(context);

            try {
                ProcessResult result = executeStepWithRetry(step, context);
                if (!result.isSuccess()) {
                    RecoveryAction action = step.onError(context, result.getError());
                    if (handleRecovery(action, context, step)) {
                        continue; // 跳过，继续后续步骤
                    } else {
                        break; // 停止处理链
                    }
                }
            } catch (Exception e) {
                log.error("步骤执行异常: {} - {}", step.getStepName(), e.getMessage(), e);
                RecoveryAction action = step.onError(context, e);
                context.addError(step.getStepName() + ": " + e.getMessage());
                if (!handleRecovery(action, context, step)) {
                    break;
                }
            }
        }

        // 处理完成
        context.setEndTime(System.currentTimeMillis());
        if (context.getErrors().isEmpty()) {
            context.setProcessStatus(ProcessState.COMPLETED);
        } else {
            context.setProcessStatus(ProcessState.FAILED);
        }

        // 持久化处理状态到数据库
        updateFileInfoProcessStatus(context);

        // 推送完成通知
        pushComplete(context);

        log.info("处理链执行完成: fileUuid={}, status={}, duration={}ms, errors={}",
                context.getFileUuid(), context.getProcessStatus(),
                context.getDuration(), context.getErrors().size());
    }

    /**
     * 执行步骤（带重试）
     */
    private ProcessResult executeStepWithRetry(ProcessStep step, ProcessContext context) {
        int retryCount = 0;
        ProcessResult result;

        do {
            result = step.execute(context);
            if (result.isSuccess()) {
                return result;
            }
            retryCount++;
            if (retryCount < MAX_RETRY_COUNT) {
                log.warn("步骤执行失败，准备重试: {} (retry {})", step.getStepName(), retryCount);
                context.setRetryCount(retryCount);
                try {
                    Thread.sleep(1000); // 重试间隔
                } catch (InterruptedException ignored) {}
            }
        } while (retryCount < MAX_RETRY_COUNT);

        return result;
    }

    /**
     * 处理恢复策略
     * @return true表示继续执行后续步骤
     */
    private boolean handleRecovery(RecoveryAction action, ProcessContext context, ProcessStep step) {
        switch (action) {
            case SKIP:
                log.info("跳过失败步骤: {}", step.getStepName());
                return true;
            case CONTINUE:
                log.info("忽略错误继续: {}", step.getStepName());
                return true;
            case STOP:
                log.info("停止处理链: {}", step.getStepName());
                stateMachine.transition(context.getProcessStatus(), ProcessState.FAILED);
                context.setProcessStatus(ProcessState.FAILED);
                return false;
            case RETRY:
                // 重试已在executeStepWithRetry中处理，如果到这里说明已超过最大重试次数
                log.warn("超过最大重试次数: {}", step.getStepName());
                return false;
            default:
                return false;
        }
    }

    /**
     * 更新FileInfo的处理状态到数据库
     */
    private void updateFileInfoProcessStatus(ProcessContext context) {
        if (context.getFileUuid() == null) {
            return;
        }
        try {
            FileInfo fileInfo = fileInfoMapper.selectByFileUuid(context.getFileUuid());
            if (fileInfo != null) {
                fileInfo.setProcessStatus(context.getProcessStatus().name());
                fileInfo.setRetryCount(context.getRetryCount());
                fileInfo.setUpdateTime(LocalDateTime.now());

                // v3.0 fix: 无论成功或失败，都保存已生成的AI结果
                if (context.getSummary() != null) {
                    fileInfo.setSummary(context.getSummary());
                }
                if (context.getKeywords() != null && !context.getKeywords().isEmpty()) {
                    // keywords存储为JSON字符串
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        fileInfo.setKeywords(mapper.writeValueAsString(context.getKeywords()));
                    } catch (JsonProcessingException e) {
                        log.warn("关键词JSON转换失败: {}", e.getMessage());
                    }
                }
                if (context.getCategory() != null) {
                    fileInfo.setCategory(context.getCategory());
                }

                if (context.getProcessStatus() == ProcessState.COMPLETED) {
                    fileInfo.setProcessTime(LocalDateTime.now());
                    fileInfo.setIndexed(context.isIndexed());
                    fileInfo.setVectorId(context.getVectorId());
                }
                fileInfoMapper.updateById(fileInfo);
                log.info("更新FileInfo处理状态: fileUuid={}, processStatus={}, hasSummary={}, hasKeywords={}",
                        context.getFileUuid(), context.getProcessStatus().name(),
                        context.getSummary() != null, context.getKeywords() != null && !context.getKeywords().isEmpty());
            }
        } catch (Exception e) {
            log.error("更新FileInfo处理状态失败: fileUuid={}", context.getFileUuid(), e);
        }
    }

    /**
     * 推送进度到前端
     */
    private void pushProgress(ProcessContext context) {
        if (websocketHandler != null) {
            websocketHandler.pushProgress(context.getUserId(), context);
        }
    }

    /**
     * 推送完成通知
     */
    private void pushComplete(ProcessContext context) {
        if (websocketHandler != null) {
            websocketHandler.pushComplete(context.getUserId(), context.getFileUuid(),
                    context.getProcessStatus() == ProcessState.COMPLETED);
        }
    }
}