// GAIN-Z 도트 공방 — 레이어별 스프라이트 PNG 편집기
// 원본: NEWMINJUN/HotelOfTheDead tools/pixelrig/paint/app.js (유민준). 캔버스·도구·팔레트 부분을 가져와
// GAIN-Z 규격(24×32, 몸/의상/머리/악기 레이어, 기준색 치환 미리보기)에 맞게 다시 썼다.
import { SPRITE_WIDTH as W, SPRITE_HEIGHT as H, LAYER_ORDER, KEY_COLORS, buildSwapTable } from '/pixel/palette.js';

const $ = (s) => document.querySelector(s);
const $$ = (s) => [...document.querySelectorAll(s)];
const CELL = 32;

const LAYER_LABEL = { body: '몸', outfit: '의상', hair: '머리', instrument: '악기' };
const KEY_LABEL = {
  SKIN: '피부', SKIN_LIGHT: '피부 밝음', SKIN_SHADOW: '피부 그림자', SKIN_LINE: '피부 선',
  HAIR: '머리', HAIR_LIGHT: '머리 밝음', HAIR_DARK: '머리 그림자', HAIR_LINE: '머리 선',
  INSTRUMENT: '악기', INSTRUMENT_LIGHT: '악기 밝음', INSTRUMENT_DARK: '악기 그림자', INSTRUMENT_LINE: '악기 선',
  OUTFIT: '의상', OUTFIT_DARK: '의상 그림자', OUTFIT_LINE: '의상 선',
};
const FIXED_COLORS = [
  '#3a2622', '#ffffff', '#f4a5a0', '#c76a6a',   // 눈, 흰자, 볼, 입
  '#3f6db5', '#2f5490', '#203f6e', '#2b2d31', '#141518',   // 청바지, 청바지 그림자, 청바지 선, 신발, 신발 선
  '#c08a52', '#8e6035', '#e9e9ec', '#9b9ba3', '#f6f1e4', // 나무, 나무 그림자, 금속, 금속 그림자, 드럼헤드
];

// ---------- 상태 ----------
const S = {
  sprites: {}, layer: 'hair', name: null,
  grid: blank(), saved: null,
  tool: 'pen', mirror: false, others: true, showGrid: true,
  color: KEY_COLORS.HAIR.color, recent: [],
  zoom: 18, pan: { x: 0, y: 0 },
  undo: [], redo: [],
  others_cache: {},   // `${layer}/${name}` -> grid
  pv: { body: 'FEMALE', outfit: 'DEFAULT_FEMALE', hair: 'LONG_STRAIGHT', instrument: 'GUITAR',
        skinColor: '#F8D7B5', hairColor: '#EC4899', instrumentColor: '#B91C1C', outfitColor: '#FFFFFF' },
};
function blank() { return Array.from({ length: H }, () => Array(W).fill(null)); }
const clone = (g) => g.map((r) => [...r]);
const same = (a, b) => a.every((row, y) => row.every((c, x) => c === b[y][x]));
const isDirty = () => !S.saved || !same(S.grid, S.saved);

// ---------- 색 ----------
const hex2 = (n) => n.toString(16).padStart(2, '0');
const lum = (c) => { const n = parseInt(c.slice(1), 16); return (n >> 16) * 0.3 + ((n >> 8) & 255) * 0.59 + (n & 255) * 0.11; };
const norm = (c) => c.toLowerCase();
const mixHex = (a, b, t) => '#' + [1, 3, 5].map((i) => hex2(Math.round(parseInt(a.slice(i, i + 2), 16) + (parseInt(b.slice(i, i + 2), 16) - parseInt(a.slice(i, i + 2), 16)) * t))).join('');
const lighter = (c) => mixHex(c, '#fff4e0', 0.2);
const darker = (c) => mixHex(c, '#1a1020', 0.2);

