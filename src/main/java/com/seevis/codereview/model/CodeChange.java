package com.seevis.codereview.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeChange {
    private String fileName;
    private String status; // added, modified, deleted
    private int additions;
    private int deletions;
    private int changes;
    private String patch; // diff content
    private String rawUrl;
    private String blobUrl;
}