package com.sophia.phd;

public class FileResponse {
    private String message;
    private Integer status;
    private String link; // 添加文件路径字段

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    @Override
    public String toString() {
        return "FileResponse{" +
                "message='" + message + '\'' +
                ", status=" + status +
                ", link='" + link + '\'' +
                '}';
    }
}