function rgbaToGrid(rgba) {
  const g = blank();
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) {
    const i = (y * W + x) * 4;
    if (rgba[i + 3] >= 128) g[y][x] = '#' + hex2(rgba[i]) + hex2(rgba[i + 1]) + hex2(rgba[i + 2]);
  }
  return g;
}
function gridToRGBA(g) {
  const out = new Uint8ClampedArray(W * H * 4);
  g.forEach((row, y) => row.forEach((c, x) => {
    if (!c) return;
    const n = parseInt(c.slice(1), 16), i = (y * W + x) * 4;
    out[i] = n >> 16; out[i + 1] = (n >> 8) & 255; out[i + 2] = n & 255; out[i + 3] = 255;
  }));
  return out;
}
function b64(bytes) {
  let s = '';
  for (let i = 0; i < bytes.length; i += 0x8000) s += String.fromCharCode.apply(null, bytes.subarray(i, i + 0x8000));
  return btoa(s);
}
function pngUrlToGrid(url) {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => {
      const c = document.createElement('canvas'); c.width = W; c.height = H;
      const cx = c.getContext('2d'); cx.drawImage(img, 0, 0);
      resolve(rgbaToGrid(cx.getImageData(0, 0, W, H).data));
    };
    img.onerror = reject;
    img.src = url;
  });
}

// ---------- 서버 ----------
async function api(path, opts) {
  const r = await fetch(path, opts);
  const j = await r.json().catch(() => ({}));
  if (!r.ok) throw new Error(j.error || r.statusText);
  return j;
}
setInterval(() => fetch('/api/ping').catch(() => {}), 2000);
fetch('/api/ping');

async function loadGrid(layer, name) {
  const key = `${layer}/${name}`;
  if (S.others_cache[key]) return S.others_cache[key];
  const g = await pngUrlToGrid(`/api/sprite/${layer}/${name}?t=${Date.now()}`).catch(() => blank());
  S.others_cache[key] = g;
  return g;
}

// ---------- 알림 ----------
function toast(msg, kind = '', ms = 2600) {
  const t = document.createElement('div'); t.className = 'toast ' + kind; t.textContent = msg;
  $('#toasts').appendChild(t);
  setTimeout(() => { t.classList.add('out'); setTimeout(() => t.remove(), 260); }, ms);
}

// ---------- 그림 캔버스 ----------
const paint = $('#paint'), pctx = paint.getContext('2d');
const wrap = $('#canvasWrap'), area = $('#canvasArea'), cursor = $('#cursor');

function drawCell(ctx, x, y, c) { ctx.fillStyle = c; ctx.fillRect(x * CELL, y * CELL, CELL, CELL); }
function drawPaint() {
  pctx.clearRect(0, 0, paint.width, paint.height);
  for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) drawCell(pctx, x, y, (x + y) % 2 ? '#e9ebf0' : '#dfe2e8');
  // 다른 레이어: 실제 색으로 아래/위에 깔아서 맥락을 보여줌
  if (S.others) {
    const table = buildSwapTable(S.pv);
    const swap = (c) => { const n = parseInt(c.slice(1), 16); const k = `${n >> 16},${(n >> 8) & 255},${n & 255}`; const s = table.get(k); return s ? `rgb(${s[0]},${s[1]},${s[2]})` : c; };
    for (const layer of LAYER_ORDER) {
      if (layer === S.layer) { pctx.globalAlpha = 1; S.grid.forEach((row, y) => row.forEach((c, x) => c && drawCell(pctx, x, y, c))); continue; }
      const name = S.pv[layer]; const g = name && S.others_cache[`${layer}/${name}`];
      if (!g) continue;
      pctx.globalAlpha = LAYER_ORDER.indexOf(layer) < LAYER_ORDER.indexOf(S.layer) ? 0.55 : 0.3;
      g.forEach((row, y) => row.forEach((c, x) => c && drawCell(pctx, x, y, swap(c))));
    }
    pctx.globalAlpha = 1;
  } else {
    S.grid.forEach((row, y) => row.forEach((c, x) => c && drawCell(pctx, x, y, c)));
  }
  if (S.showGrid) {
    pctx.fillStyle = 'rgba(0,0,0,.07)';
    for (let x = 1; x < W; x++) pctx.fillRect(x * CELL - 1, 0, 2, H * CELL);
    for (let y = 1; y < H; y++) pctx.fillRect(0, y * CELL - 1, W * CELL, 2);
    pctx.fillStyle = 'rgba(45,91,255,.25)';
    pctx.fillRect(W / 2 * CELL - 1, 0, 2, H * CELL);
  }
}

