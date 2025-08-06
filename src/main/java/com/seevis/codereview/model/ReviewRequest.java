package com.seevis.codereview.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "코드 리뷰 요청")
public class ReviewRequest {
    
    @NotBlank(message = "Repository는 필수입니다")
    @Pattern(regexp = "^[\\w-]+/[\\w.-]+$", message = "Repository 형식이 올바르지 않습니다 (owner/repo)")
    @Schema(description = "GitHub Repository (owner/repo 형식)", example = "octocat/hello-world")
    private String repository;
    
    @Min(value = 1, message = "PR 번호는 1 이상이어야 합니다")
    @Schema(description = "Pull Request 번호", example = "123")
    private int prNumber;
    
    @Schema(description = "리뷰 옵션", required = false)
    private ReviewOptions options;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "리뷰 옵션")
    public static class ReviewOptions {
        
        @Schema(description = "최대 리뷰 파일 수", example = "50")
        private Integer maxFiles;
        
        @Schema(description = "최대 코멘트 수", example = "20")
        private Integer maxComments;
        
        @Schema(description = "특정 파일 패턴만 리뷰", example = "*.java")
        private String filePattern;
        
        @Schema(description = "AI 모델", example = "gpt-4")
        private String aiModel;
    }
}