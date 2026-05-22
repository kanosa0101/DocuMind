package com.javaee.fileservice.chain;

/**
 * 错误恢复策略枚举 (v3.0)
 * 定义处理步骤失败时的恢复行为
 */
public enum RecoveryAction {
    /** 跳过当前步骤，继续执行后续步骤 */
    SKIP,
    /** 停止整个处理链 */
    STOP,
    /** 重试当前步骤（最多3次） */
    RETRY,
    /** 继续执行，忽略错误 */
    CONTINUE
}