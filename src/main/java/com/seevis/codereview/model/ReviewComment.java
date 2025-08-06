package com.seevis.codereview.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewComment {
    private String path; // file path
    private Integer line; // line number in the diff
    private String body; // comment content
    private String severity; // info, warning, error
}