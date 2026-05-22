package com.javaee.fileservice.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 处理状态机 (v3.0)
 * 管理文件处理状态的转换
 */
@Component
public class ProcessStateMachine {

    private static final Logger log = LoggerFactory.getLogger(ProcessStateMachine.class);

    private final Map<ProcessState, Set<ProcessState>> allowedTransitions = new HashMap<>();

    public ProcessStateMachine() {
        // 定义允许的状态转换
        allowedTransitions.put(ProcessState.PENDING, Set.of(ProcessState.PARSE));
        allowedTransitions.put(ProcessState.PARSE, Set.of(ProcessState.AI_PROCESSING, ProcessState.FAILED));
        allowedTransitions.put(ProcessState.AI_PROCESSING, Set.of(ProcessState.CLASSIFYING, ProcessState.FAILED));
        allowedTransitions.put(ProcessState.CLASSIFYING, Set.of(ProcessState.INDEXING, ProcessState.FAILED));
        allowedTransitions.put(ProcessState.INDEXING, Set.of(ProcessState.COMPLETED, ProcessState.FAILED));
        allowedTransitions.put(ProcessState.FAILED, Set.of(ProcessState.PENDING)); // 重试
        allowedTransitions.put(ProcessState.COMPLETED, Set.of(ProcessState.NEED_UPDATE)); // 内容更新
        allowedTransitions.put(ProcessState.NEED_UPDATE, Set.of(ProcessState.PARSE)); // 重新处理
    }

    /**
     * 尝试状态转换
     * @param from 当前状态
     * @param to 目标状态
     * @return true表示转换成功
     */
    public boolean canTransition(ProcessState from, ProcessState to) {
        Set<ProcessState> allowed = allowedTransitions.get(from);
        if (allowed == null) {
            log.warn("未定义从 {} 的状态转换", from);
            return false;
        }
        boolean result = allowed.contains(to);
        if (!result) {
            log.warn("不允许从 {} 转换到 {}", from, to);
        }
        return result;
    }

    /**
     * 执行状态转换
     * @param from 当前状态
     * @param to 目标状态
     * @return 目标状态（如果转换成功）或当前状态（如果转换失败）
     */
    public ProcessState transition(ProcessState from, ProcessState to) {
        if (canTransition(from, to)) {
            log.info("状态转换: {} → {}", from, to);
            return to;
        }
        log.warn("状态转换失败: {} → {}", from, to);
        return from;
    }

    /**
     * 获取状态描述
     */
    public String getDescription(ProcessState state) {
        return state != null ? state.getDescription() : "未知";
    }

    /**
     * 判断是否为终态
     */
    public boolean isTerminal(ProcessState state) {
        return state == ProcessState.COMPLETED || state == ProcessState.FAILED;
    }
}