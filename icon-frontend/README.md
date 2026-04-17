# ICON Frontend

React 기반 내부통제 시스템 프론트엔드

## 🚀 시작하기

```bash
npm install
npm start
```

## 📚 프로젝트 문서

### 핵심 설계 문서
- [프로젝트 개요](../business/core/프로젝트-개요.md)
- [룰 시스템 상세 설계](../business/core/룰-시스템-상세-설계.md)
- [에이전트 기반 데이터 처리 아키텍처](../business/core/에이전트-기반-데이터-처리-아키텍처.md)

### 운영 문서
- [룰 운영 전략](../business/operations/룰-운영-전략.md)
- [룰 관리 체계 개념](../business/operations/룰-관리-체계-개념.md)

### 주요 결정사항
- [SystemType 제거 결정](../business/decisions/SystemType-제거-결정.md)

### 개발 가이드
- [개발 가이드라인](../CONTRIBUTING.md)
- [전체 비즈니스 문서 구조](../business/README.md)

## 🏗️ 기술 스택

- React 18
- TypeScript
- Material-UI / Ant Design (TBD)
- Redux Toolkit (상태 관리)

## 📁 프로젝트 구조

```
frontend/
├── src/
│   ├── components/    # 공통 컴포넌트
│   ├── pages/        # 페이지 컴포넌트
│   ├── features/     # 기능별 모듈
│   │   ├── rule/     # 룰 관리
│   │   └── company/  # 회사 관리
│   ├── services/     # API 통신
│   └── utils/        # 유틸리티
├── public/
└── package.json
```

## 🎨 주요 화면

- **룰 관리**: Rule 및 Scenario 생성/편집
- **룰 모니터링**: 실시간 룰 실행 현황
- **회사 관리**: 회사 정보 및 설정
- **대시보드**: 전체 현황 및 통계

## 🔗 API 연동

백엔드 API 기본 주소: `http://localhost:8080/api/v1`

환경 변수로 설정:
```
REACT_APP_API_BASE_URL=http://localhost:8080/api/v1
```
