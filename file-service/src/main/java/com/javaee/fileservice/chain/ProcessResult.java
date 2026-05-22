package com.javaee.fileservice.chain;

/**
 * 处理步骤执行结果 (v3.0)
 */
public class ProcessResult {

    private boolean success;
    private String message;
    private Exception error;

    public static ProcessResult success() {
        ProcessResult result = new ProcessResult();
        result.setSuccess(true);
        return result;
    }

    public static ProcessResult success(String message) {
        ProcessResult result = new ProcessResult();
        result.setSuccess(true);
        result.setMessage(message);
        return result;
    }

    public static ProcessResult fail(String message) {
        ProcessResult result = new ProcessResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    public static ProcessResult fail(Exception error) {
        ProcessResult result = new ProcessResult();
        result.setSuccess(false);
        result.setError(error);
        result.setMessage(error.getMessage());
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Exception getError() {
        return error;
    }

    public void setError(Exception error) {
        this.error = error;
    }
}