function applyZoom(animate = true) {
  wrap.classList.toggle('no-anim', !animate);
  paint.style.width = W * S.zoom + 'px';
  paint.style.height = H * S.zoom + 'px';
  wrap.style.transform = `translate(${S.pan.x}px, ${S.pan.y}px)`;
  cursor.style.width = cursor.style.height = S.zoom + 'px';
  $('#zLabel').textContent = Math.round(S.zoom / 18 * 100) + '%';
}
function fitZoom() {
  const r = area.getBoundingClientRect();
  S.zoom = Math.max(6, Math.floor(Math.min((r.height - 90) / H, (r.width - 120) / W)));
  S.pan = { x: 0, y: 0 };
  applyZoom();
}
function setZoom(z, cx, cy) {
  const old = S.zoom;
  S.zoom = Math.max(6, Math.min(56, z));
  if (cx !== undefined) {
    const r = area.getBoundingClientRect();
    const ox = cx - (r.left + r.width / 2) - S.pan.x, oy = cy - (r.top + r.height / 2) - S.pan.y;
    S.pan.x -= ox * (S.zoom / old - 1); S.pan.y -= oy * (S.zoom / old - 1);
  }
  applyZoom();
}
function cellAt(e) {
  const r = paint.getBoundingClientRect();
  const x = Math.floor((e.clientX - r.left) / r.width * W), y = Math.floor((e.clientY - r.top) / r.height * H);
  return x >= 0 && x < W && y >= 0 && y < H ? { x, y } : null;
}
function moveCursor(c) {
  if (!c) { cursor.classList.remove('show'); return; }
  cursor.classList.add('show');
  cursor.style.transform = `translate(${c.x * S.zoom}px, ${c.y * S.zoom}px)`;
}

// ---------- 그리기 ----------
function pushUndo() {
  S.undo.push(clone(S.grid));
  if (S.undo.length > 120) S.undo.shift();
  S.redo = [];
  updateUndoButtons();
}
function setPx(g, x, y, c) {
  if (x < 0 || x >= W || y < 0 || y >= H) return;
  g[y][x] = c;
  if (S.mirror) g[y][W - 1 - x] = c;
}
function line(g, a, b, c) {
  let { x: x0, y: y0 } = a; const { x: x1, y: y1 } = b;
  const dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
  let err = dx + dy;
  for (;;) {
    setPx(g, x0, y0, c);
    if (x0 === x1 && y0 === y1) break;
    const e2 = 2 * err;
    if (e2 >= dy) { err += dy; x0 += sx; }
    if (e2 <= dx) { err += dx; y0 += sy; }
  }
}
function flood(g, x, y, c) {
  const target = g[y][x]; if (target === c) return;
  const st = [[x, y]];
  while (st.length) {
    const [px, py] = st.pop();
    if (px < 0 || px >= W || py < 0 || py >= H || g[py][px] !== target) continue;
    g[py][px] = c;
    st.push([px + 1, py], [px - 1, py], [px, py + 1], [px, py - 1]);
  }
}
function changed() {
  S.others_cache[`${S.layer}/${S.name}`] = S.grid;
  drawPaint(); renderUsed(); renderStatus(); renderListThumb(); drawPreview();
}

