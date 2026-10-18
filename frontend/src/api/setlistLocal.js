// API 연결 전까지 브라우저에 밴드·계정별로 보관한다. 첨부 원본도 Blob으로 보관한다.
function database() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('gainz-setlists', 1)
    request.onupgradeneeded = () => request.result.createObjectStore('bands')
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}
export async function readLocalSetlists(key) {
  const db = await database()
  try {
    return await new Promise((resolve, reject) => {
      const request = db.transaction('bands').objectStore('bands').get(key)
      request.onsuccess = () => resolve(request.result || [])
      request.onerror = () => reject(request.error)
    })
  } finally { db.close() }
}
export async function writeLocalSetlists(key, lists) {
  const db = await database()
  try {
    await new Promise((resolve, reject) => {
      const tx = db.transaction('bands', 'readwrite')
      tx.objectStore('bands').put(lists, key)
      tx.oncomplete = resolve
      tx.onerror = () => reject(tx.error)
      tx.onabort = () => reject(tx.error)
    })
  } finally { db.close() }
}
