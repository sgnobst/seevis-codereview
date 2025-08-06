package com.seevis.codereview.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seevis.codereview.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
@Tag(name = "Webhook", description = "GitHub Webhook 처리")
@Slf4j
public class WebhookController {
    
    @Autowired
    private WebhookService webhookService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${github.webhook.secret:}")
    private String webhookSecret;
    
    @PostMapping("/github")
    @Operation(
        summary = "GitHub Webhook 수신",
        description = "GitHub App에서 보내는 Webhook 이벤트를 처리합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook 처리 성공"),
        @ApiResponse(responseCode = "202", description = "Webhook 처리 대기열에 추가됨"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<Map<String, Object>> handleGitHubWebhook(
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestBody String payload) {
        
        log.info("📨 Received GitHub webhook - Event: {}, Delivery: {}", eventType, deliveryId);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 서명 검증 (webhook secret이 설정된 경우)
            if (webhookSecret != null && !webhookSecret.isEmpty()) {
                if (!verifySignature(payload, signature)) {
                    log.error("❌ Invalid webhook signature");
                    response.put("status", "error");
                    response.put("message", "Invalid signature");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                }
            }
            
            // JSON 파싱
            JsonNode jsonPayload = objectMapper.readTree(payload);
            
            // 이벤트 타입별 처리
            switch (eventType) {
                case "ping":
                    response.put("status", "success");
                    response.put("message", "Pong! Webhook is configured correctly.");
                    log.info("✅ Webhook ping successful");
                    return ResponseEntity.ok(response);
                    
                case "pull_request":
                    return handlePullRequestEvent(jsonPayload);
                    
                case "pull_request_review":
                    return handlePullRequestReviewEvent(jsonPayload);
                    
                case "pull_request_review_comment":
                    return handlePullRequestReviewCommentEvent(jsonPayload);
                    
                case "installation":
                case "installation_repositories":
                    return handleInstallationEvent(jsonPayload, eventType);
                    
                default:
                    log.info("⚠️ Unhandled event type: {}", eventType);
                    response.put("status", "ignored");
                    response.put("message", "Event type not supported: " + eventType);
                    return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            log.error("❌ Error processing webhook: ", e);
            response.put("status", "error");
            response.put("message", "Error processing webhook: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    private ResponseEntity<Map<String, Object>> handlePullRequestEvent(JsonNode payload) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String action = payload.get("action").asText();
            JsonNode pullRequest = payload.get("pull_request");
            JsonNode repository = payload.get("repository");
            
            int prNumber = pullRequest.get("number").asInt();
            String repoFullName = repository.get("full_name").asText();
            String installationId = payload.path("installation").path("id").asText();
            
            log.info("🔍 PR Event - Action: {}, Repo: {}, PR: #{}", action, repoFullName, prNumber);
            
            // PR이 열리거나 업데이트될 때만 리뷰 실행
            if ("opened".equals(action) || "synchronize".equals(action) || "reopened".equals(action)) {
                // 드래프트 PR은 건너뛰기
                if (pullRequest.get("draft").asBoolean()) {
                    log.info("⏭️ Skipping draft PR #{}", prNumber);
                    response.put("status", "skipped");
                    response.put("message", "Draft PRs are not reviewed");
                    return ResponseEntity.ok(response);
                }
                
                // 비동기로 코드 리뷰 실행
                webhookService.processCodeReview(repoFullName, prNumber, installationId);
                
                response.put("status", "accepted");
                response.put("message", String.format("Code review queued for %s PR #%d", repoFullName, prNumber));
                response.put("prNumber", prNumber);
                response.put("repository", repoFullName);
                
                log.info("✅ Code review queued for {} PR #{}", repoFullName, prNumber);
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
                
            } else if ("closed".equals(action)) {
                log.info("📕 PR #{} closed", prNumber);
                response.put("status", "ignored");
                response.put("message", "PR closed");
                return ResponseEntity.ok(response);
                
            } else {
                log.info("⚠️ Unhandled PR action: {}", action);
                response.put("status", "ignored");
                response.put("message", "Action not supported: " + action);
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            log.error("Error handling pull request event", e);
            response.put("status", "error");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    private ResponseEntity<Map<String, Object>> handlePullRequestReviewEvent(JsonNode payload) {
        Map<String, Object> response = new HashMap<>();
        
        String action = payload.get("action").asText();
        int prNumber = payload.path("pull_request").path("number").asInt();
        
        log.info("📝 PR Review Event - Action: {}, PR: #{}", action, prNumber);
        
        response.put("status", "acknowledged");
        response.put("message", "Review event received");
        return ResponseEntity.ok(response);
    }
    
    private ResponseEntity<Map<String, Object>> handlePullRequestReviewCommentEvent(JsonNode payload) {
        Map<String, Object> response = new HashMap<>();
        
        String action = payload.get("action").asText();
        int prNumber = payload.path("pull_request").path("number").asInt();
        
        log.info("💬 PR Review Comment Event - Action: {}, PR: #{}", action, prNumber);
        
        response.put("status", "acknowledged");
        response.put("message", "Review comment event received");
        return ResponseEntity.ok(response);
    }
    
    private ResponseEntity<Map<String, Object>> handleInstallationEvent(JsonNode payload, String eventType) {
        Map<String, Object> response = new HashMap<>();
        
        String action = payload.get("action").asText();
        
        log.info("⚙️ Installation Event - Type: {}, Action: {}", eventType, action);
        
        if ("created".equals(action)) {
            log.info("🎉 New installation created!");
            response.put("status", "success");
            response.put("message", "Installation successful");
        } else if ("deleted".equals(action)) {
            log.info("👋 Installation removed");
            response.put("status", "success");
            response.put("message", "Installation removed");
        } else {
            response.put("status", "acknowledged");
            response.put("message", "Installation event received");
        }
        
        return ResponseEntity.ok(response);
    }
    
    private boolean verifySignature(String payload, String signature) {
        if (signature == null || !signature.startsWith("sha256=")) {
            return false;
        }
        
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes());
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            String calculatedSignature = "sha256=" + hexString.toString();
            return calculatedSignature.equals(signature);
            
        } catch (Exception e) {
            log.error("Error verifying signature", e);
            return false;
        }
    }
}