let stroke = null, panDrag = null, spaceDown = false, lastPt = null;
area.addEventListener('contextmenu', (e) => e.preventDefault());
area.addEventListener('pointerdown', (e) => {
  if (!S.name) { toast('왼쪽에서 스프라이트를 고르거나 추가하세요'); return; }
  if (e.button === 1 || spaceDown) {
    panDrag = { x: e.clientX, y: e.clientY, px: S.pan.x, py: S.pan.y };
    area.classList.add('panning'); wrap.classList.add('no-anim'); area.setPointerCapture(e.pointerId); return;
  }
  const c = cellAt(e); if (!c) return;
  const g = S.grid;
  const tool = e.button === 2 ? 'eraser' : e.altKey ? 'picker' : S.tool;
  if (tool === 'picker') { if (g[c.y][c.x]) pickColor(g[c.y][c.x]); return; }
  pushUndo();
  if (tool === 'fill') { flood(g, c.x, c.y, S.color); changed(); addRecent(S.color); return; }
  const col = tool === 'eraser' ? null : S.color;
  if (e.shiftKey && lastPt) line(g, lastPt, c, col); else setPx(g, c.x, c.y, col);
  if (col) addRecent(col);
  stroke = { col, last: c }; lastPt = c;
  area.setPointerCapture(e.pointerId);
  changed();
});
area.addEventListener('pointermove', (e) => {
  if (panDrag) { S.pan.x = panDrag.px + e.clientX - panDrag.x; S.pan.y = panDrag.py + e.clientY - panDrag.y; applyZoom(false); return; }
  const c = cellAt(e); moveCursor(c);
  if (stroke && c && (c.x !== stroke.last.x || c.y !== stroke.last.y)) {
    line(S.grid, stroke.last, c, stroke.col); stroke.last = c; lastPt = c; changed();
  }
});
const endStroke = () => { stroke = null; if (panDrag) { panDrag = null; area.classList.remove('panning'); } };
area.addEventListener('pointerup', endStroke);
area.addEventListener('pointercancel', endStroke);
area.addEventListener('pointerleave', () => moveCursor(null));
area.addEventListener('wheel', (e) => { e.preventDefault(); setZoom(Math.round(S.zoom * (e.deltaY < 0 ? 1.15 : 1 / 1.15)), e.clientX, e.clientY); }, { passive: false });

function undo(redo = false) {
  const from = redo ? S.redo : S.undo, to = redo ? S.undo : S.redo;
  if (!from.length) return;
  to.push(clone(S.grid));
  S.grid = from.pop();
  changed(); updateUndoButtons();
}
function updateUndoButtons() { $('#tUndo').disabled = !S.undo.length; $('#tRedo').disabled = !S.redo.length; }

// ---------- 도구 · 색 ----------
function setTool(t) {
  S.tool = t;
  $$('.tool[data-tool]').forEach((b) => b.classList.toggle('on', b.dataset.tool === t));
}
function toggle(key, btn) {
  S[key] = !S[key]; $(btn).classList.toggle('on', S[key]);
  drawPaint();
}
function pickColor(c, fromUser = true) {
  S.color = norm(c);
  $('#curSwatch').style.background = c;
  $('#colorInput').value = S.color;
  $('#hexInput').value = S.color.toUpperCase();
  $$('.sw').forEach((s) => s.classList.toggle('on', s.dataset.c === S.color));
  $$('.key').forEach((k) => k.classList.toggle('on', k.dataset.c === S.color));
  if (fromUser && S.tool !== 'pen' && S.tool !== 'fill') setTool('pen');
}
function addRecent(c) {
  c = norm(c);
  if (S.recent[0] === c) return;
  S.recent = [c, ...S.recent.filter((x) => x !== c)].slice(0, 12);
  renderSwatches($('#recentColors'), S.recent);
}
function swatch(c) {
  c = norm(c);
  const b = document.createElement('button'); b.className = 'sw' + (c === S.color ? ' on' : '');
  b.style.background = c; b.dataset.c = c; b.title = c.toUpperCase();
  b.onclick = () => pickColor(c);
  return b;
}
function renderSwatches(el, colors) {
  el.innerHTML = '';
  if (!colors.length) { el.innerHTML = '<span class="empty">아직 없음</span>'; return; }
  colors.forEach((c) => el.appendChild(swatch(c)));
}
function renderKeys() {
  const box = $('#keyColors'); box.innerHTML = '';
  // 현재 레이어에 맞는 기준색을 먼저
  const prefer = { body: 'SKIN', hair: 'HAIR', instrument: 'INSTRUMENT', outfit: 'OUTFIT' }[S.layer];
  const entries = Object.entries(KEY_COLORS).sort(([a], [b]) => (b.startsWith(prefer || '~') - a.startsWith(prefer || '~')));
  for (const [key, def] of entries) {
    const el = document.createElement('button'); el.className = 'key' + (norm(def.color) === S.color ? ' on' : '');
    el.dataset.c = norm(def.color); el.title = def.color;
    el.innerHTML = `<i style="background:${def.color}"></i><span>${KEY_LABEL[key] || key}</span>`;
    el.onclick = () => pickColor(def.color);
    box.appendChild(el);
  }
}
function renderUsed() {
  const set = new Set(S.grid.flat().filter(Boolean));
  renderSwatches($('#usedColors'), [...set].sort((a, b) => lum(a) - lum(b)));
}

