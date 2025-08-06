package com.seevis.codereview.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewIssue {
    private Integer line; // line number in the file (can be null for general issues)
    private String type; // bug, performance, security, style, best-practice
    private String severity; // info, warning, error
    private String message; // description of the issue
    private String suggestion; // how to fix it
}