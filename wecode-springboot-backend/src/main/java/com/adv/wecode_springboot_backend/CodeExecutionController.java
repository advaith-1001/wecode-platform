package com.adv.wecode_springboot_backend;

import com.adv.wecode_springboot_backend.dtos.CodeSubmissionDto;
import com.adv.wecode_springboot_backend.dtos.JobResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for handling code execution requests.
 * It receives code submissions, queues them in Redis, and provides
 * an endpoint to poll for results.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class CodeExecutionController {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public CodeExecutionController(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Receives a code submission, generates a unique job ID, and pushes it
     * to a Redis queue for processing by a worker.
     *
     * @param codeSubmissionDto DTO containing the code and language.
     * @return A response entity with the generated job ID.
     */
    @PostMapping("/run")
    public ResponseEntity<JobResponseDto> runCode(@RequestBody CodeSubmissionDto codeSubmissionDto) {
        String jobId = UUID.randomUUID().toString();
        try {
            // Create the payload for the worker
            Map<String, String> jobPayload = Map.of(
                    "jobId", jobId,
                    "code", codeSubmissionDto.getCode(),
                    "language", codeSubmissionDto.getLanguage()
            );

            String jobPayloadJson = objectMapper.writeValueAsString(jobPayload);

            // Set the initial status of the job in a Redis hash
            stringRedisTemplate.opsForHash().put("job:" + jobId, "status", "pending");

            // Push the job to the Redis list (queue)
            System.out.println("Pushing job " + jobId + " to Redis queue: " + jobPayloadJson);
            Long queueSize = stringRedisTemplate.opsForList().leftPush("code-queue", jobPayloadJson);
            System.out.println("Queue size after push: " + queueSize);

            return new ResponseEntity<>(new JobResponseDto(jobId), HttpStatus.ACCEPTED);
        } catch (JsonProcessingException e) {
            // Handle potential JSON processing errors
            System.err.println("Error creating JSON payload for job " + jobId + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Allows clients to poll for the results of a code execution job.
     *
     * @param jobId The unique ID of the job.
     * @return A map containing the job's status and output.
     */
    @GetMapping("/results/{jobId}")
    public ResponseEntity<Map<Object, Object>> getResults(@PathVariable String jobId) {
        Map<Object, Object> jobData = stringRedisTemplate.opsForHash().entries("job:" + jobId);

        if (jobData == null || jobData.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(jobData);
    }
}
