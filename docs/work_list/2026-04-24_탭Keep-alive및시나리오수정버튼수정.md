# 2026-04-24 시나리오 수정 버튼 페이지 전환 수정 / 탭 Keep-alive 시도 및 롤백

## 작업 개요

1. 시나리오 수정 버튼 새 창(`target="_blank"`) → 현재 탭 페이지 전환(`router.push`)으로 변경
2. 탭 Keep-alive 구현 시도 → 동작 불가 확인 → 롤백

---

## 1. 시나리오 수정 버튼 페이지 전환 수정

### 원인

`scenarios/page.tsx` 카드 뷰 편집 버튼과 리스트 뷰 연필 아이콘이 `<a target="_blank">` 방식으로
새 창에서 열림. 이전 데이터소스 수정과 동일한 패턴.

### 수정 파일

- `icon-frontend/src/app/scenarios/page.tsx`
  - 카드 뷰 편집 버튼: `<a href target="_blank">` → `<button onClick={() => router.push(...)}>` 변경
  - 리스트 뷰 연필 아이콘 버튼: 동일 변경
  - title "새 창에서 수정" → "수정"으로 변경

---

## 2. 탭 Keep-alive 시도 및 롤백

### 시도 내용

Layout.tsx에서 `pageCacheRef(Map<string, ReactNode>)`로 방문 페이지를 캐시하고,
`display:none`으로 비활성 페이지를 숨겨 언마운트를 방지하는 방식을 구현.

### 실패 원인

Next.js App Router의 `children` prop은 단순 React 컴포넌트가 아닌,
전역 라우팅 Context를 구독하는 `LayoutRouter` 내부 컴포넌트로 래핑되어 있음.
라우트 변경 시 Context가 업데이트되면, `display:none` 처리된 캐시 내 `LayoutRouter`도
새 라우트 내용으로 재렌더링되어 컴포넌트 상태가 초기화됨.

### 롤백 내용

- `icon-frontend/src/components/Layout.tsx` — keep-alive 코드 전부 제거, 원래 `{children}` 방식으로 복원
- `icon-frontend/src/components/TabBar.tsx` — evictPage 호출 제거
- `icon-frontend/src/contexts/PageCacheContext.tsx` — 파일 삭제

### 추후 개선 방향

Jotai atom을 활용한 페이지별 필터 상태 지속 방식으로 재구현 예정 (TODO 등록 필요)

---

## CLAUDE.md 규칙 추가

- `docs/work_list/` 및 `docs/todo_list/TODO.md` 업데이트 규칙을 CLAUDE.md에 추가
