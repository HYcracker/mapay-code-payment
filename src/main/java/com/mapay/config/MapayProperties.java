package com.mapay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mpay")
public class MapayProperties {
    private long orderExpireSeconds = 180;
    private String uploadDir = "./data/uploads";
    private String baseUrl = "http://localhost:8080";
    private String docPassword = "";
    private String adminInitPassword = "";

    public long getOrderExpireSeconds() {
        return orderExpireSeconds;
    }

    public void setOrderExpireSeconds(long orderExpireSeconds) {
        this.orderExpireSeconds = orderExpireSeconds;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDocPassword() {
        return docPassword;
    }

    public void setDocPassword(String docPassword) {
        this.docPassword = docPassword;
    }

    public String getAdminInitPassword() {
        return adminInitPassword;
    }

    public void setAdminInitPassword(String adminInitPassword) {
        this.adminInitPassword = adminInitPassword;
    }
}
