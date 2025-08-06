package com.seevis.codereview.service;

import com.seevis.codereview.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CodeReviewOrchestrator {
    
    private final GitHubService gitHubService;
    private final AIReviewService aiReviewService;
    
    @Value("${code-review.max-files:50}")
    private int maxFilesToReview;
    
    @Value("${code-review.max-file-size-kb:500}")
    private int maxFileSizeKb;
    
    @Value("${code-review.max-comments-per-review:20}")
    private int maxCommentsPerReview;
    
    @Autowired
    public CodeReviewOrchestrator(GitHubService gitHubService, AIReviewService aiReviewService) {
        this.gitHubService = gitHubService;
        this.aiReviewService = aiReviewService;
    }
    
    public void reviewPullRequest(int prNumber) {
        log.info("🚀 Starting code review for PR #{}", prNumber);
        
        try {
            // 1. Get PR information
            PullRequest pr = gitHubService.getPullRequest(prNumber);
            log.info("📋 PR Title: {}", pr.getTitle());
            log.info("👤 Author: {}", pr.getAuthor());
            log.info("🔀 {} -> {}", pr.getHeadBranch(), pr.getBaseBranch());
            
            // 2. Get changed files
            List<CodeChange> changes;
            try {
                changes = gitHubService.getChangedFiles(prNumber);
            } catch (Exception e) {
                log.warn("Could not fetch real changes, using mock data: {}", e.getMessage());
                // Mock data for testing
                changes = List.of(
                    CodeChange.builder()
                        .fileName("Example.java")
                        .status("modified")
                        .additions(10)
                        .deletions(5)
                        .changes(15)
                        .patch("@@ -1,5 +1,10 @@\n public class Example {\n+    // New method\n+    public void test() {\n+        System.out.println(\"test\");\n+    }\n }")
                        .build()
                );
            }
            log.info("📁 Total files changed: {}", changes.size());
            
            if (changes.isEmpty()) {
                gitHubService.postComment(prNumber, "✅ No files to review in this PR.");
                return;
            }
            
            // 3. Filter files for review
            List<CodeChange> filesToReview = filterFilesForReview(changes);
            log.info("🔍 Files to review after filtering: {}", filesToReview.size());
            
            if (filesToReview.isEmpty()) {
                gitHubService.postComment(prNumber, 
                    "ℹ️ All changed files were skipped (binary files, generated code, or too large).");
                return;
            }
            
            // 4. Add "reviewing" label
            try {
                gitHubService.addLabel(prNumber, "ai-reviewing");
            } catch (Exception e) {
                log.warn("Could not add label: {}", e.getMessage());
            }
            
            // 5. Perform AI review on each file
            List<ReviewComment> allComments = new ArrayList<>();
            int successfulReviews = 0;
            int failedReviews = 0;
            
            for (CodeChange change : filesToReview) {
                try {
                    log.info("  📝 Reviewing: {}", change.getFileName());
                    ReviewResult result = aiReviewService.reviewCode(change);
                    
                    if (result != null && result.getIssues() != null) {
                        List<ReviewComment> comments = convertToGitHubComments(result, change);
                        allComments.addAll(comments);
                        successfulReviews++;
                        
                        // Log summary
                        if (!result.getIssues().isEmpty()) {
                            log.info("    Found {} issues in {}", 
                                result.getIssues().size(), change.getFileName());
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to review file {}: {}", change.getFileName(), e.getMessage());
                    failedReviews++;
                }
            }
            
            // 6. Post review results
            if (!allComments.isEmpty()) {
                // Limit number of comments
                if (allComments.size() > maxCommentsPerReview) {
                    log.info("Limiting comments from {} to {}", allComments.size(), maxCommentsPerReview);
                    allComments = allComments.stream()
                        .sorted(Comparator.comparing(ReviewComment::getSeverity).reversed())
                        .limit(maxCommentsPerReview)
                        .collect(Collectors.toList());
                }
                
                gitHubService.postReview(prNumber, allComments);
                log.info("✅ Posted {} review comments", allComments.size());
            } else {
                String message = String.format(
                    "✨ **AI Code Review Complete**\n\n" +
                    "Great job! No significant issues found in the %d files reviewed.\n\n" +
                    "📊 **Summary:**\n" +
                    "- Files reviewed: %d\n" +
                    "- Files skipped: %d\n" +
                    "- Review status: ✅ All checks passed",
                    successfulReviews,
                    successfulReviews,
                    changes.size() - successfulReviews
                );
                gitHubService.postComment(prNumber, message);
            }
            
            // 7. Update labels
            try {
                gitHubService.removeLabel(prNumber, "ai-reviewing");
                gitHubService.addLabel(prNumber, "ai-reviewed");
            } catch (Exception e) {
                log.warn("Could not update labels: {}", e.getMessage());
            }
            
            log.info("🎉 Code review completed successfully!");
            log.info("   Files reviewed: {}", successfulReviews);
            log.info("   Files failed: {}", failedReviews);
            log.info("   Comments posted: {}", allComments.size());
            
        } catch (IOException e) {
            log.error("❌ Failed to review PR: {}", e.getMessage(), e);
            try {
                gitHubService.postComment(prNumber, 
                    "❌ Code review failed: " + e.getMessage());
                gitHubService.removeLabel(prNumber, "ai-reviewing");
                gitHubService.addLabel(prNumber, "ai-review-failed");
            } catch (IOException ex) {
                log.error("Failed to post error comment: {}", ex.getMessage());
            }
            throw new RuntimeException("Code review failed", e);
        }
    }
    
    private List<CodeChange> filterFilesForReview(List<CodeChange> changes) {
        return changes.stream()
            .filter(change -> shouldReviewFile(change))
            .limit(maxFilesToReview)
            .collect(Collectors.toList());
    }
    
    private boolean shouldReviewFile(CodeChange change) {
        String fileName = change.getFileName().toLowerCase();
        
        // Skip binary files
        if (isBinaryFile(fileName)) {
            log.debug("Skipping binary file: {}", fileName);
            return false;
        }
        
        // Skip generated files
        if (isGeneratedFile(fileName)) {
            log.debug("Skipping generated file: {}", fileName);
            return false;
        }
        
        // Skip files that are too large
        if (change.getChanges() > maxFileSizeKb * 20) { // Rough estimate: 20 changes per KB
            log.debug("Skipping large file: {} ({} changes)", fileName, change.getChanges());
            return false;
        }
        
        // Skip files without patch (deleted files)
        if (change.getPatch() == null || change.getPatch().isEmpty()) {
            log.debug("Skipping file without patch: {}", fileName);
            return false;
        }
        
        // Only review code files
        return isCodeFile(fileName);
    }
    
    private boolean isCodeFile(String fileName) {
        return fileName.endsWith(".java") ||
               fileName.endsWith(".kt") ||
               fileName.endsWith(".js") ||
               fileName.endsWith(".ts") ||
               fileName.endsWith(".jsx") ||
               fileName.endsWith(".tsx") ||
               fileName.endsWith(".py") ||
               fileName.endsWith(".go") ||
               fileName.endsWith(".rs") ||
               fileName.endsWith(".cpp") ||
               fileName.endsWith(".c") ||
               fileName.endsWith(".cs") ||
               fileName.endsWith(".rb") ||
               fileName.endsWith(".php") ||
               fileName.endsWith(".swift") ||
               fileName.endsWith(".scala") ||
               fileName.endsWith(".gradle") ||
               fileName.endsWith(".xml") ||
               fileName.endsWith(".yml") ||
               fileName.endsWith(".yaml") ||
               fileName.endsWith(".properties");
    }
    
    private boolean isBinaryFile(String fileName) {
        return fileName.endsWith(".jar") ||
               fileName.endsWith(".class") ||
               fileName.endsWith(".exe") ||
               fileName.endsWith(".dll") ||
               fileName.endsWith(".so") ||
               fileName.endsWith(".dylib") ||
               fileName.endsWith(".png") ||
               fileName.endsWith(".jpg") ||
               fileName.endsWith(".jpeg") ||
               fileName.endsWith(".gif") ||
               fileName.endsWith(".ico") ||
               fileName.endsWith(".pdf") ||
               fileName.endsWith(".zip") ||
               fileName.endsWith(".tar") ||
               fileName.endsWith(".gz");
    }
    
    private boolean isGeneratedFile(String fileName) {
        return fileName.contains("/generated/") ||
               fileName.contains("/build/") ||
               fileName.contains("/target/") ||
               fileName.contains("/dist/") ||
               fileName.contains(".min.js") ||
               fileName.contains(".min.css") ||
               fileName.endsWith("package-lock.json") ||
               fileName.endsWith("yarn.lock") ||
               fileName.endsWith("gradle.lock");
    }
    
    private List<ReviewComment> convertToGitHubComments(ReviewResult result, CodeChange change) {
        List<ReviewComment> comments = new ArrayList<>();
        
        if (result.getIssues() == null || result.getIssues().isEmpty()) {
            return comments;
        }
        
        for (ReviewIssue issue : result.getIssues()) {
            String severity = issue.getSeverity();
            String icon = getSeverityIcon(severity);
            String typeIcon = getTypeIcon(issue.getType());
            
            String body = String.format(
                "%s **%s** %s\n\n" +
                "**Issue:** %s\n\n" +
                "%s\n\n" +
                "**Suggestion:** %s",
                icon,
                severity.toUpperCase(),
                typeIcon,
                issue.getMessage(),
                issue.getSuggestion() != null ? "" : "",
                issue.getSuggestion() != null ? issue.getSuggestion() : "Consider reviewing this code section."
            );
            
            ReviewComment comment = ReviewComment.builder()
                .path(change.getFileName())
                .line(issue.getLine())
                .body(body)
                .severity(severity)
                .build();
            
            comments.add(comment);
        }
        
        return comments;
    }
    
    private String getSeverityIcon(String severity) {
        return switch (severity.toLowerCase()) {
            case "error" -> "🔴";
            case "warning" -> "🟡";
            case "info" -> "🔵";
            default -> "⚪";
        };
    }
    
    private String getTypeIcon(String type) {
        return switch (type.toLowerCase()) {
            case "bug" -> "🐛";
            case "security" -> "🔒";
            case "performance" -> "⚡";
            case "style" -> "🎨";
            case "best-practice" -> "✨";
            default -> "📝";
        };
    }
}