// ---------- 레이어 · 목록 ----------
function renderLayerTabs() {
  const box = $('#layerTabs'); box.innerHTML = '';
  for (const layer of LAYER_ORDER) {
    const b = document.createElement('button'); b.textContent = LAYER_LABEL[layer]; b.classList.toggle('on', layer === S.layer);
    b.onclick = () => selectLayer(layer);
    box.appendChild(b);
  }
  $('#layerLabel').textContent = LAYER_LABEL[S.layer];
}
function renderList() {
  const ul = $('#sprites'); ul.innerHTML = '';
  for (const name of S.sprites[S.layer] || []) {
    const li = document.createElement('li'); li.classList.toggle('on', name === S.name); li.dataset.name = name;
    const c = document.createElement('canvas'); c.width = W; c.height = H;
    const label = document.createElement('span'); label.textContent = name;
    const del = document.createElement('button'); del.className = 'del'; del.textContent = '삭제'; del.title = '파일 삭제';
    del.onclick = (e) => { e.stopPropagation(); removeSprite(name); };
    li.append(c, label, del);
    li.onclick = () => selectSprite(name);
    ul.appendChild(li);
    loadGrid(S.layer, name).then((g) => c.getContext('2d').putImageData(new ImageData(gridToRGBA(g), W, H), 0, 0));
  }
}
function renderListThumb() {
  const li = $(`#sprites li[data-name="${S.name}"]`); if (!li) return;
  li.querySelector('canvas').getContext('2d').putImageData(new ImageData(gridToRGBA(S.grid), W, H), 0, 0);
}
function renderStatus() {
  const el = $('#status');
  if (!S.name) { el.textContent = ''; return; }
  el.textContent = isDirty() ? `${S.layer}/${S.name}.png · 저장 안 됨` : `${S.layer}/${S.name}.png · 저장됨`;
  el.classList.toggle('dirty', isDirty());
}

async function selectLayer(layer) {
  if (!(await confirmDiscard())) return;
  S.layer = layer; S.name = null; S.grid = blank(); S.saved = null; S.undo = []; S.redo = [];
  renderLayerTabs(); renderList(); renderKeys(); renderUsed(); renderStatus(); updateUndoButtons();
  const first = (S.sprites[layer] || [])[0];
  if (first) selectSprite(first); else drawPaint();
  // 기준색이 있는 레이어면 그 기본색을 바로 집어 준다
  const prefer = { body: 'SKIN', hair: 'HAIR', instrument: 'INSTRUMENT', outfit: 'OUTFIT' }[layer];
  pickColor(KEY_COLORS[prefer].color, false);
}
async function selectSprite(name) {
  if (name === S.name) return;
  if (!(await confirmDiscard())) return;
  S.name = name;
  delete S.others_cache[`${S.layer}/${name}`];
  S.grid = clone(await loadGrid(S.layer, name));
  S.saved = clone(S.grid); S.undo = []; S.redo = [];
  // 미리보기도 지금 편집하는 스프라이트로 맞춘다
  S.pv[S.layer] = name; syncPvSelects();
  renderList(); renderUsed(); renderStatus(); updateUndoButtons(); drawPaint(); drawPreview();
}
async function confirmDiscard() {
  if (!S.name || !isDirty()) return true;
  return window.confirm(`${S.layer}/${S.name}.png 에 저장하지 않은 변경이 있어요. 버릴까요?`);
}

