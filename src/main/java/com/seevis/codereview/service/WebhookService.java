package com.seevis.codereview.service;

import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Service
@Slf4j
public class WebhookService {
    
    @Autowired
    private AIReviewService aiReviewService;
    
    @Value("${github.app.id:}")
    private String appId;
    
    @Value("${github.app.private-key-path:}")
    private String privateKeyPath;
    
    @Value("${GITHUB_TOKEN:}")
    private String githubToken;
    
    @Async
    public void processCodeReview(String repoFullName, int prNumber, String installationId) {
        log.info("🚀 Starting async code review for {} PR #{}", repoFullName, prNumber);
        
        try {
            // GitHub 연결 (App 인증 또는 PAT 사용)
            GitHub github = createGitHubClient(installationId);
            
            // Repository 가져오기
            GHRepository repository = github.getRepository(repoFullName);
            
            // Pull Request 가져오기
            GHPullRequest pullRequest = repository.getPullRequest(prNumber);
            
            log.info("📋 PR Title: {}", pullRequest.getTitle());
            log.info("👤 Author: {}", pullRequest.getUser().getLogin());
            log.info("🔀 {} -> {}", pullRequest.getHead().getRef(), pullRequest.getBase().getRef());
            
            // PR에 라벨 추가 (리뷰 중)
            try {
                pullRequest.addLabels("ai-reviewing");
            } catch (Exception e) {
                log.warn("Could not add label: {}", e.getMessage());
            }
            
            // 변경된 파일 가져오기
            List<GHPullRequestFileDetail> files = pullRequest.listFiles().toList();
            log.info("📁 Files changed: {}", files.size());
            
            int totalComments = 0;
            
            // 각 파일에 대해 리뷰 실행
            for (GHPullRequestFileDetail file : files) {
                // 파일 확장자 체크
                if (!shouldReviewFile(file.getFilename())) {
                    log.debug("Skipping file: {}", file.getFilename());
                    continue;
                }
                
                log.info("🔍 Reviewing: {}", file.getFilename());
                
                // 코드 변경 사항 생성
                com.seevis.codereview.model.CodeChange codeChange = 
                    new com.seevis.codereview.model.CodeChange();
                codeChange.setFileName(file.getFilename());
                codeChange.setStatus(file.getStatus());
                codeChange.setAdditions(file.getAdditions());
                codeChange.setDeletions(file.getDeletions());
                codeChange.setPatch(file.getPatch());
                
                // AI 리뷰 실행
                com.seevis.codereview.model.ReviewResult result = 
                    aiReviewService.reviewCode(codeChange);
                
                // 리뷰 결과를 PR 코멘트로 작성
                if (result != null && result.getIssues() != null && !result.getIssues().isEmpty()) {
                    StringBuilder comment = new StringBuilder();
                    comment.append("## 🤖 AI Code Review for `").append(file.getFilename()).append("`\n\n");
                    
                    if (result.getSummary() != null && !result.getSummary().isEmpty()) {
                        comment.append("**Summary:** ").append(result.getSummary()).append("\n\n");
                    }
                    
                    comment.append("### Issues Found:\n\n");
                    
                    for (com.seevis.codereview.model.ReviewIssue issue : result.getIssues()) {
                        // 심각도에 따른 이모지
                        String emoji = "ℹ️";
                        if ("error".equals(issue.getSeverity())) {
                            emoji = "🔴";
                        } else if ("warning".equals(issue.getSeverity())) {
                            emoji = "🟡";
                        }
                        
                        comment.append(emoji).append(" **").append(issue.getType().toUpperCase())
                               .append("** ");
                        
                        if (issue.getLine() != null) {
                            comment.append("(Line ").append(issue.getLine()).append(")");
                        }
                        
                        comment.append("\n");
                        comment.append("- **Issue:** ").append(issue.getMessage()).append("\n");
                        
                        if (issue.getSuggestion() != null && !issue.getSuggestion().isEmpty()) {
                            comment.append("- **Suggestion:** ").append(issue.getSuggestion()).append("\n");
                        }
                        
                        comment.append("\n");
                    }
                    
                    // 긍정적인 피드백 추가
                    if (result.getPositives() != null && !result.getPositives().isEmpty()) {
                        comment.append("### ✅ Good Practices:\n");
                        for (String positive : result.getPositives()) {
                            comment.append("- ").append(positive).append("\n");
                        }
                    }
                    
                    // PR에 코멘트 작성
                    pullRequest.comment(comment.toString());
                    totalComments++;
                    
                    log.info("📝 Posted review comment for {}", file.getFilename());
                }
            }
            
            // 라벨 업데이트 (리뷰 완료)
            try {
                pullRequest.removeLabel("ai-reviewing");
                pullRequest.addLabels("ai-reviewed");
            } catch (Exception e) {
                log.warn("Could not update labels: {}", e.getMessage());
            }
            
            // 전체 리뷰 요약 코멘트
            if (totalComments > 0) {
                String summary = String.format(
                    "## ✅ Code Review Complete!\n\n" +
                    "I've reviewed **%d file(s)** and left **%d comment(s)**.\n\n" +
                    "Please review the feedback and make necessary improvements.\n\n" +
                    "_Powered by AI Code Reviewer with Google Gemini_ 🤖",
                    files.size(), totalComments
                );
                pullRequest.comment(summary);
            } else {
                pullRequest.comment(
                    "## ✅ Code Review Complete!\n\n" +
                    "No significant issues found. Good job! 👍\n\n" +
                    "_Powered by AI Code Reviewer with Google Gemini_ 🤖"
                );
            }
            
            log.info("✅ Code review completed for {} PR #{}", repoFullName, prNumber);
            
        } catch (Exception e) {
            log.error("❌ Error during code review for {} PR #{}", repoFullName, prNumber, e);
            
            // 에러 발생 시 PR에 코멘트 남기기
            try {
                GitHub github = createGitHubClient(installationId);
                GHRepository repository = github.getRepository(repoFullName);
                GHPullRequest pullRequest = repository.getPullRequest(prNumber);
                
                pullRequest.comment(
                    "## ❌ Code Review Failed\n\n" +
                    "An error occurred during the code review process.\n" +
                    "Error: " + e.getMessage() + "\n\n" +
                    "Please try again or contact the administrator."
                );
                
                // 라벨 정리
                try {
                    pullRequest.removeLabel("ai-reviewing");
                    pullRequest.addLabels("ai-review-failed");
                } catch (Exception labelError) {
                    log.warn("Could not update labels: {}", labelError.getMessage());
                }
                
            } catch (Exception commentError) {
                log.error("Could not post error comment", commentError);
            }
        }
    }
    
