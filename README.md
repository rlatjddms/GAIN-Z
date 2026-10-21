# GAIN-Z

악기 연습을 기록하면 캐릭터가 성장하고, 밴드를 만들어 악보·셋리스트·녹음을 나누는 연습 앱입니다.

## 구성

| 폴더 | 내용 | 명령 |
| --- | --- | --- |
| `backend/` | Spring Boot 3 (Java 17, JPA, MariaDB, Redis) | `./gradlew bootRun`, 테스트 `./gradlew test` |
| `frontend/` | Vue 3 + Vite + Pinia (PWA) | `npm run dev`, 빌드 `npm run build` |
| `tools/dot-studio/` | 캐릭터 스프라이트 PNG 편집기 | `node tools/dot-studio/server.cjs` |
| `docs/` | 설계·조사 문서 | |

작업 규칙(이슈·브랜치·커밋·PR)은 [CLAUDE.md](CLAUDE.md)에 있습니다.

## 로컬 실행

### 준비물

- Java 17, Node.js 18 이상
- MariaDB (기본 `localhost:3306`, DB 이름 `gain-z`)
- Redis (기본 `localhost:6379`)
- 소셜 로그인을 써 보려면 구글·카카오·네이버 개발자 콘솔의 클라이언트 ID/시크릿. 리다이렉트 URI는 `http://localhost:8080/login/oauth2/code/{google|kakao|naver}`

### 백엔드

```bash
cd backend
cp src/main/resources/application-secret.yml.example src/main/resources/application-secret.yml
# application-secret.yml 에 DB·Redis·소셜 로그인·JWT 비밀값을 채운다 (커밋되지 않음)
./gradlew bootRun
```

- `http://localhost:8080`에서 뜹니다. API 문서: `http://localhost:8080/swagger-ui/index.html`
- 테이블은 처음 띄울 때 JPA가 만듭니다 (`ddl-auto: update`).
- 올린 파일(악보·필기·녹음·프로필 사진)은 `~/.gainz/uploads`에 저장됩니다. `gainz.upload-dir`로 바꿀 수 있습니다.

### 프론트

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

- `http://localhost:5173`에서 뜹니다. `.env`의 `VITE_API_BASE_URL`이 백엔드 주소입니다 (기본 `http://localhost:8080`).
- 휴대폰으로 같은 와이파이에서 접속해 보려면 `npm run dev -- --host`로 띄우고, 백엔드 `application-secret.yml`에 `gainz.allowed-origins: http://localhost:5173,http://<내 IP>:5173`을 더합니다.

## 설정값

### 백엔드 (`gainz.*`)

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `gainz.frontend-url` | `http://localhost:5173` | 소셜 로그인이 끝나면 돌아갈 프론트 주소. CORS 기본 허용 출처 |
| `gainz.allowed-origins` | `frontend-url` 하나 | CORS 허용 출처. 쉼표로 여러 개 |
| `gainz.upload-dir` | `~/.gainz/uploads` | 올린 파일 저장 폴더 |

비밀값(DB·Redis·소셜 로그인 클라이언트·JWT 서명 키)은 로컬에서는 `application-secret.yml`, 운영에서는 환경 변수로 넣습니다. 소셜 로그인의 scope·provider 주소 같은 비밀 아닌 설정은 `application.yml`에 있습니다.

### 프론트 (`.env`)

| 키 | 기본값 | 설명 |
| --- | --- | --- |
| `VITE_API_BASE_URL` | `http://localhost:8080` | 백엔드 주소. 빈 값(`VITE_API_BASE_URL=`)이면 프론트와 같은 도메인으로 호출 |

Vite 환경 변수는 빌드 시점에 박히므로, 값을 바꾸면 `npm run build`를 다시 합니다.

## Docker로 띄우기

DB·Redis·백엔드·프론트를 한 번에 올리는 `docker-compose.yml`이 루트에 있습니다. 프론트 컨테이너의 nginx가 정적 파일을 서빙하면서 `/api`와 소셜 로그인 경로만 백엔드로 넘기므로, 한 도메인(기본 `http://localhost`)으로 전부 접속합니다.

