// pdf.js는 PDF를 열 때만 불러온다 (#167). 오래된 브라우저(iOS Safari 등)도 열리도록 legacy 빌드.
// 파일 수준에 두어 PDF가 여러 개 열려도 한 번만 불러오고 워커도 하나만 쓴다
let pdfjsPromise = null
export const loadPdfjs = () => {
  // 워커는 Vite가 .js로 묶게 한다 (.mjs는 서버에 따라 MIME 형식이 틀려서 못 여는 경우가 있음)
  pdfjsPromise ||= Promise.all([
    import('pdfjs-dist/legacy/build/pdf.mjs'),
    import('pdfjs-dist/legacy/build/pdf.worker.min.mjs?worker'),
  ]).then(([pdfjs, { default: PdfWorker }]) => {
    pdfjs.GlobalWorkerOptions.workerPort = new PdfWorker()
    return pdfjs
  })
  return pdfjsPromise
}

// 받아 둔 blob: 주소의 PDF 쪽마다 세로/가로 비율 (크게 보기에서 쪽을 미리 펼치고 화면에 맞출 때, #231). 못 열면 null
export async function pdfPageRatios(url) {
  const pdfjs = await loadPdfjs()
  const bytes = await fetch(url).then((r) => r.arrayBuffer())
  const task = pdfjs.getDocument({ data: new Uint8Array(bytes), isEvalSupported: false })
  try {
    const doc = await task.promise
    const ratios = []
    for (let n = 1; n <= doc.numPages; n++) {
      const { width, height } = (await doc.getPage(n)).getViewport({ scale: 1 })
      ratios.push(height / width)
    }
    return ratios
  } catch {
    return null
  } finally {
    try { task.destroy() } catch { /* 이미 닫힘 */ }
  }
}
