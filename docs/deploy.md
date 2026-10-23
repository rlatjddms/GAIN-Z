# 배포 가이드 (Render + Supabase + Vercel)

GAIN-Z를 무료로 인터넷에 올리는 방법입니다 (#180). 세 서비스 모두 GitHub 계정으로 가입하고, 카드 등록은 필요 없습니다.

| 하는 일 | 서비스 | 무료 범위 |
| --- | --- | --- |
| 화면(프론트) | Vercel | 무료, HTTPS 자동 |
| 백엔드 | Render (Docker) | RAM 512MB, 15분 동안 요청이 없으면 잠듦 → 10분마다 깨움 |
| 로그인 유지(Redis) | Render Key Value | 25MB, 재시작되면 다시 로그인해야 함 |
| DB | Supabase PostgreSQL | 500MB |
| 올린 파일 | Supabase Storage | 1GB |

레포에 들어 있는 설정 파일:

- `render.yaml`: Render 백엔드 + Key Value (Blueprint)
- `frontend/vercel.json`: Vercel 빌드·SPA 설정
- `.github/workflows/keep-awake.yml`: 10분마다 백엔드를 깨우는 GitHub Actions

세 곳 모두 **`main` 브랜치**를 배포합니다. `dev` → `main` 릴리스 PR이 머지된 뒤에 아래를 진행합니다. 그 뒤로는 `main`에 머지될 때마다 자동으로 다시 배포됩니다.

> 아래 주소는 예시입니다. Render 서비스 이름 `gainz-backend` → `https://gainz-backend.onrender.com`, Vercel 프로젝트 이름 `gain-z` → `https://gain-z.vercel.app`. 이름이 이미 쓰이고 있으면 뒤에 글자가 붙으니, 실제로 정해진 주소로 바꿔 넣습니다.

## 1. Supabase (DB·파일)

1. [supabase.com](https://supabase.com) → **Start your project** → GitHub로 로그인
2. **New project**
   - Name: `gainz`
   - Database Password: **Generate** 후 복사해서 따로 적어 둡니다 (아래 `DB_PASSWORD`)
   - Region: **Southeast Asia (Singapore)**. Render 백엔드와 같은 곳이어야 DB가 빠릅니다
3. 프로젝트가 만들어지면 위쪽 **Connect** → **Session pooler** 탭의 주소를 봅니다.
   `postgresql://postgres.abcdefgh:[YOUR-PASSWORD]@aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres` 이면
   - `DB_URL` = `jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require`
   - `DB_USERNAME` = `postgres.abcdefgh`
   - `DB_PASSWORD` = 2번에서 적어 둔 비밀번호
   - Direct connection은 IPv6 전용이라 Render에서 안 붙고, Transaction pooler(6543)는 JPA와 맞지 않습니다. **Session pooler**를 씁니다.
4. 왼쪽 **Storage** → **New bucket**
   - Name: `uploads`
   - **Public bucket은 끈 채로** 만듭니다. 파일은 백엔드가 권한을 확인한 뒤에만 내보냅니다
5. 왼쪽 아래 **Project Settings** → **API Keys**
   - `SUPABASE_URL` = Project URL (`https://abcdefgh.supabase.co`)
   - `SUPABASE_SERVICE_KEY` = **service_role** 키(Legacy 탭, `eyJ…`) 또는 **Secret key**(`sb_secret_…`). 서버 전용 키라 프론트나 레포에 넣지 않습니다

테이블은 백엔드가 처음 뜰 때 자동으로 만듭니다.

## 2. Render (백엔드)

1. [render.com](https://render.com) → **Get Started** → GitHub로 로그인 → GAIN-Z 레포 접근 허용
2. **New** → **Blueprint** → `GAIN-Z` 레포 선택, Branch `main`
3. `render.yaml`을 읽어 `gainz-backend`(웹 서비스)와 `gainz-redis`(Key Value)가 보입니다. 비어 있는 값을 채웁니다.

   | 키 | 값 |
   | --- | --- |
   | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | 1-3에서 만든 값 |
   | `SUPABASE_URL`, `SUPABASE_SERVICE_KEY` | 1-5에서 복사한 값 |
   | `FRONTEND_URL` | Vercel 주소 (예: `https://gain-z.vercel.app`, 끝에 `/` 없이). 3단계에서 주소가 달라지면 고칩니다 |
   | `GOOGLE_CLIENT_ID` 등 6개 | 로컬 `application-secret.yml`에 쓰던 소셜 로그인 값 |

   `JWT_SECRET`은 Render가 알아서 만들고, 나머지(메모리 설정·Redis 주소 등)는 `render.yaml`에 이미 들어 있습니다.
4. **Apply** → 첫 빌드는 5~10분 걸립니다. 서비스 화면의 **Logs**에서 `Tomcat started`가 보이면 됩니다.
5. 브라우저로 `https://gainz-backend.onrender.com/api/v1/health` 를 열어 `"data":"ok"`가 보이면 백엔드와 DB 연결이 끝났습니다.

## 3. Vercel (프론트)

1. [vercel.com](https://vercel.com) → **Sign Up** → Continue with GitHub
2. **Add New…** → **Project** → `GAIN-Z` **Import**
3. 설정
   - Project Name: `gain-z`
   - Root Directory: **Edit** → `frontend`
   - Environment Variables: `VITE_API_BASE_URL` = `https://gainz-backend.onrender.com` (끝에 `/` 없이)
4. **Deploy** → 끝나면 나오는 주소(예: `https://gain-z.vercel.app`)를 엽니다.
5. 주소가 2-3에서 넣은 `FRONTEND_URL`과 다르면 Render → `gainz-backend` → **Environment**에서 `FRONTEND_URL`을 고치고 저장합니다 (자동으로 다시 배포됨).

`VITE_API_BASE_URL`은 빌드할 때 박히므로, 바꾸면 Vercel에서 **Redeploy**를 해야 반영됩니다.

## 4. 소셜 로그인 콘솔

소셜 로그인이 끝나고 돌아오는 주소는 **백엔드** 주소입니다: `https://gainz-backend.onrender.com/login/oauth2/code/{google|kakao|naver}`. 로컬용 주소는 지우지 말고 하나 더 추가합니다.

- **Google** ([Google Cloud Console](https://console.cloud.google.com/apis/credentials)) → 사용자 인증 정보 → OAuth 클라이언트 → **승인된 리디렉션 URI**에 `https://gainz-backend.onrender.com/login/oauth2/code/google` 추가
  - OAuth 동의 화면이 "테스트" 상태면 테스트 사용자로 등록한 계정만 로그인됩니다. 누구나 쓰게 하려면 **앱 게시**를 합니다
- **Kakao** ([Kakao Developers](https://developers.kakao.com)) → 내 애플리케이션
  - 앱 설정 → 플랫폼 → **Web 사이트 도메인**에 `https://gain-z.vercel.app`, `https://gainz-backend.onrender.com` 추가
  - 카카오 로그인 → **Redirect URI**에 `https://gainz-backend.onrender.com/login/oauth2/code/kakao` 추가
- **Naver** ([Naver Developers](https://developers.naver.com/apps)) → 내 애플리케이션 → API 설정
  - 서비스 URL에 `https://gain-z.vercel.app`, **Callback URL**에 `https://gainz-backend.onrender.com/login/oauth2/code/naver` 추가
  - 검수 전에는 "멤버관리"에 등록한 계정만 로그인됩니다

## 5. 깨우기 (GitHub Actions)

GitHub 레포 → **Settings** → **Secrets and variables** → **Actions** → **Variables** 탭 → **New repository variable**

- Name: `BACKEND_URL`
- Value: `https://gainz-backend.onrender.com`

그 뒤로 10분마다 `keep-awake` 워크플로가 헬스 체크를 부릅니다. **Actions** 탭 → keep-awake → **Run workflow**로 바로 한 번 돌려 볼 수 있습니다. 60일 동안 레포에 커밋이 없으면 GitHub가 예약 실행을 멈추니, 그때는 Actions 탭에서 다시 켭니다.

## 6. 확인

- [ ] Vercel 주소에서 이메일 회원가입·로그인
- [ ] 구글·카카오·네이버 로그인 후 앱으로 돌아오기
- [ ] 악보 올리기 → Supabase Storage `uploads` 버킷에 `sheets/` 폴더가 생기는지
- [ ] 프로필 사진 바꾸기, 밴드 첨부·녹음 올리고 재생
- [ ] 친구 요청을 보내 실시간 알림이 오는지

## 알아 둘 것

- 백엔드가 잠들어 있으면 첫 요청이 1분쯤 걸립니다. 깨우기가 켜져 있으면 거의 없습니다.
- Render Key Value(무료)는 디스크에 남지 않아, 재시작되면 모두 다시 로그인해야 합니다.
- 운영에서도 `DDL_AUTO=update`로 둡니다. 새 엔티티·컬럼은 배포 때 자동으로 생기지만, 컬럼 이름을 바꾸거나 지우는 변경은 Supabase **SQL Editor**에서 직접 해야 합니다.
- Supabase 무료 프로젝트는 7일 동안 쓰지 않으면 멈춥니다. 깨우기가 DB까지 한 번씩 물어서 막아 줍니다. 멈췄다면 Supabase 대시보드에서 **Restore**를 누릅니다.
- 로그는 Render → `gainz-backend` → **Logs**에서 봅니다.
