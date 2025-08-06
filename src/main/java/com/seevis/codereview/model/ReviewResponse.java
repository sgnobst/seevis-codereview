package com.seevis.codereview.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "코드 리뷰 응답")
public class ReviewResponse {
    
    @Schema(description = "Pull Request 번호", example = "123")
    private int prNumber;
    
    @Schema(description = "리뷰 상태", example = "completed", allowableValues = {"pending", "in_progress", "completed", "failed"})
    private String status;
    
    @Schema(description = "응답 메시지", example = "코드 리뷰가 성공적으로 완료되었습니다.")
    private String message;
    
    @Schema(description = "타임스탬프", example = "1701234567890")
    private long timestamp;
    
    @Schema(description = "리뷰 상세 정보", required = false)
    private ReviewDetails details;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "리뷰 상세 정보")
    public static class ReviewDetails {
        
        @Schema(description = "리뷰된 파일 수", example = "15")
        private int filesReviewed;
        
        @Schema(description = "발견된 이슈 수", example = "3")
        private int issuesFound;
        
        @Schema(description = "생성된 코멘트 수", example = "5")
        private int commentsPosted;
        
        @Schema(description = "리뷰 소요 시간 (ms)", example = "12500")
        private long reviewDurationMs;
    }
}