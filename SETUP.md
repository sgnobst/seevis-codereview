# 🚀 GitHub Code Reviewer 설정 가이드

## 1. API 키 설정

### Gemini API 키 (제공됨)
```
GEMINI_API_KEY=AIzaSyD1Yyt-xKtNXm6tPPiqX3-3gMnSdfNClk4
```

### GitHub Token 발급
1. GitHub → Settings → Developer settings → Personal access tokens
2. "Generate new token (classic)" 클릭
3. 필요 권한 선택:
   - `repo` (전체 리포지토리 액세스)
   - `write:discussion` (PR 코멘트 작성)
4. 토큰 생성 및 복사

## 2. GitHub Actions 설정

### Repository Secrets 추가
1. Repository → Settings → Secrets and variables → Actions
2. 다음 시크릿 추가:
   - `GEMINI_API_KEY`: AIzaSyD1Yyt-xKtNXm6tPPiqX3-3gMnSdfNClk4
   - `GITHUB_TOKEN`: (자동으로 제공되는 토큰 사용)

### Workflow 파일 복사
`.github/workflows/code-review.yml` 파일을 대상 repository에 복사

## 3. 로컬 테스트

### Swagger UI로 테스트
```bash
# 환경 변수 설정
export GEMINI_API_KEY="AIzaSyD1Yyt-xKtNXm6tPPiqX3-3gMnSdfNClk4"
export GITHUB_TOKEN="your-github-token"
export GITHUB_REPOSITORY="owner/repo"

# Swagger UI 실행
./gradlew runWeb

# 브라우저에서 접속
# http://localhost:8080/swagger-ui.html
```

### CLI로 테스트
```bash
# PR 리뷰 실행
export PR_NUMBER=1
./gradlew runReview
```

### API 직접 호출
```bash
# 헬스체크
curl http://localhost:8080/api/v1/review/health

# PR 리뷰 (Mock 모드)
curl -X POST http://localhost:8080/api/v1/review/pr/1
```

## 4. 실제 사용

### GitHub Actions 자동 실행
PR이 생성되거나 업데이트되면 자동으로 코드 리뷰가 실행됩니다.

### 수동 실행
```bash
# GitHub Actions 수동 트리거
gh workflow run code-review.yml
```

## 5. 모니터링

### 로그 확인
- GitHub Actions: Actions 탭에서 실행 로그 확인
- 로컬: `logs/code-review.log` 파일 확인

### Gemini API 사용량
- [Google Cloud Console](https://console.cloud.google.com/)에서 API 사용량 모니터링

## 6. 문제 해결

### API 키 에러
- Gemini API 키가 올바른지 확인
- API 할당량 초과 여부 확인

### GitHub 권한 에러
- Personal Access Token 권한 확인
- Repository 접근 권한 확인

### 빌드 에러
```bash
# 클린 빌드
./gradlew clean build

# Gradle 래퍼 재생성
gradle wrapper --gradle-version=8.5
```

## 7. 설정 커스터마이징

### application.yml
```yaml
# AI 모델 설정
gemini:
  model: gemini-1.5-flash  # 또는 gemini-1.5-pro
  temperature: 0.3         # 0.0-1.0 (창의성 레벨)
  max-tokens: 2048         # 응답 최대 토큰

# 코드 리뷰 설정
code-review:
  max-files: 50            # 리뷰할 최대 파일 수
  max-file-size-kb: 500    # 최대 파일 크기
  max-comments-per-review: 20  # PR당 최대 코멘트
```

## 8. 보안 주의사항

⚠️ **중요**: 
- API 키를 코드에 직접 포함하지 마세요
- `.gitignore`에 환경 설정 파일 추가
- GitHub Secrets 사용 권장
- 로컬 테스트 시 환경 변수 사용

## 9. 추가 리소스

- [Google Gemini API 문서](https://ai.google.dev/docs)
- [GitHub Actions 문서](https://docs.github.com/en/actions)
- [Spring Boot 문서](https://spring.io/projects/spring-boot)