package com.seevis.codereview.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;
import java.util.List;

@Configuration
@Profile("!cli") // CLI 모드에서는 비활성화
public class SwaggerConfig {
    
    @Value("${spring.application.name:GitHub Code Reviewer}")
    private String applicationName;
    
    @Bean
    public OpenAPI customOpenAPI() {
        // Security Scheme 정의
        SecurityScheme githubTokenScheme = new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.HEADER)
            .name("Authorization")
            .description("GitHub Personal Access Token (Bearer token)");
        
        SecurityScheme apiKeyScheme = new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.HEADER)
            .name("X-API-Key")
            .description("API Key for authentication");
        
        // Security Requirement
        SecurityRequirement securityRequirement = new SecurityRequirement()
            .addList("GitHub Token")
            .addList("API Key");
        
        // Servers
        Server localServer = new Server()
            .url("http://localhost:8080")
            .description("Local Development Server");
        
        Server prodServer = new Server()
            .url("https://api.codereview.seevis.com")
            .description("Production Server");
        
        return new OpenAPI()
            .info(new Info()
                .title("GitHub Code Reviewer API")
                .version("1.0.0")
                .description("""
                    ## 🤖 AI 기반 GitHub Pull Request 코드 리뷰 시스템
                    
                    이 API를 통해 GitHub Pull Request에 대한 자동 코드 리뷰를 실행할 수 있습니다.
                    
                    ### 주요 기능
                    - **자동 코드 리뷰**: PR 번호만으로 즉시 코드 리뷰 실행
                    - **AI 분석**: GPT-4를 활용한 지능형 코드 분석
                    - **GitHub 통합**: 리뷰 결과를 PR 코멘트로 자동 작성
                    - **Webhook 지원**: GitHub 이벤트 자동 처리
                    
                    ### 사용 방법
                    1. GitHub Personal Access Token 또는 API Key를 준비합니다
                    2. Authorization 헤더에 토큰을 설정합니다
                    3. `/api/v1/review/pr/{prNumber}` 엔드포인트를 호출합니다
                    
                    ### 인증
                    - **GitHub Token**: `Authorization: Bearer {your-github-token}`
                    - **API Key**: `X-API-Key: {your-api-key}`
                    """)
                .contact(new Contact()
                    .name("Seevis Team")
                    .email("dev@seevis.com")
                    .url("https://github.com/seevis/github-code-reviewer"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(Arrays.asList(localServer, prodServer))
            .components(new Components()
                .addSecuritySchemes("GitHub Token", githubTokenScheme)
                .addSecuritySchemes("API Key", apiKeyScheme))
            .security(List.of(securityRequirement))
            .externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation()
                .description("GitHub Repository")
                .url("https://github.com/seevis/github-code-reviewer"));
    }
}