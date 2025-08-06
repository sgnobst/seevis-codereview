# 🤖 다른 프로젝트에 AI 코드 리뷰 봇 설치하기

## 📋 설치 방법

### 1️⃣ **대상 프로젝트에 워크플로우 파일 추가**

대상 프로젝트의 `.github/workflows/` 폴더에 아래 파일을 생성하세요:

**.github/workflows/ai-code-review.yml**
```yaml
name: AI Code Review

on:
  pull_request:
    types: [opened, synchronize, reopened]

jobs:
  code-review:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      pull-requests: write
      issues: write
    
    steps:
    - name: Checkout PR
      uses: actions/checkout@v4
      with:
        fetch-depth: 0
        
    - name: Setup JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Clone Code Review Tool
      run: |
        git clone https://github.com/sgnobst/seevis-codereview.git /tmp/reviewer
        cd /tmp/reviewer
        
    - name: Setup Gradle
      uses: gradle/gradle-build-action@v2
      with:
        gradle-version: '8.5'
        build-root-directory: /tmp/reviewer
        
    - name: Build Review Tool
      run: |
        cd /tmp/reviewer
        chmod +x gradlew
        ./gradlew build -x test --no-daemon
        
    - name: Run Code Review
      run: |
        cd /tmp/reviewer
        ./gradlew runReview --no-daemon
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        GEMINI_API_KEY: ${{ secrets.GEMINI_API_KEY }}
        PR_NUMBER: ${{ github.event.pull_request.number }}
        GITHUB_REPOSITORY: ${{ github.repository }}
        GITHUB_BASE_REF: ${{ github.base_ref }}
        GITHUB_HEAD_REF: ${{ github.head_ref }}
```

### 2️⃣ **GitHub Secrets 설정**

대상 프로젝트의 Settings에서:

1. **Settings** → **Secrets and variables** → **Actions**
2. **New repository secret** 클릭
3. 추가:
   ```
   Name: GEMINI_API_KEY
   Value: AIzaSyD1Yyt-xKtNXm6tPPiqX3-3gMnSdfNClk4
   ```

### 3️⃣ **권한 설정**

대상 프로젝트의 Settings에서:

1. **Settings** → **Actions** → **General**
2. **Workflow permissions**
3. **Read and write permissions** 선택
4. **Save**

## 🎉 완료!

이제 해당 프로젝트에서 PR이 생성되면:
1. 자동으로 코드 리뷰 도구가 다운로드됨
2. PR의 변경사항을 분석
3. Gemini AI가 코드 리뷰 수행
4. PR에 코멘트 자동 작성

## 📝 예시

누군가 당신의 프로젝트에 PR을 보내면:
```
User → Creates PR → GitHub Actions 자동 실행 → AI 코드 리뷰 → PR에 코멘트
```

## ⚙️ 커스터마이징

### 특정 파일만 리뷰하려면:
워크플로우에 조건 추가:
```yaml
on:
  pull_request:
    types: [opened, synchronize, reopened]
    paths:
      - '**.java'
      - '**.js'
      - '**.py'
```

### 특정 브랜치만 리뷰하려면:
```yaml
on:
  pull_request:
    types: [opened, synchronize, reopened]
    branches:
      - main
      - develop
```

## 🔧 문제 해결

### "GEMINI_API_KEY not found" 에러
→ Secrets 설정 확인

### "Permission denied" 에러
→ Workflow permissions를 "Read and write"로 변경

### API 할당량 초과
→ 새 Gemini API 키 발급 또는 대기

## 💡 팁

- Private 저장소도 동일하게 작동
- 여러 프로젝트에 동일한 GEMINI_API_KEY 사용 가능
- PR당 자동으로 2-3개의 주요 이슈를 코멘트로 남김