# 배포 가이드 (Render + Supabase + Vercel)

GAIN-Z를 무료로 인터넷에 올리는 방법입니다 (#180, 실제 배포 후 #183에서 고침, 깨우기는 #195에서 UptimeRobot으로). 세 서비스 모두 GitHub 계정으로 가입하고, 카드 등록은 필요 없습니다.

| 하는 일 | 서비스 | 무료 범위 |
| --- | --- | --- |
| 화면(프론트) | Vercel | 무료, HTTPS 자동 |
| 백엔드 | Render (Docker) | RAM 512MB, 15분 동안 요청이 없으면 잠듦 → UptimeRobot이 5분마다 깨움 |
| 로그인 유지(Redis) | Render Key Value | 25MB, 재시작되면 다시 로그인해야 함 |
| DB | Supabase PostgreSQL | 500MB |
| 올린 파일 | Supabase Storage | 1GB |

지금 운영 중인 주소:

| | 주소 |
| --- | --- |
| 사이트 (Vercel) | https://gainz-band.vercel.app |
| 백엔드 (Render) | https://gainz-backend-3y99.onrender.com |
| 헬스 체크 | https://gainz-backend-3y99.onrender.com/api/v1/health |

레포에 들어 있는 설정 파일:

- `render.yaml`: Render 백엔드 + Key Value (Blueprint)
- `frontend/vercel.json`: Vercel 빌드·SPA 설정

깨우기(UptimeRobot)는 레포 밖에서 설정합니다 (5단계).

## 브랜치

- 이 레포의 **기본 브랜치는 `dev`**입니다. 기능 PR은 전부 `dev`로 머지합니다.
- **운영 배포는 `main`**입니다. `dev` → `main` 릴리스 PR을 **merge commit**으로 머지하면 Render와 Vercel이 새 버전을 올립니다. squash로 머지하면 `main`과 `dev`의 기록이 갈라져 다음 릴리스에서 충돌이 납니다.
- Render는 `render.yaml`에 `branch: main`이 적혀 있어 따로 설정할 것이 없습니다. **Vercel은 기본 브랜치(`dev`)를 운영으로 배포하므로 3-5에서 `main`으로 바꿔야 합니다.**

## 주소 이름

Render·Vercel 주소는 모든 사용자가 같이 쓰는 이름 공간입니다. 원하는 이름을 누가 이미 쓰고 있으면 **뒤에 글자가 붙습니다** (예: `gainz-backend` → `gainz-backend-3y99.onrender.com`). 이름만 보고 주소를 짐작하지 말고, 만든 뒤 각 서비스 화면에서 실제 주소를 복사해 씁니다. 짐작한 주소는 남의 서비스일 수 있습니다.

- Render 백엔드 주소는 서비스 화면의 이름 바로 아래에 있습니다. 무료로는 바꿀 수 없습니다.
- Vercel 주소는 Settings → **Domains**에서 비어 있는 `<이름>.vercel.app`을 추가해 무료로 바꿀 수 있습니다. 옛 주소는 새 주소로 Redirect 해 둡니다.

## 1. Supabase (DB·파일)

### 1-1. 프로젝트 만들기

[supabase.com](https://supabase.com) → GitHub로 로그인 → **New project**

| 칸 | 값 |
| --- | --- |
| Organization | 쓰는 것 그대로 (Free) |
| GitHub (optional) | **연결하지 않음.** `supabase/` 마이그레이션을 쓰지 않고, 테이블은 백엔드(Hibernate)가 만듭니다 |
| Project name | 아무거나 (예: `gain-z`) |
| Database password | **Generate a password** → 바로 복사해 둠 (`DB_PASSWORD`). 잊으면 Project Settings → Database에서 다시 만듭니다 |
| Region | **Southeast Asia (Singapore)**. Render 무료에 서울 리전이 없어 백엔드가 싱가포르에 있으므로 DB도 같은 곳에 둡니다. 리전은 나중에 못 바꿉니다 |
| Enable Data API | **끔.** 브라우저에서 DB를 직접 부르지 않으므로 필요 없고, 꺼야 DB가 밖에 드러나지 않습니다. Storage는 이것과 상관없이 동작합니다 |
| Automatically expose new tables | 끔 |
| Enable automatic RLS | 켬. 테이블 소유자(백엔드)는 RLS를 받지 않으므로 백엔드는 막히지 않습니다 |

"Incident in progress" 안내가 있으면 생성이 늦거나 실패할 수 있습니다. [status.supabase.com](https://status.supabase.com)을 보고 다시 시도하되, 다른 리전으로 바꾸지는 않습니다.

### 1-2. DB 접속 값

프로젝트 위쪽 **Connect** → Type **JDBC** → **Session pooler**. 이런 주소가 나옵니다.

```
jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?user=postgres.abcdefghijkl&password=[YOUR-PASSWORD]
```

| Render 키 | 값 |
| --- | --- |
| `DB_URL` | `?` 앞부분 + `?sslmode=require` → `jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require` |
| `DB_USERNAME` | `user=` 뒤 값 → `postgres.abcdefghijkl` (점 뒤 프로젝트 ID까지) |
| `DB_PASSWORD` | 1-1에서 복사한 비밀번호 (대괄호 없이) |

- 포트가 **5432**이고 호스트가 `pooler.supabase.com`이어야 합니다. Direct connection(`db.<ID>.supabase.co`)은 IPv6 전용이라 Render에서 안 붙고, Transaction pooler(6543)는 JPA와 맞지 않습니다.
- 호스트(`aws-0-…`/`aws-1-…`)는 프로젝트마다 다르니 화면 값을 그대로 씁니다.

### 1-3. 파일 버킷

왼쪽 **Storage** → **New bucket** → 이름 `uploads`, **Public bucket은 끈 채로** 만듭니다. 파일은 백엔드가 로그인·권한을 확인한 뒤에만 내보냅니다. 정책(Policies)은 만들지 않아도 됩니다 (서버용 키는 정책을 거치지 않음).

### 1-4. 프로젝트 주소와 서버용 키

| Render 키 | 어디서 |
| --- | --- |
| `SUPABASE_URL` | `https://<프로젝트 ID>.supabase.co`. 1-2의 `postgres.` 뒤 ID로 만들 수 있습니다. 끝에 `/` 없이 |
| `SUPABASE_SERVICE_KEY` | Project Settings → **API Keys** → **Legacy API keys** 탭의 `service_role`(`eyJ…`). 없으면 Secret keys의 `sb_secret_…`. `anon`·`publishable` 키는 아닙니다 |

서버용 키는 DB·파일 전체에 접근할 수 있습니다. Render 환경 변수에만 넣고 Vercel·레포·채팅에는 넣지 않습니다.

테이블은 백엔드가 처음 뜰 때 만듭니다 (Table Editor에 29개).

## 2. Render (백엔드)

1. [render.com](https://render.com) → GitHub로 로그인 → GAIN-Z 레포 접근 허용
2. **New** → **Blueprint** → Blueprint Name `GAIN-Z`, Branch **`main`**, Blueprint Path `render.yaml`
   - `main`에 `render.yaml`이 있어야 합니다. 릴리스 전이면 "파일을 찾을 수 없다"가 나옵니다.
3. `gainz-backend`(웹 서비스)와 `gainz-redis`(Key Value)가 보입니다. 비어 있는 값을 채웁니다.

   | 키 | 값 |
   | --- | --- |
   | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | 1-2 |
   | `SUPABASE_URL`, `SUPABASE_SERVICE_KEY` | 1-4 |
   | `FRONTEND_URL` | Vercel 사이트 주소 (끝에 `/` 없이). 아직 모르면 임시로 넣고 3-6에서 고칩니다 |
   | `GOOGLE_CLIENT_ID` 등 6개 | 로컬 `application-secret.yml`에 쓰던 소셜 로그인 값 |

   `JWT_SECRET`은 Render가 만들고, 메모리 설정·Redis 주소 등은 `render.yaml`에 들어 있습니다.
4. **Apply** → 첫 빌드 5~10분. **Logs**에 `Tomcat started`가 보이면 됩니다.
5. 서비스 화면에서 **실제 주소**를 복사하고, 뒤에 `/api/v1/health`를 붙여 엽니다. `{"success":true,…,"data":"ok"}`면 백엔드와 DB 연결까지 끝입니다.
   - `Cannot GET /api/v1/health`처럼 다른 문구가 나오면 남의 서비스 주소입니다 ([주소 이름](#주소-이름)).

환경 변수를 바꿔 저장하면 Render가 다시 배포합니다. 그동안 1~3분은 백엔드가 응답하지 않습니다.

## 3. Vercel (프론트)

1. [vercel.com](https://vercel.com) → Continue with GitHub
2. **Add New…** → **Project** → `GAIN-Z` **Import**
3. 설정
   - Root Directory: **Edit** → `frontend`
   - Environment Variables: `VITE_API_BASE_URL` = 2-5의 Render 실제 주소 (끝에 `/`, `/api/v1/health` 없이), Type은 **Config**
4. **Deploy**
5. **Settings → Environments → Production → Branch Tracking**을 **`main`**으로 바꾸고 Save. 기본값은 기본 브랜치(`dev`)라서, 안 바꾸면 `dev`에 머지할 때마다 운영 사이트가 바뀝니다. (Settings → Git에는 이 설정이 없습니다.)
6. 사이트 주소를 정하고(Settings → Domains), Render의 `FRONTEND_URL`과 **똑같이** 맞춥니다.

### `VITE_API_BASE_URL`은 Config로

- `VITE_`로 시작하는 값은 빌드 때 프론트 코드에 들어가 브라우저에 보입니다. 백엔드 주소는 원래 공개되는 값이라 괜찮습니다.
- Vercel이 "Remove the public framework prefix…"라고 경고하면 **Config**를 고르면 됩니다. **`VITE_`는 떼지 않습니다.** 떼면 Vite가 값을 넣지 않아 프론트가 `localhost:8080`을 부릅니다.
- **Secret**으로 만들었다면 값을 다시 볼 수도, Config로 바꿀 수도 없습니다. ⋯ → **Remove** 후 Config로 새로 만듭니다.
- 이 값은 빌드할 때 들어가므로, 바꾼 뒤에는 **Deployments → 최신 배포 ⋯ → Redeploy** (Use existing Build Cache 해제)를 해야 반영됩니다.

Domains에 주소가 여러 개 보이는 것은 정상입니다. `<프로젝트>.vercel.app`(또는 추가한 주소)가 운영 주소이고, `…-git-main-….vercel.app`(브랜치 주소)과 `…-<해시>-….vercel.app`(배포마다 생기는 주소)은 쓰지 않습니다.

## 4. 소셜 로그인 콘솔

로그인이 끝나고 돌아오는 주소(리다이렉트·콜백)는 **백엔드** 주소입니다. 로컬용 주소는 두고 하나 더 추가합니다.

| | 리다이렉트(콜백) | 사이트 도메인 |
| --- | --- | --- |
| Google ([Console](https://console.cloud.google.com/apis/credentials)) | `https://<백엔드>/login/oauth2/code/google` | — |
| Kakao ([Developers](https://developers.kakao.com)) | 카카오 로그인 → Redirect URI: `https://<백엔드>/login/oauth2/code/kakao` | 플랫폼 → Web 사이트 도메인: Vercel 주소, 백엔드 주소 |
| Naver ([Developers](https://developers.naver.com/apps)) | API 설정 → Callback URL: `https://<백엔드>/login/oauth2/code/naver` | 서비스 URL: Vercel 주소 |

- Google: OAuth 동의 화면이 "테스트"면 테스트 사용자로 등록한 계정만 로그인됩니다. 누구나 쓰게 하려면 **앱 게시**.
- Naver: 검수 전에는 "멤버관리"에 등록한 계정만 로그인됩니다.

## 5. 깨우기 (UptimeRobot)

Render 무료 백엔드는 15분 동안 요청이 없으면 잠들고, 깨는 데 1분쯤 걸립니다. 바깥에서 5분마다 헬스 체크를 불러 깨워 둡니다. 헬스 체크가 DB도 한 번 물어서 Supabase가 7일 미사용으로 멈추는 것도 막습니다.

1. [uptimerobot.com](https://uptimerobot.com) 무료 가입
2. **New Monitor**
   - Monitor Type: **HTTP(s)**
   - URL: `https://<백엔드>/api/v1/health`
   - Monitoring Interval: **5분**
   - 알림 받을 이메일 체크 (서버가 응답하지 않으면 메일이 옵니다)
3. 저장하면 바로 시작합니다. 대시보드에서 **Up**(초록)이 보이면 됩니다.

GitHub Actions 예약 실행은 쓰지 않습니다 (#195). 예약 실행이 붐빌 때 늦거나 빠지고, 레포가 비공개라 10분마다 돌면 무료 Actions 시간(월 2,000분)을 넘깁니다.

## 6. 확인

- [ ] 사이트에서 이메일 회원가입·로그인, 캐릭터 만들기
- [ ] 구글·카카오·네이버 로그인 후 앱으로 돌아오기
- [ ] 악보 올리기 → Supabase Storage `uploads`에 `sheets/` 폴더가 생기고 다시 열리는지
- [ ] 프로필 사진 바꾸기, 밴드 첨부·녹음 올리고 재생
- [ ] 친구 요청을 보내 실시간 알림이 오는지

## 주소가 바뀔 때 같이 바꿀 곳

**Vercel 사이트 주소**가 바뀌면:

1. Render → `gainz-backend` → Environment → `FRONTEND_URL` (끝에 `/` 없이) → 저장 (자동 재배포)
2. 카카오 Web 사이트 도메인, 네이버 서비스 URL

`FRONTEND_URL`이 실제 사이트 주소와 다르면 소셜 로그인은 (옛 주소의 Redirect 덕에) 되는 것처럼 보여도, 화면 안에서 부르는 API가 **CORS로 막혀** "서버에 연결하지 못했어요"가 뜹니다. 백엔드는 `FRONTEND_URL`(또는 `ALLOWED_ORIGINS`)에 적힌 출처만 허용합니다.

**Render 백엔드 주소**가 바뀌면 (서비스를 새로 만든 경우):

1. Vercel `VITE_API_BASE_URL` → Redeploy
2. 소셜 로그인 콘솔 3곳의 리다이렉트 주소, 카카오 Web 사이트 도메인
3. UptimeRobot 모니터 URL

## 문제가 생겼을 때

| 증상 | 원인과 할 일 |
| --- | --- |
| 검은 화면에 "SERVICE WAKING UP…" (Render 화면) | 백엔드가 잠들었다 깨는 중입니다. 1분쯤 기다리면 됩니다. 자주 보이면 UptimeRobot 모니터가 Up인지 확인합니다(5) |
| "서버에 연결하지 못했어요", Console에 `CORS` | Render `FRONTEND_URL`이 지금 사이트 주소와 다릅니다 |
| Console에 `Failed to fetch`/`ERR_…`, 헬스 체크도 안 열림 | 백엔드가 재배포 중이거나 꺼졌습니다. Render Events·Logs 확인 |
| 헬스 체크가 503 `db-down` | DB 연결 실패. Supabase가 **Paused**면 대시보드에서 Restore, 아니면 `DB_*` 값 확인 |

볼 곳:

- **브라우저** F12: Console(빨간 오류), Network(실패한 요청과 상태 코드)
- **Render** → `gainz-backend`: **Logs**(서버 로그, `ERROR` 검색), **Events**(배포 성공·실패, 재시작, Out of memory)
- **Vercel** → Deployments: 최신 배포가 Ready인지, Error면 Build Logs
- **Supabase**: 프로젝트 홈(Paused 여부), Logs & Analytics(Postgres·Storage 로그)

## 알아 둘 것

- Render Key Value(무료)는 디스크에 남지 않아, 재시작되면 모두 다시 로그인해야 합니다.
- 운영에서도 `DDL_AUTO=update`입니다. 새 엔티티·컬럼은 배포 때 자동으로 생기지만, 컬럼 이름 바꾸기·지우기는 Supabase **SQL Editor**에서 직접 합니다.
- Supabase 무료 프로젝트는 7일 동안 쓰지 않으면 멈춥니다. 멈춘 프로젝트는 대시보드에서 **Restore**로 되살립니다 (데이터는 그대로).
