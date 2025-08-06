package com.seevis.codereview.controller;

import com.seevis.codereview.model.PullRequest;
import com.seevis.codereview.model.ReviewRequest;
import com.seevis.codereview.model.ReviewResponse;
import com.seevis.codereview.service.CodeReviewOrchestrator;
import com.seevis.codereview.service.GitHubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/review")
@Tag(name = "Code Review", description = "GitHub PR 코드 리뷰 API")
@Validated
@Slf4j
public class CodeReviewController {
    
    private final CodeReviewOrchestrator orchestrator;
    private final GitHubService gitHubService;
    
    @Autowired
    public CodeReviewController(CodeReviewOrchestrator orchestrator, GitHubService gitHubService) {
        this.orchestrator = orchestrator;
        this.gitHubService = gitHubService;
    }
    
    @PostMapping("/pr/{prNumber}")
    @Operation(
        summary = "PR 코드 리뷰 실행",
        description = "지정된 Pull Request에 대해 AI 코드 리뷰를 실행합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "리뷰 성공",
            content = @Content(schema = @Schema(implementation = ReviewResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "404", description = "PR을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ReviewResponse> reviewPullRequest(
            @Parameter(description = "Pull Request 번호", required = true, example = "123")
            @PathVariable @Min(1) int prNumber) {
        
        log.info("Received review request for PR #{}", prNumber);
        
        try {
            orchestrator.reviewPullRequest(prNumber);
            
            ReviewResponse response = ReviewResponse.builder()
                .prNumber(prNumber)
                .status("completed")
                .message("코드 리뷰가 성공적으로 완료되었습니다.")
                .timestamp(System.currentTimeMillis())
                .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to review PR #{}: {}", prNumber, e.getMessage(), e);
            
            ReviewResponse errorResponse = ReviewResponse.builder()
                .prNumber(prNumber)
                .status("failed")
                .message("리뷰 실패: " + e.getMessage())
                .timestamp(System.currentTimeMillis())
                .build();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @PostMapping("/custom")
    @Operation(
        summary = "커스텀 코드 리뷰 실행",
        description = "Repository와 PR 번호를 지정하여 코드 리뷰를 실행합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "리뷰 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ReviewResponse> reviewCustom(@Valid @RequestBody ReviewRequest request) {
        log.info("Received custom review request for {}, PR #{}", 
            request.getRepository(), request.getPrNumber());
        
        // 여기서는 간단한 응답만 반환 (실제 구현 시 repository 설정 변경 필요)
        ReviewResponse response = ReviewResponse.builder()
            .prNumber(request.getPrNumber())
            .status("accepted")
            .message(String.format("리뷰 요청이 접수되었습니다: %s PR #%d", 
                request.getRepository(), request.getPrNumber()))
            .timestamp(System.currentTimeMillis())
            .build();
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/pr/{prNumber}")
    @Operation(
        summary = "PR 정보 조회",
        description = "Pull Request의 기본 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "PR을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<PullRequest> getPullRequestInfo(
            @Parameter(description = "Pull Request 번호", required = true)
            @PathVariable @Min(1) int prNumber) {
        
        try {
            PullRequest pr = gitHubService.getPullRequest(prNumber);
            return ResponseEntity.ok(pr);
        } catch (Exception e) {
            log.error("Failed to get PR info for #{}: {}", prNumber, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    @GetMapping("/health")
    @Operation(
        summary = "헬스 체크",
        description = "서비스 상태를 확인합니다."
    )
    @ApiResponse(responseCode = "200", description = "서비스 정상")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "GitHub Code Reviewer");
        health.put("version", "1.0.0");
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }
    
    @PostMapping("/webhook")
    @Operation(
        summary = "GitHub Webhook 수신",
        description = "GitHub에서 보내는 Webhook 이벤트를 처리합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook 처리 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestHeader("X-GitHub-Event") String event,
            @RequestBody Map<String, Object> payload) {
        
        log.info("Received GitHub webhook event: {}", event);
        
        Map<String, String> response = new HashMap<>();
        
        if ("pull_request".equals(event)) {
            String action = (String) payload.get("action");
            if ("opened".equals(action) || "synchronize".equals(action)) {
                Map<String, Object> pullRequest = (Map<String, Object>) payload.get("pull_request");
                Integer prNumber = (Integer) pullRequest.get("number");
                
                log.info("Processing PR #{} with action: {}", prNumber, action);
                
                // 비동기로 리뷰 실행 (실제 구현 시)
                response.put("status", "accepted");
                response.put("message", "Review queued for PR #" + prNumber);
            } else {
                response.put("status", "ignored");
                response.put("message", "Action not supported: " + action);
            }
        } else {
            response.put("status", "ignored");
            response.put("message", "Event not supported: " + event);
        }
        
        return ResponseEntity.ok(response);
    }
}