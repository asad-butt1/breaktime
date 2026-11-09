// Builds Play Store images from the rendered app screenshots.
// Usage: node store/make_store_images.js <screenshots-dir> <out-dir>
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const [, , shotsDir, outDir] = process.argv;
fs.mkdirSync(outDir, { recursive: true });
const font = 'file://' + path.resolve(__dirname, '../app/src/main/res/font/plus_jakarta_sans.ttf');

const C = { parch: '#FAFBF6', pine: '#173D34', forest: '#245D4C', sage: '#E2EFE4', sky: '#DCEEF0', sun: '#F9DFA0' };

const shots = [
  ['store_1_home', 'Take a real break<br>from your phone', 'Pick a length, tap start, put the phone down.', C.sage],
  ['store_2_locked', 'Distracting apps<br>stay paused', 'Opening one shows a calm reminder instead.', C.sky],
  ['store_3_active', 'Go outside,<br>breathe, reset', 'Simple ideas for your time away.', '#F6EED2'],
  ['store_4_apps', 'Choose the apps<br>that pull you in', 'Social feeds, short videos, AI chat and more.', C.sage],
  ['store_5_progress', 'Watch your time<br>offline add up', 'Breaks, hours offline and history.', C.sky],
  ['store_6_complete', 'Build a daily<br>streak', 'Small breaks, every day.', '#F6EED2'],
];

const base = `
@font-face { font-family: J; src: url('${font}'); font-weight: 100 900; }
* { box-sizing: border-box; margin: 0; }
body { width: 1080px; height: 1920px; font-family: J, sans-serif; color: ${C.pine}; overflow: hidden; }`;

function screenshotPage([file, title, sub, bg]) {
  const img = 'file://' + path.resolve(shotsDir, file + '.png');
  return `<html><head><style>${base}
  body { background: ${bg}; display: flex; flex-direction: column; align-items: center; }
  h1 { font-size: 84px; line-height: 1.08; font-weight: 800; letter-spacing: -0.03em; text-align: center; margin-top: 120px; }
  p { font-size: 36px; color: #4F625A; margin-top: 26px; text-align: center; font-weight: 500; }
  .phone { margin-top: 70px; width: 760px; border-radius: 64px; background: ${C.pine}; padding: 18px;
           box-shadow: 0 40px 80px rgba(23,61,52,0.22); }
  .phone img { width: 100%; display: block; border-radius: 48px; }
  </style></head><body><h1>${title}</h1><p>${sub}</p><div class="phone"><img src="${img}"></div></body></html>`;
}

const sunHills = (w, h) => `
<svg width="${w}" height="${h}" viewBox="0 0 ${w} ${h}" style="position:absolute;inset:0">
 <defs><linearGradient id="sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#D5ECEE"/><stop offset="1" stop-color="#F7EFCF"/></linearGradient></defs>
 <rect width="${w}" height="${h}" fill="url(#sky)"/>
 <circle cx="${w * 0.78}" cy="${h * 0.42}" r="${h * 0.3}" fill="#FBEFC6" opacity="0.7"/>
 <circle cx="${w * 0.78}" cy="${h * 0.42}" r="${h * 0.2}" fill="${C.sun}"/>
 <path d="M0,${h * 0.62} C${w * 0.3},${h * 0.5} ${w * 0.6},${h * 0.72} ${w},${h * 0.56} V${h} H0Z" fill="#C4DFC9"/>
 <path d="M0,${h * 0.74} C${w * 0.35},${h * 0.62} ${w * 0.65},${h * 0.84} ${w},${h * 0.68} V${h} H0Z" fill="#A2CBAF"/>
 <path d="M0,${h * 0.86} C${w * 0.3},${h * 0.76} ${w * 0.7},${h * 0.94} ${w},${h * 0.8} V${h} H0Z" fill="#79AE8E"/>
</svg>`;

const iconSvg = (s) => `
<svg width="${s}" height="${s}" viewBox="0 0 108 108">
 <rect width="108" height="108" fill="${C.forest}"/>
 <circle cx="54" cy="58" r="29" fill="${C.sun}" fill-opacity="0.18"/>
 <circle cx="54" cy="58" r="22" fill="${C.sun}"/>
 <rect x="46" y="43" width="5" height="17" rx="2.5" fill="${C.pine}"/>
 <rect x="57" y="43" width="5" height="17" rx="2.5" fill="${C.pine}"/>
 <path d="M0,68C18,63 36,70 54,67C72,64 90,61 108,65V108H0z" fill="${C.sage}"/>
 <path d="M0,79C22,73 44,82 66,77C80,74 94,74 108,76V108H0z" fill="#9CC3AA"/>
</svg>`;

const feature = `<html><head><style>${base}
 body { width: 1024px; height: 500px; position: relative; }
 .t { position: absolute; left: 64px; top: 0; bottom: 0; display: flex; flex-direction: column; justify-content: center; }
 .row { display: flex; align-items: center; gap: 18px; }
 .icon { width: 84px; height: 84px; border-radius: 22px; overflow: hidden; }
 h1 { font-size: 64px; font-weight: 800; letter-spacing: -0.03em; }
 p { font-size: 30px; font-weight: 600; margin-top: 18px; max-width: 520px; line-height: 1.25; }
</style></head><body>${sunHills(1024, 500)}
<div class="t"><div class="row"><div class="icon">${iconSvg(84)}</div><h1>BreakTime</h1></div>
<p>Lock distracting apps.<br>Take a real break.</p></div></body></html>`;

const icon512 = `<html><head><style>body{margin:0;width:512px;height:512px}</style></head><body>${iconSvg(512)}</body></html>`;

async function show(page, html) {
  const f = path.join(outDir, '_tmp.html');
  fs.writeFileSync(f, html);
  await page.goto('file://' + path.resolve(f), { waitUntil: 'load' });
  await page.evaluate(() => document.fonts.ready);
}

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1080, height: 1920 } });
  for (const [i, s] of shots.entries()) {
    await show(page, screenshotPage(s));
    await page.screenshot({ path: path.join(outDir, `phone-${i + 1}.png`) });
  }
  await page.setViewportSize({ width: 1024, height: 500 });
  await show(page, feature);
  await page.screenshot({ path: path.join(outDir, 'feature-graphic-1024x500.png') });
  await page.setViewportSize({ width: 512, height: 512 });
  await show(page, icon512);
  await page.screenshot({ path: path.join(outDir, 'icon-512.png') });
  fs.unlinkSync(path.join(outDir, '_tmp.html'));
  await browser.close();
})();
