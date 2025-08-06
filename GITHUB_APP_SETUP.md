# 🤖 GitHub App 설정 가이드

## 📌 작동 방식
1. 누군가 PR을 생성/업데이트
2. GitHub가 웹훅을 당신의 서버로 전송
3. 서버가 Gemini AI로 코드 분석
4. 분석 결과를 PR에 코멘트로 작성

## 1️⃣ GitHub App 생성

### GitHub.com에서:
1. https://github.com/settings/apps 접속
2. **New GitHub App** 클릭
3. 다음 정보 입력:

### 📝 Basic Information
```
GitHub App name: Seevis AI Code Reviewer
Homepage URL: https://github.com/sgnobst/seevis-codereview
```

### 🔗 Webhook
```
Webhook URL: https://your-server.com/webhook/github
(또는 ngrok 사용 시: https://xxxxx.ngrok.io/webhook/github)

Webhook secret: (랜덤 문자열 생성 - 예: abc123xyz789)
```

### 🔐 Permissions (권한)

**Repository permissions:**
- `Contents`: Read
- `Issues`: Write  
- `Metadata`: Read
- `Pull requests`: Write
- `Commit statuses`: Write (선택사항)

**Organization permissions:**
- 필요 없음

### 📬 Subscribe to events (이벤트 구독)
다음 항목 체크:
- ✅ Pull request
- ✅ Pull request review (선택사항)
- ✅ Pull request review comment (선택사항)

### 🌍 Where can this GitHub App be installed?
- 🔘 **Any account** (누구나 설치 가능) 또는
- 🔘 **Only on this account** (본인만 사용)

4. **Create GitHub App** 클릭

## 2️⃣ Private Key 생성

1. 생성된 App 페이지에서 스크롤 다운
2. **Private keys** 섹션 찾기
3. **Generate a private key** 클릭
4. `.pem` 파일 다운로드 (⚠️ 안전하게 보관!)

## 3️⃣ App ID 확인

App 페이지 상단에서:
- **App ID**: 123456 (숫자)
- 이 값을 기록해두세요

## 4️⃣ 서버 설정

### 로컬 테스트 (ngrok 사용)

#### ngrok 설치 및 실행:
```bash
# ngrok 설치 (Mac)
brew install ngrok

# 또는 직접 다운로드
# https://ngrok.com/download

# ngrok 실행 (8080 포트)
ngrok http 8080
```

ngrok 실행 후 나오는 URL을 복사 (예: https://abc123.ngrok.io)

#### 애플리케이션 실행:
```bash
# 환경 변수 설정
export GEMINI_API_KEY="AIzaSyD1Yyt-xKtNXm6tPPiqX3-3gMnSdfNClk4"
export GITHUB_APP_ID="123456"  # 위에서 확인한 App ID
export GITHUB_APP_PRIVATE_KEY_PATH="/path/to/your-app.private-key.pem"
export GITHUB_WEBHOOK_SECRET="abc123xyz789"  # 위에서 설정한 secret

# 서버 실행
./gradlew runWeb
```

### 프로덕션 배포 (Heroku 예시)

```bash
# Heroku 앱 생성
heroku create seevis-code-reviewer

# 환경 변수 설정
heroku config:set GEMINI_API_KEY="AIzaSyD1Yyt-xKtNXm6tPPiqX3-3gMnSdfNClk4"
heroku config:set GITHUB_APP_ID="123456"
heroku config:set GITHUB_WEBHOOK_SECRET="abc123xyz789"

# Private key는 base64로 인코딩해서 저장
cat your-app.private-key.pem | base64 | heroku config:set GITHUB_APP_PRIVATE_KEY_BASE64

# 배포
git push heroku main
```

### Docker 배포

```dockerfile
FROM openjdk:17-jdk-slim
COPY build/libs/github-code-reviewer-1.0.0.jar app.jar
COPY your-app.private-key.pem /app/private-key.pem

ENV GITHUB_APP_PRIVATE_KEY_PATH=/app/private-key.pem
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 5️⃣ GitHub App 설치

### App 설치하기:
1. https://github.com/settings/apps 에서 생성한 App 클릭
2. **Install App** 클릭
3. 설치할 계정 선택
4. 저장소 선택:
   - **All repositories** (모든 저장소) 또는
   - **Selected repositories** (특정 저장소 선택)
5. **Install** 클릭

## 6️⃣ 웹훅 URL 업데이트

### ngrok 사용 시:
1. GitHub App 설정 페이지로 이동
2. **Webhook URL** 수정:
   ```
   https://your-ngrok-url.ngrok.io/webhook/github
   ```
3. **Save changes**

## 7️⃣ 테스트

### 테스트 PR 생성:
```bash
# 테스트 저장소에서
git checkout -b test-ai-review
echo "// Test code" > test.java
git add test.java
git commit -m "Test AI review"
git push origin test-ai-review
```

GitHub에서 PR 생성 → 1-2분 내 AI 리뷰 코멘트 확인

## 📊 모니터링

### 웹훅 전송 기록 확인:
1. GitHub App 설정 페이지
2. **Advanced** 탭
3. **Recent Deliveries** 섹션
4. 각 전송 기록 클릭하여 상세 내용 확인

### 서버 로그 확인:
```bash
# 로컬
tail -f logs/code-review.log

# Heroku
heroku logs --tail
```

## 🔧 문제 해결

### "Webhook delivery failed"
- 서버가 실행 중인지 확인
- ngrok URL이 올바른지 확인
- 방화벽/보안 그룹 설정 확인

### "Bad credentials"
- App ID가 올바른지 확인
- Private key 파일 경로 확인
- Installation ID 확인

### "API rate limit exceeded"
- Gemini API 할당량 확인
- GitHub API 제한 확인 (App은 시간당 5,000 요청)

## 🚀 고급 설정

### 여러 조직에 설치:
- Public App으로 설정하면 누구나 설치 가능
- 각 설치마다 고유한 Installation ID 부여

### 커스텀 리뷰 규칙:
`WebhookService.java`에서 `shouldReviewFile()` 메서드 수정

### 리뷰 메시지 커스터마이징:
`WebhookService.java`에서 코멘트 템플릿 수정

## 📝 환경 변수 정리

```bash
# 필수
GEMINI_API_KEY=AIzaSyD1Yyt-xKtNXm6tPPiqX3-3gMnSdfNClk4
GITHUB_APP_ID=123456
GITHUB_APP_PRIVATE_KEY_PATH=/path/to/private-key.pem
GITHUB_WEBHOOK_SECRET=your-webhook-secret

# 선택사항 (PAT 사용 시)
GITHUB_TOKEN=ghp_xxxxxxxxxx
```

## ✅ 완료!

이제 GitHub App이 설치된 저장소에서 PR을 만들면 자동으로 AI 코드 리뷰가 실행됩니다!