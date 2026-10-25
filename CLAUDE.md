# GAIN-Z 작업 규칙

Claude Code와 팀원이 함께 따르는 규칙입니다. Claude는 세션을 시작할 때 이 파일을 자동으로 읽습니다.

## 프로젝트

- `backend/`: Spring Boot (Java 17, JPA, MariaDB, Redis). 테스트는 `./gradlew test` (Mockito 단위 테스트, DB 불필요).
- `frontend/`: Vue 3 + Vite + Pinia. 빌드는 `npm run build`. 테스트는 `npm test` (Playwright 브라우저 테스트, 가짜 API 응답, 약 40초). 개발 모드 전용 미리보기 `/dev/sprites`.
- `tools/dot-studio/`: 캐릭터 스프라이트 PNG 편집기. `node tools/dot-studio/server.cjs`.
- 비밀값은 `backend/src/main/resources/application-secret.yml`에만 두고 커밋하지 않습니다 (`.gitignore`에 있음).

## 역할

- **Claude는 PR까지만 합니다. 머지는 사람이 합니다.** Claude는 `gh pr merge`를 실행하지 않고, auto-merge도 켜지 않습니다.
- `main`, `dev`에 직접 push하지 않습니다. 모든 변경은 브랜치 → PR → 리뷰 → 머지입니다.
- 되돌리기 어려운 작업(브랜치 삭제, force push, 파일 삭제, 외부 서비스 호출)은 먼저 확인을 받습니다. 예외: Claude가 직접 만든 브랜치를 rebase 후 `--force-with-lease`로 올리는 것.

## 이슈

- `.github/ISSUE_TEMPLATE/issue-form.yml` 형식을 따릅니다 (작업 타입, 관련 에픽, 상세 내용, 체크리스트, 참고 자료).
- 제목: `[feat] 설명`, `[chore] 설명`, `[docs] 설명`, `[fix] 설명`, `[refactor] 설명`. 에픽은 `[epic] 설명`.
- 라벨은 **타입 1개 + 영역 1개 이상** 필수.
  - 타입: `Epic`, `Feature`, `Refactor`, `Bug`, `Docs`, `Chore`
  - 영역: `FE`, `BE`, `Common`
  - 예) 백엔드 기능 → `Feature, BE` / 풀스택 → `Feature, FE, BE` / 설정·문서 → `Chore, Common` 또는 `Docs, Common`
- 모든 작업 이슈는 **상위 에픽의 서브이슈**로 연결합니다. 본문에 `#에픽번호`를 적는 것만으로는 부족하고, GitHub 서브이슈로 실제로 붙여야 합니다.
  - 에픽: #1 초기 환경 구축 및 베이스라인, #2 유저·캐릭터 및 친구 시스템, #3 개인용 연습 워크스페이스, #4 밴드 협업 시스템, #268 알림 및 실시간 동기화, #269 서비스 운영 및 품질 개선
  - 기능은 그 기능의 에픽에 붙입니다. 여러 기능에 걸친 일(배포·저장소·CI, 공통 UI·문구, 버그, 코드 정리, 문서)은 #269에 붙입니다. #1은 처음 세팅·배포 준비에 해당하는 일만 붙입니다.
  - 하위 이슈가 다 끝나도 에픽은 열어 둡니다 (나중에 관련 작업이 더 생길 수 있음). 어느 에픽에도 안 맞는 큰 기능이 생기면 에픽을 새로 만듭니다.
  - Claude는 `gh issue create` 뒤에 GraphQL `addSubIssue`로 연결합니다. 다른 에픽으로 옮길 때는 `replaceParent: true`를 줍니다.
- 이슈 작업이 끝나면 체크리스트를 `[x]`로 갱신합니다. 이슈 닫기는 PR의 `Closes #N`에 맡깁니다.

## 브랜치

- 항상 최신 `origin/dev`에서 분기합니다.
- 이름: `feat/be-<이슈번호>`, `feat/fe-<이슈번호>`, `chore/common-<이슈번호>`, `docs/common-<이슈번호>`, `fix/<영역>-<이슈번호>`.
- 이슈 하나에 브랜치 하나, PR 하나가 원칙입니다.
- 앞 PR 위에 쌓아야 할 때(stacked PR)는 PR 본문에 의존 관계를 적고, **앞 PR이 머지되는 즉시 뒤 브랜치를 `origin/dev` 위로 rebase**합니다. squash 머지라서 rebase하지 않으면 충돌이 납니다.

## 커밋

- 형식: `[타입] 한국어 설명 (#이슈번호)` 한 줄. 타입은 `feat`, `fix`, `chore`, `test`, `docs`, `refactor`.
  - 예) `[feat] 연습 로그 기록 API 및 경험치 환산 로직 구현 (#15)`
- 기능 커밋과 테스트 커밋은 분리합니다 (`[feat] …`, `[test] …`).
- 하나의 커밋은 하나의 의미 단위로. 포맷팅만 바꾼 변경을 기능 커밋에 섞지 않습니다.

## PR

- base는 항상 `dev`. `main`으로는 릴리스 때만 올립니다.
- `.github/PULL_REQUEST_TEMPLATE.md`를 채웁니다. `Closes #이슈번호`, 작업 내용 체크리스트, 테스트 결과, 주의 사항.
- 제목은 이슈 제목과 같게 (`[feat] …`). 라벨은 이슈와 같게.
- 머지 방식은 squash. 머지 후 원격 브랜치는 삭제합니다.
- 레포에 CI는 없습니다. PR을 올리기 전에 로컬에서 백엔드 `./gradlew test`, 프론트 `npm run build`와 `npm test`를 통과시키고 결과를 PR에 적습니다. 화면을 바꿔서 프론트 테스트가 깨지면 테스트도 같이 고칩니다.

## 코드

- 백엔드: 도메인별 `controller / service / repository / entity / dto / enums` 구조. 응답은 `ApiResponse`, 예외는 `BusinessException + ErrorCode`. 새 에러 코드는 `ErrorCode`에 도메인 접두어(U, C, …)로 추가합니다.
- 프론트: `views`(페이지), `components`(재사용), `stores`(Pinia), `api`(axios), `constants`, `pixel`(픽셀 렌더링). 컴포넌트 스타일은 `scoped`.
- 프론트의 레벨·경험치 테이블(`constants/levelTier.js`)은 백엔드 `LevelPolicy`를 복사한 값입니다. 한쪽을 바꾸면 반드시 같이 바꿉니다.
- 주석과 UI 문구는 한국어, 식별자는 영어.

## 에셋 (픽셀 캐릭터)

- 캐릭터 스프라이트: `frontend/src/assets/sprites/<body|outfit|hair|instrument>/<ENUM>.png`, **24×32**, 투명 배경, 1배 크기. 파일명은 백엔드 enum 값(`HairStyle`, `InstrumentType`, `Gender`)과 같아야 자동 인식됩니다.
- 사용자가 색을 고르는 부분(머리·악기·의상)은 반드시 **기준색**으로만 칠합니다. 목록은 `frontend/src/assets/sprites/README.md`, 정의는 `frontend/src/pixel/palette.js`. 기준색을 바꾸면 두 곳을 함께 갱신합니다.
- 무대 배경: `frontend/src/assets/stages/<tier>.png`, **208×72**. CSS에서 5배 확대됩니다.
- 스프라이트 수정은 도트 공방(`tools/dot-studio/`)에서, 배경은 외부 도트 툴에서 크기만 지켜 수정합니다.
