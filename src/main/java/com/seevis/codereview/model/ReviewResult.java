package com.seevis.codereview.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResult {
    private String fileName;
    private String summary;
    private String severity; // overall severity: info, warning, error
    private List<ReviewIssue> issues;
    private List<String> positives; // positive feedback
}