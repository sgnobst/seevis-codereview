package com.seevis.codereview;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ComponentScan(excludeFilters = @ComponentScan.Filter(
    type = FilterType.ASSIGNABLE_TYPE,
    classes = GitHubCodeReviewerApplication.class))
@Slf4j
public class GitHubCodeReviewerWebApplication {
    
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(GitHubCodeReviewerWebApplication.class);
        app.setWebApplicationType(WebApplicationType.SERVLET); // 웹 서버 활성화
        app.run(args);
    }
    
    @PostConstruct
    public void init() {
        log.info("=========================================================");
        log.info("  GitHub Code Reviewer - Web Mode");
        log.info("=========================================================");
        log.info("  Swagger UI: http://localhost:8080/swagger-ui.html");
        log.info("  API Docs: http://localhost:8080/v3/api-docs");
        log.info("=========================================================");
    }
}