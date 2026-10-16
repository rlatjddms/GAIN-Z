# 밴드 게시판 API (#94)

모든 경로는 `/api/v1/bands/{bandId}/posts` 아래이며 로그인과 해당 밴드의 활성 멤버 자격이 필요하다.
응답은 `ApiResponse`의 `data`에 들어간다. 작성자 표시는 캐릭터 닉네임과 계정 프로필 사진을 사용한다.

| 메서드 | 경로 | 동작 |
| --- | --- | --- |
| GET | `/` | 공지 우선, 생성 시각·ID 내림차순 목록. `page=0`, `size=12`(최대 20), 선택 `notice=true/false` 필터 |
| POST | `/` | JSON `{title, content}` 또는 아래 multipart로 글 작성 |
| PUT | `/{postId}` | multipart로 본문·공지·첨부를 한 번에 수정 |
| GET | `/{postId}/attachments/{attachmentId}` | 여러 첨부 중 파일 조회, `download=true` 지원 |
| GET / PATCH / DELETE | `/{postId}` | 조회 / 작성자만 수정 / 작성자만 삭제 |
| PATCH | `/{postId}/notice` | 리더만 `{notice: true/false}`로 공지 지정·해제 |
| PUT | `/{postId}/attachment` | 작성자만 multipart `file` 한 개 등록·교체 |
| GET | `/{postId}/attachment` | 멤버만 파일 조회, `download=true`면 다운로드 |
| DELETE | `/{postId}/attachment` | 작성자만 첨부 제거 |
| GET / POST | `/{postId}/comments` | 댓글 목록 / `{content, parentId?}`로 댓글·대댓글 작성 |
| PATCH / DELETE | `/{postId}/comments/{commentId}` | 작성자만 댓글 수정·삭제 |

제목 100자, 본문 10,000자, 댓글 500자까지이며 공백만 있는 입력은 거부한다.
대댓글은 한 단계만 허용한다. 자식이 있는 댓글은 `deleted=true`, `author=null`, `content="삭제된 댓글"`로 남긴다.
댓글 목록은 평면 배열이며 `parentId`로 원댓글에 연결한다. 삭제 표시는 댓글 수에 포함하지 않는다.

목록은 `{content, number, totalPages, totalElements}` 형태다. 각 글은
`{id, title, content, notice, author, createdAt, mine, commentCount, attachment, attachments}`를 반환한다.
`author`는 `{userId, nickname, profileImageUrl}`, `attachments`는 `{id, name, type, url}` 배열이며, `attachment`는 호환용 첫 파일 또는 null이다.
브라우저의 사진·영상 요청에도 인증이 필요하므로 프론트에서 토큰을 포함해 blob으로 받은 뒤 표시한다.

multipart의 `post` 파트는 application/json인 `{title, content, notice, keepAttachmentIds}`이고, `files` 파트는 새 파일들을 반복해서 보낸다. 수정 시 유지할 첨부 ID만 전달하며 제외한 파일은 삭제된다. 기존 단일 첨부 ID는 `legacy`다. 공지 변경은 리더만 가능하다. 글당 최대 8개이며 본문·공지·파일을 원자적으로 저장한다. 실패하면 글 변경과 새 파일을 함께 롤백한다.

첨부는 파일당 10MB까지 PNG/JPEG/WEBP, MP4/WebM, MP3/WAV, PDF를 받는다. 실제 헤더를 검사하고 UUID 이름으로
`gainz.upload-dir/band`에 저장한다. HTML/SVG 등은 거부한다. DB 롤백 때 새 파일을 지우며, 커밋 후 교체 전 파일을 정리한다.
파일 응답은 `private, no-store`와 `nosniff`를 사용한다.

검증: `backend`에서 `./gradlew test` (Windows: `gradlew.bat test`). 서비스·저장소 파일 처리·독립 MockMvc 테스트이며 실제 MariaDB/Redis 통합 검증은 포함하지 않는다.
