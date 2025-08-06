package com.seevis.codereview;

import com.seevis.codereview.service.CodeReviewOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@Slf4j
public class GitHubCodeReviewerApplication implements CommandLineRunner {
    
    @Autowired
    private CodeReviewOrchestrator orchestrator;
    
    @Value("${PR_NUMBER:0}")
    private int pullRequestNumber;
    
    @Value("${GITHUB_REPOSITORY:}")
    private String repository;
    
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(GitHubCodeReviewerApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE); // 웹 서버 비활성화
        app.run(args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        // Web 모드에서는 실행하지 않음
        if (System.getProperty("web.mode") != null) {
            return;
        }
        
        log.info("=========================================================");
        log.info("  GitHub Code Reviewer - Powered by AI");
        log.info("=========================================================");
        log.info("  Repository: {}", repository);
        log.info("  PR Number: {}", pullRequestNumber);
        log.info("=========================================================");
        
        if (pullRequestNumber > 0) {
            try {
                log.info("🔍 Starting code review for PR #{}...", pullRequestNumber);
                orchestrator.reviewPullRequest(pullRequestNumber);
                log.info("✅ Code review completed successfully!");
            } catch (Exception e) {
                log.error("❌ Code review failed: {}", e.getMessage(), e);
                System.exit(1);
            }
        } else {
            log.warn("⚠️ No PR number provided. Please set PR_NUMBER environment variable.");
            log.info("Usage: PR_NUMBER=123 ./gradlew runReview");
        }
        
        log.info("👋 Shutting down...");
        System.exit(0);
    }
}