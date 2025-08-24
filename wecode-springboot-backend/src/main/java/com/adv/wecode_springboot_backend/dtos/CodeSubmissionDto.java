package com.adv.wecode_springboot_backend.dtos;



/**
 * DTO for incoming code submission requests.
 */
public class CodeSubmissionDto {

    private String code;

    private String language;

    // Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