async function save() {
  if (!S.name) return;
  try {
    const r = await api(`/api/sprite/${S.layer}/${S.name}`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ rgba: b64(new Uint8Array(gridToRGBA(S.grid).buffer)) }) });
    S.saved = clone(S.grid);
    renderStatus();
    toast(`저장했어요 → ${r.file}`, 'ok');
  } catch (e) { toast('저장 실패: ' + e.message, 'err', 4000); }
}
async function addSprite() {
  const name = $('#newName').value.trim().toUpperCase();
  if (!/^[A-Z0-9_]{1,40}$/.test(name)) { toast('이름은 영문 대문자·숫자·_ 만 돼요 (예: PONYTAIL)', 'err'); return; }
  if ((S.sprites[S.layer] || []).includes(name)) { toast('이미 있는 이름이에요', 'err'); return; }
  if (!(await confirmDiscard())) return;
  // 현재 그림을 복제해서 시작 (빈 캔버스보다 빠름)
  const start = S.name ? clone(S.grid) : blank();
  S.sprites[S.layer] = [...(S.sprites[S.layer] || []), name].sort();
  S.others_cache[`${S.layer}/${name}`] = start;
  S.name = null;
  await selectSprite(name);
  S.saved = null;   // 아직 파일 없음
  $('#newName').value = '';
  renderStatus();
  toast(`${name} 추가. 저장하면 파일이 만들어져요`);
}
async function removeSprite(name) {
  if (!window.confirm(`${S.layer}/${name}.png 파일을 지울까요? (git으로 되돌릴 수 있어요)`)) return;
  try {
    await api(`/api/sprite/${S.layer}/${name}`, { method: 'DELETE' });
    S.sprites[S.layer] = S.sprites[S.layer].filter((n) => n !== name);
    delete S.others_cache[`${S.layer}/${name}`];
    if (S.name === name) { S.name = null; S.grid = blank(); S.saved = null; }
    renderList(); renderStatus(); drawPaint();
    toast('지웠어요', 'ok');
  } catch (e) { toast('삭제 실패: ' + e.message, 'err'); }
}

// ---------- 미리보기 (실제 색으로 합성) ----------
const pv = $('#pvCanvas'), pvx = pv.getContext('2d');
pvx.imageSmoothingEnabled = false;
const off = document.createElement('canvas'); off.width = W; off.height = H;
const offx = off.getContext('2d');
let pvFrame = 0;
function swapGrid(g, table) {
  const out = gridToRGBA(g);
  for (let i = 0; i < out.length; i += 4) {
    if (!out[i + 3]) continue;
    const s = table.get(`${out[i]},${out[i + 1]},${out[i + 2]}`);
    if (s) { out[i] = s[0]; out[i + 1] = s[1]; out[i + 2] = s[2]; }
  }
  return out;
}
function drawPreview() {
  pvx.clearRect(0, 0, pv.width, pv.height);
  const table = buildSwapTable(S.pv);
  const scale = 7, bob = pvFrame % 2;
  for (const layer of LAYER_ORDER) {
    const name = S.pv[layer]; if (!name) continue;
    const g = layer === S.layer && name === S.name ? S.grid : S.others_cache[`${layer}/${name}`];
    if (!g) { loadGrid(layer, name).then(() => { drawPaint(); drawPreview(); }); continue; }
    offx.putImageData(new ImageData(swapGrid(g, table), W, H), 0, 0);
    const dy = layer === 'instrument' && ['GUITAR', 'BASS', 'VOCAL'].includes(name) ? (bob ? 0 : 1) : layer === 'instrument' ? 0 : bob;
    pvx.drawImage(off, (pv.width - W * scale) / 2, 12 + dy * scale, W * scale, H * scale);
  }
}
setInterval(() => { pvFrame++; drawPreview(); }, 500);

