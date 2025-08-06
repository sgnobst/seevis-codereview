# 🤖 GitHub App으로 코드 리뷰 봇 만들기

## 왜 GitHub App?
- 여러 저장소에 한 번에 설치 가능
- 중앙 집중식 관리
- 더 강력한 권한 관리

## 📋 GitHub App 생성 방법

### 1️⃣ GitHub App 생성
1. https://github.com/settings/apps 접속
2. **New GitHub App** 클릭
3. 다음 정보 입력:

#### 기본 정보
- **GitHub App name**: `Seevis Code Reviewer`
- **Homepage URL**: `https://github.com/sgnobst/seevis-codereview`
- **Webhook URL**: `https://your-server.com/webhook` (서버가 있을 경우)
- **Webhook secret**: 랜덤 문자열 생성

#### 권한 (Permissions)
**Repository permissions:**
- Contents: Read
- Issues: Write
- Metadata: Read
- Pull requests: Write
- Commit statuses: Write

#### 이벤트 구독 (Subscribe to events)
- ✅ Pull request
- ✅ Pull request review
- ✅ Pull request review comment

#### Where can this GitHub App be installed?
- 🔘 Any account (공개) 또는
- 🔘 Only on this account (개인용)

4. **Create GitHub App** 클릭

### 2️⃣ Private Key 생성
1. 생성된 App 페이지에서
2. **Private keys** 섹션
3. **Generate a private key** 클릭
4. `.pem` 파일 다운로드 (안전하게 보관!)

### 3️⃣ App 설치
1. App 페이지에서 **Install App** 클릭
2. 설치할 계정 선택
3. 저장소 선택:
   - All repositories (모든 저장소) 또는
   - Selected repositories (특정 저장소만)
4. **Install** 클릭

### 4️⃣ 서버 설정 (선택사항)

AWS, Heroku, 또는 자체 서버에 배포:

**application.yml 수정:**
```yaml
github:
  auth:
    type: APP
    app-id: YOUR_APP_ID  # App 페이지에서 확인
    private-key-path: /path/to/private-key.pem
```

**서버 실행:**
```bash
java -jar github-code-reviewer.jar \
  --spring.profiles.active=server \
  --server.port=8080
```

## 🎯 사용 방법

### App이 설치된 저장소에서:
1. 누군가 PR 생성
2. GitHub App이 자동으로 이벤트 수신
3. 코드 리뷰 실행
4. PR에 코멘트 작성

## 💡 장점
- 한 번 설치로 여러 저장소 관리
- 중앙에서 API 키 관리
- 더 세밀한 권한 설정
- 조직 전체에 적용 가능

## 🔧 무료 서버 옵션

### 1. GitHub Actions (무료)
매달 2,000분 무료 (Private은 3,000분)

### 2. Heroku (무료 제한적)
```bash
heroku create seevis-code-reviewer
heroku config:set GEMINI_API_KEY=AIzaSyD1Yyt-xKtNXm6tPPiqX3-3gMnSdfNClk4
git push heroku main
```

### 3. Vercel/Netlify Functions
Serverless 함수로 배포 가능

## 📊 관리 대시보드

App 설치 현황 확인:
https://github.com/settings/installations

각 설치별로:
- 저장소 권한 수정
- 일시 중지/재개
- 제거