    private GitHub createGitHubClient(String installationId) throws IOException {
        // GitHub App 인증 사용 (privateKey가 있는 경우)
        if (appId != null && !appId.isEmpty() && 
            privateKeyPath != null && !privateKeyPath.isEmpty() && 
            installationId != null && !installationId.isEmpty()) {
            
            try {
                log.info("🔐 Using GitHub App authentication");
                
                // Private key 읽기
                File privateKeyFile = new File(privateKeyPath);
                String privateKey = Files.readString(privateKeyFile.toPath());
                
                // GitHub App으로 인증
                GHAppInstallationToken token = GitHub.connectUsingOAuth(appId)
                    .getApp()
                    .getInstallationById(Long.parseLong(installationId))
                    .createToken()
                    .create();
                
                return GitHub.connectUsingOAuth(token.getToken());
                
            } catch (Exception e) {
                log.warn("GitHub App authentication failed, falling back to PAT: {}", e.getMessage());
            }
        }
        
        // Personal Access Token 사용 (fallback)
        if (githubToken != null && !githubToken.isEmpty()) {
            log.info("🔑 Using Personal Access Token authentication");
            return GitHub.connectUsingOAuth(githubToken);
        }
        
        // 인증 정보가 없는 경우
        throw new IOException("No GitHub authentication configured. " +
            "Please set either GitHub App credentials or GITHUB_TOKEN");
    }
    
    private boolean shouldReviewFile(String filename) {
        // 리뷰할 파일 확장자 목록
        String[] extensions = {
            ".java", ".kt", ".js", ".ts", ".jsx", ".tsx",
            ".py", ".go", ".rs", ".cpp", ".c", ".cs",
            ".rb", ".php", ".swift", ".scala"
        };
        
        String lowerFilename = filename.toLowerCase();
        
        // 제외할 패턴
        if (lowerFilename.contains("/test/") || 
            lowerFilename.contains("/tests/") ||
            lowerFilename.contains(".min.") ||
            lowerFilename.contains("/node_modules/") ||
            lowerFilename.contains("/vendor/") ||
            lowerFilename.contains("/build/") ||
            lowerFilename.contains("/dist/")) {
            return false;
        }
        
        // 확장자 체크
        for (String ext : extensions) {
            if (lowerFilename.endsWith(ext)) {
                return true;
            }
        }
        
        return false;
    }
}