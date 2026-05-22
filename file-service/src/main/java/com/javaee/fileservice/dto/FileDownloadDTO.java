package com.javaee.fileservice.dto;

/**
 * 文件下载请求参数
 */
public class FileDownloadDTO {

    private String fileId;

    private boolean preview;

    private String range;

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public boolean isPreview() {
        return preview;
    }

    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }
}
