package com.seevis.codereview.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PullRequest {
    private int number;
    private String title;
    private String description;
    private String author;
    private String baseBranch;
    private String headBranch;
    private String state;
    private Date createdAt;
    private Date updatedAt;
}