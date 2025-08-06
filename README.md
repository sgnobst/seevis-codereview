# GitHub Code Reviewer 🤖

AI 기반 자동 GitHub Pull Request 코드 리뷰 시스템

## 📋 개요

이 프로젝트는 GitHub Actions와 Google Gemini AI를 활용하여 Pull Request에 대한 자동 코드 리뷰를 수행하는 Spring Boot 애플리케이션입니다.

## ✨ 주요 기능

- 🔍 **자동 코드 리뷰**: PR이 생성되거나 업데이트될 때 자동으로 코드 분석
- 🤖 **AI 기반 분석**: Google Gemini를 활용한 지능형 코드 리뷰
- 💬 **GitHub 통합**: 리뷰 결과를 PR 코멘트로 직접 작성
- 🏷️ **자동 라벨링**: 리뷰 상태에 따른 자동 라벨 관리
- ⚙️ **커스터마이징 가능**: 파일 타입, 크기, 리뷰 범위 설정 가능

## 🚀 빠른 시작

### 사전 요구사항

- Java 17 이상
- Gradle 8.5 이상
- GitHub Repository
- Google Gemini API Key

### API 키 발급 방법

#### 1. Google Gemini API Key
1. [Google AI Studio](https://makersuite.google.com/app/apikey) 접속
2. "Get API key" 클릭
3. 프로젝트 선택 또는 새 프로젝트 생성
4. API 키 생성 및 복사

#### 2. GitHub Token
1. GitHub > Settings > Developer settings > Personal access tokens
2. "Generate new token (classic)" 클릭
3. 필요 권한 선택:
   - `repo` (전체 리포지토리 액세스)
   - `write:discussion` (PR 코멘트 작성)
4. 토큰 생성 및 복사

### 설치 방법

1. **Repository Fork 또는 Clone**
```bash
git clone https://github.com/your-org/github-code-reviewer.git
cd github-code-reviewer
```

2. **GitHub Secrets 설정**

Repository Settings > Secrets and variables > Actions에서 다음 시크릿 추가:
- `GEMINI_API_KEY`: Google Gemini API 키 ([여기서 발급](https://makersuite.google.com/app/apikey))

3. **GitHub Actions Workflow 추가**

`.github/workflows/code-review.yml` 파일을 대상 repository에 복사

4. **로컬 테스트**
```bash
# 환경 변수 설정
export GITHUB_TOKEN=your-github-token
export GEMINI_API_KEY=your-gemini-key
export GITHUB_REPOSITORY=owner/repo
export PR_NUMBER=1

# 빌드 및 실행
./gradlew build
./gradlew runReview
```

## 🖥️ Swagger UI로 로컬 테스트

### Swagger UI 실행
```bash
# Swagger UI와 함께 웹 서버 실행
./gradlew runWeb

# 또는 환경 변수와 함께 실행
export GITHUB_TOKEN=your-github-token
export GEMINI_API_KEY=your-gemini-key
export GITHUB_REPOSITORY=owner/repo
./gradlew runWeb
```

### Swagger UI 접속
브라우저에서 http://localhost:8080/swagger-ui.html 접속

### API 엔드포인트
- `POST /api/review/pr/{prNumber}` - 특정 PR 리뷰
- `POST /api/review/manual` - 수동 코드 리뷰
- `GET /api/review/health` - 서비스 상태 확인

## 📁 프로젝트 구조

```
github-code-reviewer/
├── src/main/java/com/seevis/codereview/
│   ├── GitHubCodeReviewerApplication.java    # 메인 애플리케이션
│   ├── service/
│   │   ├── GitHubService.java               # GitHub API 연동
│   │   ├── AIReviewService.java             # AI 리뷰 서비스
│   │   └── CodeReviewOrchestrator.java      # 리뷰 오케스트레이터
│   └── model/
│       ├── PullRequest.java                 # PR 모델
│       ├── CodeChange.java                  # 코드 변경 모델
│       ├── ReviewComment.java               # 리뷰 코멘트 모델
│       ├── ReviewResult.java                # 리뷰 결과 모델
│       └── ReviewIssue.java                 # 리뷰 이슈 모델
├── .github/workflows/
│   └── code-review.yml                      # GitHub Actions workflow
├── build.gradle                             # Gradle 빌드 설정
└── src/main/resources/
    └── application.yml                       # 애플리케이션 설정
```

## ⚙️ 설정

### application.yml 주요 설정

```yaml
# AI 모델 설정 (Gemini)
gemini:
  model: gemini-1.5-flash   # 사용할 AI 모델 (gemini-1.5-flash, gemini-1.5-pro)
  temperature: 0.3          # 창의성 레벨 (0.0-1.0)
  max-tokens: 2048         # 최대 토큰 수

# 코드 리뷰 설정
code-review:
  max-files: 50            # 리뷰할 최대 파일 수
  max-file-size-kb: 500    # 최대 파일 크기 (KB)
  max-comments-per-review: 20  # PR당 최대 코멘트 수
```

### 지원하는 파일 형식

- **프로그래밍 언어**: Java, Kotlin, JavaScript, TypeScript, Python, Go, Rust, C/C++, C#, Ruby, PHP, Swift, Scala
- **설정 파일**: XML, YAML, JSON, Properties, Gradle
- **기타**: SQL, Shell Scripts, Dockerfile

## 🔍 리뷰 항목

AI는 다음 항목들을 중점적으로 검토합니다:

1. 🐛 **버그 가능성**: 잠재적 버그 및 로직 오류
2. 🔒 **보안 취약점**: SQL Injection, XSS 등 보안 이슈
3. ⚡ **성능 문제**: 비효율적인 알고리즘, 메모리 누수
4. 🎨 **코드 품질**: 명명 규칙, 가독성, 중복 코드
5. ✨ **베스트 프랙티스**: 언어별 관례 및 모범 사례

## 📊 리뷰 결과 예시

```markdown
🤖 **AI Code Review Complete**

Found 3 issues in UserService.java:

🟡 **WARNING** 🔒
**Issue:** Potential SQL injection vulnerability
**Line:** 45
**Suggestion:** Use parameterized queries instead of string concatenation

🔵 **INFO** ✨
**Issue:** Method could be simplified using Stream API
**Line:** 78
**Suggestion:** Consider using stream().filter().collect() pattern
```

## 🛠️ 고급 설정

### GitHub App 사용 (기업용)

더 안전하고 확장 가능한 인증을 위해 GitHub App을 사용할 수 있습니다:

1. GitHub App 생성 (Settings > Developer settings > GitHub Apps)
2. 필요 권한 설정:
   - Pull requests: Read & Write
   - Contents: Read
   - Issues: Write
3. App ID와 Private Key 설정

### 커스텀 AI 모델 사용

다른 AI 서비스(OpenAI, Claude 등)를 사용하려면 `AIReviewService.java`를 수정하세요.

## 🤝 기여하기

프로젝트 개선을 위한 기여를 환영합니다!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다.

## 🙋‍♂️ 지원

문제가 발생하거나 질문이 있으시면 [Issues](https://github.com/your-org/github-code-reviewer/issues)를 통해 문의해주세요.

## 🏆 Credits

- Built with Spring Boot & Gradle
- Powered by Google Gemini AI
- GitHub API integration using kohsuke/github-api

---

Made with ❤️ by Seevis Team