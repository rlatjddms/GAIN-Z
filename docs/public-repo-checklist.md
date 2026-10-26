# 공개 레포 전환 체크리스트 (#296)

레포를 공개(Public)로 바꾸기 전에 확인할 것과, 2026-10-26에 점검한 결과입니다.

## 점검 결과 (2026-10-26, `dev` 기준)

| 본 곳 | 결과 |
|---|---|
| 지금 파일 | 실제 키·비밀번호 없음. 비밀값은 `application-secret.yml`·`.env`(둘 다 `.gitignore`)에만 있고, 레포에는 빈 예시 파일(`*.example`)만 있음 |
| **git 기록 전체** (커밋 701개, 모든 브랜치) | 실제 키 없음. Brevo(`xkeysib-`), 구글(`GOCSPX-`), JWT(`eyJ…`), Supabase(`sbp_`·`sb_secret_`), GitHub 토큰, AWS 키, 개인 키, 비밀번호가 든 DB 주소를 찾았는데 문서의 자리 표시(`[YOUR-PASSWORD]`)뿐 |
| 설정 파일 기록 (yml·env·compose·gradle) | 비밀값을 직접 적은 줄 없음. 모두 `${환경 변수}` |
| 이슈·PR 본문 | 키·토큰·개인 이메일 없음 |
| 문서의 주소 | 서비스 주소(Vercel·Render)만 있음. 원래 공개된 주소라 문제없음 |
| **커밋 작성자 이메일** | ⚠️ 커밋 540개에 개인 이메일이 작성자로 남아 있음. 공개하면 누구나 볼 수 있음 (아래 1번) |

## 공개로 바꾸기 전에 할 일

> 2026-10-26 공개로 바꿨어요. 2·3번은 설정을 마쳤고(Secret scanning·Push protection, Rulesets `protect-dev`·`release-main`), 1번은 이미 올라간 이메일이 남는 걸 알고 그대로 두기로 했어요.

1. **이메일 숨기기** (GitHub → Settings → Emails)
   - "Keep my email addresses private"와 "Block command line pushes that expose my email"을 켭니다.
   - 로컬 git 이메일을 GitHub가 준 `…@users.noreply.github.com` 주소로 바꿉니다: `git config --global user.email "<noreply 주소>"`
   - 이미 올라간 커밋의 이메일은 그대로 남습니다. 지우려면 기록 전체를 다시 써야 해서(force push, 모든 PR·브랜치 깨짐) 추천하지 않습니다. 남아도 괜찮은지 먼저 정합니다.
2. **비밀값 감시 켜기** (레포 → Settings → Code security)
   - Secret scanning과 **Push protection**을 켭니다 (공개 레포는 무료). 실수로 키를 커밋하면 GitHub가 push를 막아 줍니다.
3. **`main` 보호** (레포 → Settings → Rules → Rulesets)
   - `main`은 PR로만, 머지 방식은 **Merge만** 허용합니다. 릴리스를 squash로 합치면 `main`과 `dev`의 기록이 갈라집니다 (#290).
4. **로컬 비밀 파일 확인**
   - `git status`에 `application-secret.yml`, `.env`가 보이면 안 됩니다 (`.gitignore`에 있음). 새 비밀 파일을 만들면 이름을 `*-secret.yml`로 하거나 `.gitignore`에 먼저 넣습니다.
5. **혹시 새어 나간 키가 있으면** 기록을 지우는 것보다 **키를 새로 발급**하는 게 먼저입니다. Brevo, Supabase, 소셜 로그인(구글·카카오·네이버) 콘솔, Render `JWT_SECRET`이 대상입니다.

## 배포 설정 점검 (2026-10-26)

| 항목 | 상태 |
|---|---|
| 비밀값 | Render 환경 변수(`sync: false`), `JWT_SECRET`은 Render가 만듦(`generateValue`) |
| API 문서(Swagger) | 운영에서 꺼져 있음 (`SWAGGER_ENABLED` 기본 false) |
| CORS | 프론트 주소(`FRONTEND_URL`/`ALLOWED_ORIGINS`)만 허용 |
| SQL 로그 | 운영에서 꺼짐 |
| DB 스키마 | Render는 `DDL_AUTO=update`. 새 테이블·칸은 자동, 칸 이름 바꾸기·지우기는 SchemaPatcher나 SQL로 (`docs/setup.md` 고침) |
| 파일 | Supabase 비공개 버킷, 브라우저는 백엔드 API로만 받음 (권한 검사) |
| 백엔드 보안 헤더 | Spring Security 기본값 (`X-Frame-Options`, `X-Content-Type-Options` 등) |
| **프론트 보안 헤더** | 없었음 → `vercel.json`에 추가: `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy`, `Permissions-Policy`(마이크는 이 사이트만 — 레이어 잼 녹음) |
| Redis | 저장하는 키(로그인 유지 토큰·소셜 연결 표·소셜 로그인 코드)는 모두 만료 시간이 있음. 무료 Redis가 가득 차면 `allkeys-lru`라 오래된 키부터 지워져 일부 사용자가 다시 로그인해야 할 수 있음 (지금 규모에서는 괜찮음) |
