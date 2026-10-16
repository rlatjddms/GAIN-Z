// 밴드 커뮤니티 미리보기 글. 게시판 API(#94)를 붙이면 지운다
// authorIndex: 밴드 멤버 중 몇 번째가 쓴 글로 보여 줄지 (멤버 수로 나눈 나머지)
// minutesAgo: 지금부터 몇 분 전에 쓴 글인지
export const PLACEHOLDER_NOTICE = {
  id: 'notice',
  authorIndex: 0,
  title: '이번 주 일요일 오후 2시 정기 합주',
  content: '홍대 A-1룸 3시간 잡아 뒀어요. 셋리스트 1~4번 위주로 맞춰 봐요.',
  minutesAgo: 60 * 5,
  commentCount: 3,
}

export const PLACEHOLDER_POSTS = [
  {
    id: 1,
    authorIndex: 1,
    title: '베이스 레이어 올렸어요',
    content: '2절 들어가기 전 필인 부분 살짝 바꿔 봤는데 들어 보고 의견 주세요!',
    minutesAgo: 25,
    commentCount: 2,
    attachment: 'bass_take3.mp3',
  },
  {
    id: 2,
    authorIndex: 2,
    title: '드럼 템포 이야기',
    content: '후렴에서 자꾸 빨라지는 것 같아서 다음 합주 때는 클릭 켜고 해 보면 어떨까요?',
    minutesAgo: 60 * 3,
    commentCount: 5,
  },
  {
    id: 3,
    authorIndex: 0,
    title: '공연 신청 마감 다음 주 금요일',
    content: '학교 축제 무대 신청서 제출해야 해요. 곡 순서 확정되면 바로 낼게요.',
    minutesAgo: 60 * 26,
    commentCount: 1,
  },
  {
    id: 4,
    authorIndex: 3,
    title: '코드 악보 정리본',
    content: '브릿지 코드 다시 따서 올려 둡니다. 키는 원곡 그대로예요.',
    minutesAgo: 60 * 50,
    commentCount: 0,
    attachment: 'bridge_chords.pdf',
  },
  {
    id: 5,
    authorIndex: 1,
    title: '지난 합주 녹음 들어 봤는데',
    content: '보컬이랑 기타 볼륨 밸런스가 좀 안 맞았던 것 같아요. 앰프 위치 바꿔 봐요.',
    minutesAgo: 60 * 72,
    commentCount: 4,
  },
  {
    id: 6,
    authorIndex: 2,
    title: '새 곡 추천 받아요',
    content: '다음 셋리스트에 넣을 신나는 곡 하나씩만 댓글로 남겨 주세요 🎸',
    minutesAgo: 60 * 24 * 4,
    commentCount: 7,
  },
]
