# 🚀 GitHub Actions 배포 가이드

## 1️⃣ GitHub 저장소 생성

### GitHub.com에서:
1. 우측 상단 **+** 버튼 → **New repository**
2. Repository name: `seevis-codereview` (또는 원하는 이름)
3. Private/Public 선택
4. **Create repository** 클릭

## 2️⃣ 로컬 코드를 GitHub에 푸시

```bash
# 1. Git 초기화 (이미 완료됨)
git init

# 2. 모든 파일 추가
git add .

# 3. 첫 커밋
git commit -m "Initial commit: GitHub Code Reviewer with Gemini AI"

# 4. GitHub 저장소 연결 (your-username을 실제 GitHub 사용자명으로 변경)
git remote add origin https://github.com/your-username/seevis-codereview.git

# 5. 메인 브랜치로 설정
git branch -M main

# 6. 푸시
git push -u origin main
```

## 3️⃣ GitHub Secrets 설정

### GitHub 저장소 페이지에서:
1. **Settings** 탭 클릭
2. 왼쪽 메뉴에서 **Secrets and variables** → **Actions**
3. **New repository secret** 클릭
4. 다음 시크릿 추가:

#### Secret 1: GEMINI_API_KEY
- Name: `GEMINI_API_KEY`
- Value: `AIzaSyD1Yyt-xKtNXm6tPPiqX3-3gMnSdfNClk4`
- **Add secret** 클릭

#### Secret 2: GITHUB_TOKEN (선택사항)
- 기본적으로 GitHub Actions가 자동 제공
- 더 많은 권한이 필요한 경우만 Personal Access Token 추가

## 4️⃣ GitHub Actions 활성화 확인

1. 저장소 **Actions** 탭 클릭
2. "Get started with GitHub Actions" 메시지가 보이면 활성화됨
3. Workflow가 `.github/workflows/code-review.yml`에 이미 있으므로 자동 인식됨

## 5️⃣ 테스트 PR 생성

### 테스트 브랜치 생성:
```bash
# 1. 새 브랜치 생성
git checkout -b test-code-review

# 2. 테스트 파일 생성
echo "public class Test {
    public static void main(String[] args) {
        String password = \"123456\"; // 보안 이슈!
        System.out.println(password);
    }
}" > Test.java

# 3. 커밋
git add Test.java
git commit -m "Add test file with security issue"

# 4. 푸시
git push origin test-code-review
```

### GitHub에서 PR 생성:
1. GitHub 저장소 페이지로 이동
2. **Pull requests** 탭 클릭
3. **New pull request** 클릭
4. base: `main` ← compare: `test-code-review`
5. **Create pull request** 클릭
6. 제목과 설명 입력 후 **Create pull request** 클릭

## 6️⃣ 자동 코드 리뷰 확인

PR을 생성하면:
1. **Actions** 탭에서 실행 상태 확인
2. 약 1-2분 후 PR 페이지에 AI 코드 리뷰 코멘트 표시
3. 라벨 확인:
   - `ai-reviewing`: 리뷰 진행 중
   - `ai-reviewed`: 리뷰 완료

## 7️⃣ 실행 로그 확인

### Actions 탭에서:
1. 실행된 워크플로우 클릭
2. `code-review` job 클릭
3. 각 단계별 로그 확인 가능

### 주요 확인 사항:
- ✅ Checkout PR
- ✅ Setup JDK 17
- ✅ Build with Gradle
- ✅ Run Code Review
- ✅ Gemini API 호출
- ✅ PR 코멘트 작성

## 8️⃣ 문제 해결

### "GEMINI_API_KEY is not set" 에러:
- Settings → Secrets → GEMINI_API_KEY 확인
- 시크릿 이름이 정확한지 확인

### "Permission denied" 에러:
- Settings → Actions → General
- Workflow permissions → "Read and write permissions" 선택

### Build 실패:
- Java 17이 설정되었는지 확인
- Gradle wrapper 파일이 푸시되었는지 확인

### API 호출 실패:
- Gemini API 키가 유효한지 확인
- API 할당량 초과 여부 확인

## 9️⃣ 고급 설정

### 특정 브랜치만 리뷰:
`.github/workflows/code-review.yml` 수정:
```yaml
on:
  pull_request:
    types: [opened, synchronize, reopened]
    branches:
      - main
      - develop
```

### 특정 파일 제외:
`application.yml` 수정:
```yaml
code-review:
  exclude-patterns:
    - "**/test/**"
    - "**/*.md"
    - "**/docs/**"
```

### 리뷰 언어 변경:
`AIReviewService.java`의 프롬프트 수정

## 🎉 완료!

이제 PR을 생성할 때마다 자동으로 Gemini AI가 코드 리뷰를 수행합니다!

### 추가 명령어:
```bash
# 수동으로 워크플로우 실행
gh workflow run code-review.yml

# 워크플로우 상태 확인
gh run list --workflow=code-review.yml

# 최근 실행 로그 보기
gh run view --log
```