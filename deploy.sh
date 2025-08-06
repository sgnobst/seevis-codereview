#!/bin/bash

echo "🚀 GitHub Actions 배포 스크립트"
echo "================================"

# GitHub 사용자명 입력
read -p "GitHub 사용자명을 입력하세요: " GITHUB_USERNAME
read -p "저장소 이름을 입력하세요 (기본: seevis-codereview): " REPO_NAME
REPO_NAME=${REPO_NAME:-seevis-codereview}

echo ""
echo "📦 Git 설정 중..."

# Git 사용자 설정
git config user.name "$GITHUB_USERNAME"
git config user.email "$GITHUB_USERNAME@users.noreply.github.com"

# 모든 파일 추가
echo "📝 파일 추가 중..."
git add .

# 커밋
echo "💾 커밋 중..."
git commit -m "Initial commit: GitHub Code Reviewer with Gemini AI

- Spring Boot 3.2.0 기반 코드 리뷰 시스템
- Google Gemini AI 통합
- GitHub Actions 자동 실행
- Swagger UI 포함"

# Remote 설정
echo "🔗 GitHub 저장소 연결 중..."
git remote add origin "https://github.com/$GITHUB_USERNAME/$REPO_NAME.git" 2>/dev/null || \
git remote set-url origin "https://github.com/$GITHUB_USERNAME/$REPO_NAME.git"

# 메인 브랜치 설정
git branch -M main

# 푸시
echo "⬆️ GitHub에 푸시 중..."
git push -u origin main

echo ""
echo "✅ 푸시 완료!"
echo ""
echo "📋 다음 단계:"
echo "1. GitHub에서 저장소 확인: https://github.com/$GITHUB_USERNAME/$REPO_NAME"
echo "2. Settings → Secrets and variables → Actions"
echo "3. 'New repository secret' 클릭"
echo "4. Name: GEMINI_API_KEY"
echo "5. Value: AIzaSyD1Yyt-xKtNXm6tPPiqX3-3gMnSdfNClk4"
echo "6. 'Add secret' 클릭"
echo ""
echo "🎉 완료되면 PR을 생성하면 자동으로 코드 리뷰가 실행됩니다!"