```bash
cp .env.example .env
# .env 에 DB 비밀번호, JWT_SECRET, 소셜 로그인 클라이언트, FRONTEND_URL 을 채운다.
# 첫 실행은 테이블을 만들어야 하므로 .env 에 DDL_AUTO=update 를 한 줄 넣고 띄운 뒤, 뜨고 나면 그 줄을 지운다
docker compose up -d --build
```

- 이미지: `backend/Dockerfile`(Gradle 빌드 → JRE 17), `frontend/Dockerfile`(Vite 빌드 → nginx 1.27). nginx 설정은 `frontend/nginx.conf`.
- 데이터는 볼륨 `db-data`(MariaDB), `redis-data`, `uploads`(올린 파일)에 남습니다. `docker compose down -v`는 이 볼륨까지 지우니 조심합니다.
- 포트를 바꾸려면 `.env`에 `WEB_PORT=8089`처럼 적고, `FRONTEND_URL`도 `http://localhost:8089`로 맞춥니다.
- 소셜 로그인 콘솔의 리다이렉트 URI는 `${FRONTEND_URL}/login/oauth2/code/{google|kakao|naver}`입니다. 백엔드가 nginx의 `X-Forwarded-*` 헤더로 바깥 주소를 알아내므로 백엔드 포트를 밖으로 열 필요가 없습니다.
- HTTPS는 이 compose에 없습니다. 도메인이 정해지면 앞에 Caddy·certbot 같은 TLS 종단을 두거나 클라우드 로드밸런서를 씁니다. 앞단이 `X-Forwarded-Proto`를 보내면 nginx가 그대로 백엔드에 넘깁니다.
- 로그: `docker compose logs -f backend`

## 운영 실행 (prod 프로필)

백엔드를 `SPRING_PROFILES_ACTIVE=prod`로 띄우면 `application-prod.yml`이 적용되어 아래 환경 변수를 읽습니다.

| 환경 변수 | 필수 | 설명 |
| --- | --- | --- |
| `DB_URL` | ✅ | 예) `jdbc:mariadb://db:3306/gain-z?serverTimezone=Asia/Seoul` |
| `DB_USERNAME`, `DB_PASSWORD` | ✅ | DB 계정 |
| `REDIS_HOST` | ✅ | Redis 호스트. `REDIS_PORT`(기본 6379), `REDIS_PASSWORD`(없으면 생략) |
| `JWT_SECRET` | ✅ | 32자 이상의 서명 키 |
| `FRONTEND_URL` | ✅ | 프론트 주소. 예) `https://gainz.example.com` |
| `ALLOWED_ORIGINS` | | CORS 허용 출처 (쉼표 구분). 기본값은 `FRONTEND_URL` |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | ✅ | 구글 로그인 |
| `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET` | ✅ | 카카오 로그인 |
| `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET` | ✅ | 네이버 로그인 |
| `UPLOAD_DIR` | | 올린 파일 저장 폴더 (기본 `/data/uploads`). 지워지지 않는 볼륨에 둔다 |
| `DDL_AUTO` | | 기본 `validate`(스키마 검사만). 첫 배포처럼 테이블을 만들어야 할 때만 `update`로 한 번 띄운다 |
| `SWAGGER_ENABLED` | | 기본 `false`. API 문서를 운영에서 보려면 `true` |
| `SERVER_PORT` | | 기본 8080 |

운영에서 달라지는 점:

- SQL 로그를 끄고, 스키마는 검사만 합니다.
- 리버스 프록시(nginx 등) 뒤에서 `X-Forwarded-*` 헤더를 읽어 소셜 로그인 리다이렉트 URI의 `{baseUrl}`을 바깥 주소로 잡습니다. 프록시가 `X-Forwarded-Proto`, `X-Forwarded-Host`를 넘겨 줘야 합니다.
- 소셜 로그인 콘솔에 운영 리다이렉트 URI `https://<백엔드 도메인>/login/oauth2/code/{google|kakao|naver}`를 등록해야 합니다.

프론트는 `VITE_API_BASE_URL`을 운영 백엔드 주소로 두고 `npm run build`한 `dist/`를 정적 파일로 서빙합니다. 프론트와 백엔드를 같은 도메인에 두고 프록시가 `/api`, `/oauth2`, `/login/oauth2`를 백엔드로 넘기면 `VITE_API_BASE_URL`을 비워 두면 됩니다.
