package com.seevis.codereview.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seevis.codereview.model.CodeChange;
import com.seevis.codereview.model.ReviewResult;
import com.seevis.codereview.model.ReviewIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
@Slf4j
public class AIReviewService {
    
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}";
    
    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    
    @Value("${GEMINI_API_KEY:}")
    private String apiKey;
    
    @Value("${gemini.model:gemini-1.5-flash}")
    private String modelName;
    
    @Value("${gemini.temperature:0.3}")
    private double temperature;
    
    @Value("${gemini.max-tokens:2048}")
    private int maxTokens;
    
    @PostConstruct
    public void initialize() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        
        if (apiKey != null && !apiKey.isEmpty() && !apiKey.equals("your-api-key-here")) {
            log.info("✅ Gemini AI service initialized with model: {}", modelName);
        } else {
            log.warn("⚠️ GEMINI_API_KEY not configured. AI review will use mock responses.");
        }
    }
    
    public ReviewResult reviewCode(CodeChange change) {
        log.debug("Reviewing file with Gemini: {}", change.getFileName());
        
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-api-key-here")) {
            return createMockReview(change);
        }
        
        try {
            String prompt = buildReviewPrompt(change);
            String review = callGeminiAPI(prompt);
            return parseReviewResult(review, change);
        } catch (Exception e) {
            log.error("Error during Gemini AI review for file {}: {}", change.getFileName(), e.getMessage());
            return createErrorReview(change, e.getMessage());
        }
    }
    
    private String callGeminiAPI(String prompt) throws Exception {
        String url = GEMINI_API_URL.replace("{model}", modelName).replace("{apiKey}", apiKey);
        
        // Build request body
        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode contents = requestBody.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", prompt);
        
        // Add generation config
        ObjectNode generationConfig = requestBody.putObject("generationConfig");
        generationConfig.put("temperature", temperature);
        generationConfig.put("maxOutputTokens", maxTokens);
        generationConfig.put("topP", 0.95);
        generationConfig.put("topK", 40);
        
        // Add safety settings (optional - adjust as needed)
        ArrayNode safetySettings = requestBody.putArray("safetySettings");
        String[] categories = {
            "HARM_CATEGORY_HARASSMENT",
            "HARM_CATEGORY_HATE_SPEECH",
            "HARM_CATEGORY_SEXUALLY_EXPLICIT",
            "HARM_CATEGORY_DANGEROUS_CONTENT"
        };
        for (String category : categories) {
            ObjectNode setting = safetySettings.addObject();
            setting.put("category", category);
            setting.put("threshold", "BLOCK_NONE");
        }
        
        // Make API call
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            url, 
            HttpMethod.POST, 
            request, 
            String.class
        );
        
        // Parse response
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        JsonNode candidates = responseJson.path("candidates");
        if (candidates.isArray() && candidates.size() > 0) {
            JsonNode firstCandidate = candidates.get(0);
            JsonNode content1 = firstCandidate.path("content");
            JsonNode parts1 = content1.path("parts");
            if (parts1.isArray() && parts1.size() > 0) {
                return parts1.get(0).path("text").asText();
            }
        }
        
        throw new RuntimeException("Invalid response from Gemini API");
    }
    
    private String buildReviewPrompt(CodeChange change) {
        return String.format("""
            You are an expert code reviewer with 20+ years of experience.
            Review the following code changes and provide constructive feedback.
            
            File: %s
            Status: %s
            Lines added: %d, Lines deleted: %d
            
            Code changes:
            ```diff
            %s
            ```
            
            Please analyze this code and provide a JSON response with the following structure:
            {
              "summary": "Brief summary of the changes",
              "severity": "info|warning|error",
              "issues": [
                {
                  "line": line_number_or_null,
                  "type": "bug|performance|security|style|best-practice",
                  "severity": "info|warning|error",
                  "message": "Description of the issue",
                  "suggestion": "How to fix it"
                }
              ],
              "positives": ["List of good practices found in the code"]
            }
            
            Focus on:
            1. Potential bugs and logic errors
            2. Security vulnerabilities
            3. Performance issues
            4. Code quality and maintainability
            5. Best practices for the programming language
            
            Return ONLY the JSON response, no additional text or markdown formatting.
            """, 
            change.getFileName(),
            change.getStatus(),
            change.getAdditions(),
            change.getDeletions(),
            change.getPatch() != null ? change.getPatch() : "No patch available"
        );
    }
    
    private ReviewResult parseReviewResult(String review, CodeChange change) {
        try {
            // Clean the response
            review = review.trim();
            
            // Remove markdown code blocks if present
            if (review.contains("```json")) {
                int start = review.indexOf("```json") + 7;
                int end = review.lastIndexOf("```");
                if (end > start) {
                    review = review.substring(start, end).trim();
                }
            } else if (review.startsWith("```")) {
                review = review.replaceAll("```", "").trim();
            }
            
            JsonNode json = objectMapper.readTree(review);
            
            ReviewResult result = new ReviewResult();
            result.setFileName(change.getFileName());
            result.setSummary(json.path("summary").asText("Code review completed"));
            result.setSeverity(json.path("severity").asText("info"));
            
            List<ReviewIssue> issues = new ArrayList<>();
            JsonNode issuesNode = json.path("issues");
            if (issuesNode.isArray()) {
                for (JsonNode issueNode : issuesNode) {
                    ReviewIssue issue = new ReviewIssue();
                    issue.setLine(issueNode.path("line").isNull() ? null : issueNode.path("line").asInt());
                    issue.setType(issueNode.path("type").asText("general"));
                    issue.setSeverity(issueNode.path("severity").asText("info"));
                    issue.setMessage(issueNode.path("message").asText());
                    issue.setSuggestion(issueNode.path("suggestion").asText());
                    issues.add(issue);
                }
            }
            result.setIssues(issues);
            
            List<String> positives = new ArrayList<>();
            JsonNode positivesNode = json.path("positives");
            if (positivesNode.isArray()) {
                for (JsonNode positive : positivesNode) {
                    positives.add(positive.asText());
                }
            }
            result.setPositives(positives);
            
            return result;
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
            log.debug("Raw response: {}", review);
            return createErrorReview(change, "Failed to parse AI response");
        }
    }
    
    private ReviewResult createMockReview(CodeChange change) {
        ReviewResult result = new ReviewResult();
        result.setFileName(change.getFileName());
        result.setSummary("Mock review - Gemini API key not configured");
        result.setSeverity("info");
        
        List<ReviewIssue> issues = new ArrayList<>();
        
        if (change.getFileName().endsWith(".java")) {
            ReviewIssue issue = new ReviewIssue();
            issue.setType("info");
            issue.setSeverity("info");
            issue.setMessage("This is a mock review. Configure GEMINI_API_KEY to enable real AI reviews with Google Gemini.");
            issue.setSuggestion("Get your API key from https://makersuite.google.com/app/apikey");
            issues.add(issue);
        }
        
        result.setIssues(issues);
        result.setPositives(Arrays.asList("Code structure looks good", "Proper naming conventions"));
        
        return result;
    }
    
    private ReviewResult createErrorReview(CodeChange change, String error) {
        ReviewResult result = new ReviewResult();
        result.setFileName(change.getFileName());
        result.setSummary("Review failed: " + error);
        result.setSeverity("warning");
        result.setIssues(new ArrayList<>());
        result.setPositives(new ArrayList<>());
        return result;
    }
}