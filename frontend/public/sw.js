// GAIN-Z 서비스 워커 (#143): 홈 화면에 설치할 수 있게 하고, 화면 파일만 캐시한다
// - API(다른 주소의 백엔드)는 건드리지 않는다: 같은 주소(origin)의 GET 요청만 다룬다
// - 페이지 이동: 네트워크 먼저, 안 되면 저장해 둔 첫 화면(index.html)
// - 빌드 파일(/assets/…, 이름에 해시가 붙음)과 아이콘: 캐시 먼저, 없으면 받아서 저장
// 캐시 내용을 바꾸면 VERSION을 올린다 (예전 캐시는 activate에서 지운다)
const VERSION = 'gainz-v1'
const SHELL = ['/', '/manifest.webmanifest', '/icons/icon-192.png', '/icons/icon-512.png']

self.addEventListener('install', (event) => {
  event.waitUntil(caches.open(VERSION).then((cache) => cache.addAll(SHELL)).then(() => self.skipWaiting()))
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((key) => key !== VERSION).map((key) => caches.delete(key))))
      .then(() => self.clients.claim()),
  )
})

self.addEventListener('fetch', (event) => {
  const { request } = event
  if (request.method !== 'GET') return
  const url = new URL(request.url)
  if (url.origin !== self.location.origin) return

  if (request.mode === 'navigate') {
    event.respondWith(
      fetch(request)
        .then((response) => {
          const copy = response.clone()
          caches.open(VERSION).then((cache) => cache.put('/', copy))
          return response
        })
        .catch(() => caches.match('/')),
    )
    return
  }

  if (url.pathname.startsWith('/assets/') || url.pathname.startsWith('/icons/')) {
    event.respondWith(
      caches.match(request).then((cached) => cached || fetch(request).then((response) => {
        if (response.ok) {
          const copy = response.clone()
          caches.open(VERSION).then((cache) => cache.put(request, copy))
        }
        return response
      })),
    )
  }
})