function syncPvSelects() {
  for (const layer of LAYER_ORDER) {
    const sel = $('#pv' + layer[0].toUpperCase() + layer.slice(1));
    sel.innerHTML = '';
    const names = [...(S.sprites[layer] || [])];
    if (S.name && layer === S.layer && !names.includes(S.name)) names.push(S.name);
    const none = document.createElement('option'); none.value = ''; none.textContent = '(없음)'; sel.appendChild(none);
    for (const n of names) { const o = document.createElement('option'); o.value = n; o.textContent = n; sel.appendChild(o); }
    sel.value = S.pv[layer] || '';
  }
}
for (const layer of LAYER_ORDER) {
  $('#pv' + layer[0].toUpperCase() + layer.slice(1)).addEventListener('change', (e) => { S.pv[layer] = e.target.value || null; drawPaint(); drawPreview(); });
}
for (const k of ['skinColor', 'hairColor', 'instrumentColor', 'outfitColor']) {
  $('#pv' + k[0].toUpperCase() + k.slice(1)).addEventListener('input', (e) => { S.pv[k] = e.target.value; drawPaint(); drawPreview(); });
}

// ---------- 입력 ----------
$$('.tool[data-tool]').forEach((b) => b.addEventListener('click', () => setTool(b.dataset.tool)));
$('#tMirror').onclick = () => toggle('mirror', '#tMirror');
$('#tOthers').onclick = () => toggle('others', '#tOthers');
$('#tGrid').onclick = () => toggle('showGrid', '#tGrid');
$('#tUndo').onclick = () => undo(false);
$('#tRedo').onclick = () => undo(true);
$('#tClear').onclick = () => { if (!S.name) return; pushUndo(); S.grid = blank(); changed(); };
$('#zIn').onclick = () => setZoom(Math.round(S.zoom * 1.25));
$('#zOut').onclick = () => setZoom(Math.round(S.zoom / 1.25));
$('#zFit').onclick = fitZoom;
$('#btnSave').onclick = save;
$('#btnNew').onclick = addSprite;
$('#newName').addEventListener('keydown', (e) => { if (e.key === 'Enter') addSprite(); });
$('#colorInput').addEventListener('input', (e) => pickColor(e.target.value));
$('#hexInput').addEventListener('change', (e) => { const v = e.target.value.trim(); if (/^#?[0-9a-fA-F]{6}$/.test(v)) pickColor(v.startsWith('#') ? v : '#' + v); });
$('#btnLighter').onclick = () => pickColor(lighter(S.color));
$('#btnDarker').onclick = () => pickColor(darker(S.color));

window.addEventListener('keydown', (e) => {
  if (e.target.matches('input, select, textarea')) return;
  const k = e.key.toLowerCase();
  if ((e.ctrlKey || e.metaKey) && k === 's') { e.preventDefault(); save(); return; }
  if ((e.ctrlKey || e.metaKey) && k === 'z') { e.preventDefault(); undo(e.shiftKey); return; }
  if ((e.ctrlKey || e.metaKey) && k === 'y') { e.preventDefault(); undo(true); return; }
  if (k === ' ') { spaceDown = true; e.preventDefault(); return; }
  if (k === 'b') setTool('pen'); else if (k === 'e') setTool('eraser'); else if (k === 'f') setTool('fill'); else if (k === 'i') setTool('picker');
  else if (k === 'm') toggle('mirror', '#tMirror'); else if (k === 'g') toggle('showGrid', '#tGrid');
  else if (k === 'tab') { e.preventDefault(); toggle('others', '#tOthers'); }
  else if (k === '0') fitZoom(); else if (k === 'w') pickColor(lighter(S.color)); else if (k === 's') pickColor(darker(S.color));
});
window.addEventListener('keyup', (e) => { if (e.key === ' ') spaceDown = false; });
window.addEventListener('resize', fitZoom);
window.addEventListener('beforeunload', (e) => { if (isDirty()) { e.preventDefault(); e.returnValue = ''; } });

// ---------- 시작 ----------
(async () => {
  S.sprites = await api('/api/sprites');
  renderSwatches($('#fixedColors'), FIXED_COLORS);
  renderSwatches($('#recentColors'), []);
  syncPvSelects();
  await selectLayer('hair');
  fitZoom();
})();
