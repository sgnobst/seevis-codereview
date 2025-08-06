package com.seevis.codereview.service;

import com.seevis.codereview.model.CodeChange;
import com.seevis.codereview.model.PullRequest;
import com.seevis.codereview.model.ReviewComment;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class GitHubService {
    
    private GitHub github;
    private String repository;
    
    @Value("${GITHUB_TOKEN:}")
    private String githubToken;
    
    @Value("${GITHUB_REPOSITORY:}")
    private String repositoryName;
    
    @Value("${github.auth.type:ACTIONS}")
    private String authType;
    
    @PostConstruct
    public void initialize() throws IOException {
        log.info("Initializing GitHub Service with auth type: {}", authType);
        
        // Allow dummy tokens for local development/testing with Swagger
        if (githubToken == null || githubToken.isEmpty() || githubToken.equals("your-token-here")) {
            log.warn("GITHUB_TOKEN is not properly configured. Using mock mode for testing.");
            this.repository = repositoryName != null && !repositoryName.isEmpty() ? repositoryName : "owner/repo";
            return;
        }
        
        if (repositoryName == null || repositoryName.isEmpty()) {
            log.warn("GITHUB_REPOSITORY is not set, using default: owner/repo");
            repositoryName = "owner/repo";
        }
        
        this.repository = repositoryName;
        
        // GitHub 연결 초기화
        this.github = new GitHubBuilder()
            .withOAuthToken(githubToken)
            .build();
            
        // 연결 테스트
        try {
            github.checkApiUrlValidity();
            log.info("✅ Successfully connected to GitHub");
            
            // Repository 접근 테스트
            GHRepository repo = github.getRepository(repository);
            log.info("✅ Successfully accessed repository: {}", repo.getFullName());
        } catch (IOException e) {
            log.error("Failed to connect to GitHub: {}", e.getMessage());
            throw e;
        }
    }
    
    public PullRequest getPullRequest(int number) throws IOException {
        log.debug("Fetching PR #{}", number);
        
        if (github == null) {
            // Mock response for testing
            return PullRequest.builder()
                .number(number)
                .title("Mock PR Title")
                .description("This is a mock PR for testing")
                .author("test-user")
                .baseBranch("main")
                .headBranch("feature-branch")
                .state("open")
                .build();
        }
        
        GHRepository repo = github.getRepository(repository);
        GHPullRequest ghPr = repo.getPullRequest(number);
        
        return PullRequest.builder()
            .number(number)
            .title(ghPr.getTitle())
            .description(ghPr.getBody())
            .author(ghPr.getUser().getLogin())
            .baseBranch(ghPr.getBase().getRef())
            .headBranch(ghPr.getHead().getRef())
            .state(ghPr.getState().toString())
            .createdAt(ghPr.getCreatedAt())
            .updatedAt(ghPr.getUpdatedAt())
            .build();
    }
    
    public List<CodeChange> getChangedFiles(int prNumber) throws IOException {
        log.debug("Fetching changed files for PR #{}", prNumber);
        
        if (github == null) {
            // Mock response for testing
            return List.of(
                CodeChange.builder()
                    .fileName("MockFile.java")
                    .status("modified")
                    .additions(20)
                    .deletions(10)
                    .changes(30)
                    .patch("@@ -1,10 +1,20 @@\n Mock patch content")
                    .build()
            );
        }
        
        GHRepository repo = github.getRepository(repository);
        GHPullRequest pr = repo.getPullRequest(prNumber);
        
        return StreamSupport.stream(pr.listFiles().spliterator(), false)
            .map(file -> {
                try {
                    return CodeChange.builder()
                        .fileName(file.getFilename())
                        .status(file.getStatus())
                        .additions(file.getAdditions())
                        .deletions(file.getDeletions())
                        .changes(file.getChanges())
                        .patch(file.getPatch())
                        .rawUrl(file.getRawUrl() != null ? file.getRawUrl().toString() : null)
                        .blobUrl(file.getBlobUrl() != null ? file.getBlobUrl().toString() : null)
                        .build();
                } catch (Exception e) {
                    log.error("Error processing file: {}", file.getFilename(), e);
                    return null;
                }
            })
            .filter(change -> change != null)
            .collect(Collectors.toList());
    }
    
    public void postReview(int prNumber, List<ReviewComment> comments) throws IOException {
        if (comments == null || comments.isEmpty()) {
            log.info("No comments to post for PR #{}", prNumber);
            return;
        }
        
        if (github == null) {
            log.info("Mock mode: Would post {} review comments to PR #{}", comments.size(), prNumber);
            return;
        }
        
        log.info("Posting {} review comments to PR #{}", comments.size(), prNumber);
        GHRepository repo = github.getRepository(repository);
        GHPullRequest pr = repo.getPullRequest(prNumber);
        
        // Create review with comments
        GHPullRequestReviewBuilder review = pr.createReview()
            .body("🤖 **AI Code Review Complete**\n\n" +
                  "I've analyzed your code and provided feedback below. " +
                  "Please review the comments and make necessary adjustments.")
            .event(GHPullRequestReviewEvent.COMMENT);
        
        for (ReviewComment comment : comments) {
            if (comment.getLine() != null && comment.getLine() > 0) {
                review.comment(comment.getBody(), comment.getPath(), comment.getLine());
            }
        }
        
        review.create();
        log.info("✅ Successfully posted review to PR #{}", prNumber);
    }
    
    public void postComment(int prNumber, String comment) throws IOException {
        log.info("Posting comment to PR #{}", prNumber);
        
        if (github == null) {
            log.info("Mock mode: Would post comment to PR #{}: {}", prNumber, comment);
            return;
        }
        
        GHRepository repo = github.getRepository(repository);
        GHPullRequest pr = repo.getPullRequest(prNumber);
        
        pr.comment(comment);
        log.info("✅ Successfully posted comment to PR #{}", prNumber);
    }
    
    public void addLabel(int prNumber, String label) throws IOException {
        log.info("Adding label '{}' to PR #{}", label, prNumber);
        
        if (github == null) {
            log.info("Mock mode: Would add label '{}' to PR #{}", label, prNumber);
            return;
        }
        
        GHRepository repo = github.getRepository(repository);
        GHPullRequest pr = repo.getPullRequest(prNumber);
        
        pr.addLabels(label);
    }
    
    public void removeLabel(int prNumber, String label) throws IOException {
        log.info("Removing label '{}' from PR #{}", label, prNumber);
        
        if (github == null) {
            log.info("Mock mode: Would remove label '{}' from PR #{}", label, prNumber);
            return;
        }
        
        GHRepository repo = github.getRepository(repository);
        GHPullRequest pr = repo.getPullRequest(prNumber);
        
        pr.removeLabel(label);
    }
}