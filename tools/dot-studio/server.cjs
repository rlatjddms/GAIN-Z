// GAIN-Z 도트 공방 — 스프라이트 PNG 편집기 서버 (의존성 없음, Node 18+)
//   실행: node tools/dot-studio/server.cjs   (또는 dot-studio.cmd 더블클릭)
//   브라우저가 자동으로 열리고, 창을 닫으면 서버도 끝난다.
//
// 원본: NEWMINJUN/HotelOfTheDead tools/pixelrig/paint (유민준). GAIN-Z 규격(24×32 레이어 PNG + 기준색 팔레트)에 맞게 포팅.
'use strict';
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const { exec } = require('node:child_process');

const ROOT = path.resolve(__dirname, '..', '..');
const WEB = path.join(__dirname, 'web');
const SPRITES = path.join(ROOT, 'frontend', 'src', 'assets', 'sprites');
const PALETTE_JS = path.join(ROOT, 'frontend', 'src', 'pixel', 'palette.js');
// 프론트 pixel/palette.js의 LAYER_ORDER와 같은 레이어 (하의·소품은 레벨 보상)
const LAYERS = ['body', 'bottom', 'shoes', 'outfit', 'waist', 'neck', 'hair', 'earrings', 'hat', 'glasses', 'instrument'];
const W = 24, H = 32;

// ---------- 정적 파일 (디스크에서 매번 읽음: 코드 고치면 새로고침으로 반영) ----------
const STATIC = {
  '/': ['index.html', 'text/html; charset=utf-8'],
  '/app.js': ['app.js', 'text/javascript; charset=utf-8'],
  '/app.css': ['app.css', 'text/css; charset=utf-8'],
};

// ---------- PNG 인코더 ----------
const CRC = (() => { const t = new Uint32Array(256); for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; t[n] = c >>> 0; } return t; })();
function crc32(buf) { let c = 0xffffffff; for (const b of buf) c = CRC[(c ^ b) & 0xff] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; }
function chunk(type, data) {
  const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
  const td = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4); crc.writeUInt32BE(crc32(td));
  return Buffer.concat([len, td, crc]);
}
function encodePNG(w, h, rgba) {
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) { raw[y * (w * 4 + 1)] = 0; rgba.copy(raw, y * (w * 4 + 1) + 1, y * w * 4, (y + 1) * w * 4); }
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw, { level: 9 })), chunk('IEND', Buffer.alloc(0))]);
}

// ---------- API ----------
const validLayer = (l) => LAYERS.includes(l);
const validName = (n) => /^[A-Z0-9_]{1,40}$/.test(n);
function send(res, code, body, type = 'application/json; charset=utf-8') {
  res.writeHead(code, { 'Content-Type': type, 'Cache-Control': 'no-store' });
  res.end(typeof body === 'string' || Buffer.isBuffer(body) ? body : JSON.stringify(body));
}
function readBody(req) {
  return new Promise((resolve, reject) => {
    const parts = []; let n = 0;
    req.on('data', (c) => { n += c.length; if (n > 5e6) reject(new Error('too large')); parts.push(c); });
    req.on('end', () => resolve(Buffer.concat(parts).toString('utf8')));
    req.on('error', reject);
  });
}
function listSprites() {
  const out = {};
  for (const layer of LAYERS) {
    const dir = path.join(SPRITES, layer);
    out[layer] = fs.existsSync(dir) ? fs.readdirSync(dir).filter((f) => f.endsWith('.png')).map((f) => f.slice(0, -4)).sort() : [];
  }
  return out;
}

let lastPing = Date.now(), pinged = false;
const server = http.createServer(async (req, res) => {
  try {
    const url = new URL(req.url, 'http://x');
    const p = decodeURIComponent(url.pathname);
    if (STATIC[p]) return send(res, 200, fs.readFileSync(path.join(WEB, STATIC[p][0])), STATIC[p][1]);
    if (p === '/pixel/palette.js') return send(res, 200, fs.readFileSync(PALETTE_JS), 'text/javascript; charset=utf-8');
    if (p === '/favicon.ico') return send(res, 204, '');
    if (p === '/api/ping') { lastPing = Date.now(); pinged = true; return send(res, 200, { ok: true }); }
    if (p === '/api/info') return send(res, 200, { root: ROOT, sprites: path.relative(ROOT, SPRITES).replace(/\\/g, '/'), layers: LAYERS, width: W, height: H });
    if (p === '/api/sprites') return send(res, 200, listSprites());

    const m = p.match(/^\/api\/sprite\/([a-z]+)\/([^/]+)$/);
    if (m) {
      const [, layer, name] = m;
      if (!validLayer(layer)) return send(res, 400, { error: '없는 레이어예요' });
      if (!validName(name)) return send(res, 400, { error: '이름은 영문 대문자·숫자·_ 만 돼요 (예: LONG_STRAIGHT)' });
      const file = path.join(SPRITES, layer, `${name}.png`);
      if (req.method === 'GET') {
        if (!fs.existsSync(file)) return send(res, 404, { error: '없는 스프라이트' });
        return send(res, 200, fs.readFileSync(file), 'image/png');
      }
      if (req.method === 'POST') {
        const body = JSON.parse(await readBody(req));
        const rgba = Buffer.from(body.rgba, 'base64');
        if (rgba.length !== W * H * 4) return send(res, 400, { error: `크기가 달라요 (${W}×${H} 필요)` });
        fs.mkdirSync(path.dirname(file), { recursive: true });
        fs.writeFileSync(file, encodePNG(W, H, rgba));
        return send(res, 200, { ok: true, file: path.relative(ROOT, file).replace(/\\/g, '/') });
      }
      if (req.method === 'DELETE') {
        if (fs.existsSync(file)) fs.unlinkSync(file);
        return send(res, 200, { ok: true });
      }
    }
    send(res, 404, { error: 'not found' });
  } catch (e) { send(res, 500, { error: String(e.message || e) }); }
});

function openBrowser(url) {
  const cmd = process.platform === 'win32' ? `start "" "${url}"` : process.platform === 'darwin' ? `open "${url}"` : `xdg-open "${url}"`;
  exec(cmd, () => {});
}

const PORT = Number(process.env.DOT_STUDIO_PORT || 5179);
server.listen(PORT, '127.0.0.1', () => {
  const url = `http://127.0.0.1:${server.address().port}/`;
  console.log(`GAIN-Z 도트 공방 → ${url}`);
  console.log(`스프라이트 폴더: ${SPRITES}`);
  if (process.argv.includes('--no-open')) return;
  openBrowser(url);
  // 창이 핑을 멈추면(탭을 닫음) 서버도 종료. 처음 창이 뜰 때까지는 1분 기다림
  const started = Date.now();
  setInterval(() => {
    const quiet = Date.now() - lastPing;
    if (pinged ? quiet > 8000 : Date.now() - started > 60000) process.exit(0);
  }, 2000);
});
