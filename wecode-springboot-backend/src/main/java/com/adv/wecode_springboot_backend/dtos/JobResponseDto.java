package com.adv.wecode_springboot_backend.dtos;

public class JobResponseDto {
    private String jobId;

    public JobResponseDto(String jobId) {
        this.jobId = jobId;
    }

    // Getter and Setter
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }
}
