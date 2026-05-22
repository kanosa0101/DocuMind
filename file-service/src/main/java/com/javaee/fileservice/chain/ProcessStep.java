package com.javaee.fileservice.chain;

/**
 * 处理步骤接口 (v3.0)
 * 每个处理步骤需实现此接口
 */
public interface ProcessStep {

    /**
     * 获取步骤名称
     */
    String getStepName();

    /**
     * 获取步骤执行顺序
     */
    int getOrder();

    /**
     * 执行处理步骤
     * @param context 处理上下文
     * @return 执行结果
     */
    ProcessResult execute(ProcessContext context);

    /**
     * 判断是否应该跳过此步骤
     * @param context 处理上下文
     * @return true表示跳过
     */
    boolean shouldSkip(ProcessContext context);

    /**
     * 错误处理回调
     * @param context 处理上下文
     * @param error 异常
     * @return 恢复策略
     */
    RecoveryAction onError(ProcessContext context, Exception error);
}