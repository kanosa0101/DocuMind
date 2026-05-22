package com.javaee.fileservice.registry;

import com.javaee.fileservice.chain.ProcessStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 处理步骤注册表 (v3.0)
 * 管理所有处理步骤的生命周期
 */
@Component
public class ProcessStepRegistry {

    private final Map<String, ProcessStep> steps = new LinkedHashMap<>();

    @Autowired
    public ProcessStepRegistry(List<ProcessStep> stepList) {
        // 自动注入所有实现了ProcessStep接口的步骤
        for (ProcessStep step : stepList) {
            registerStep(step);
        }
    }

    /**
     * 注册处理步骤
     */
    public void registerStep(ProcessStep step) {
        steps.put(step.getStepName(), step);
    }

    /**
     * 获取按顺序排列的所有步骤
     */
    public List<ProcessStep> getOrderedSteps() {
        return steps.values().stream()
                .sorted(Comparator.comparingInt(ProcessStep::getOrder))
                .collect(Collectors.toList());
    }

    /**
     * 获取指定步骤
     */
    public ProcessStep getStep(String stepName) {
        return steps.get(stepName);
    }

    /**
     * 获取步骤数量
     */
    public int getStepCount() {
        return steps.size();
    }
}