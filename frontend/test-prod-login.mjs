import http from 'http';
import fs from 'fs';
import path from 'path';
import { chromium } from 'playwright';

// Start server
const dist = '/home/with/Projects/notify/frontend/dist/frontend/browser';
const mime = {'.html':'text/html','.js':'text/javascript','.css':'text/css','.png':'image/png','.svg':'image/svg+xml','.ico':'image/x-icon','.woff2':'font/woff2'};

const server = http.createServer((req, res) => {
  let url = req.url.split('?')[0].split('#')[0];
  const serve = (p) => {
    try {
      const data = fs.readFileSync(p);
      res.writeHead(200, {'Content-Type': mime[path.extname(p)] || 'text/plain', 'Cache-Control': 'no-cache', 'Access-Control-Allow-Origin': '*'});
      res.end(data);
    } catch(e) {
      try {
        const idx = fs.readFileSync(path.join(dist, 'index.html'));
        res.writeHead(200, {'Content-Type': 'text/html'});
        res.end(idx);
      } catch(e2) { res.writeHead(404); res.end('NF'); }
    }
  };
  serve(url === '/' ? path.join(dist, 'index.html') : path.join(dist, url));
});

server.listen(4301, async () => {
  console.log('Server started on 4301');
  
  await new Promise(r => setTimeout(r, 1000));
  
  try {
    const chromePath = path.join(process.env.HOME, '.cache/ms-playwright/chromium-1228/chrome-linux64/chrome');
    const browser = await chromium.launch({ 
      executablePath: chromePath,
      headless: true,
      args: ['--no-sandbox', '--disable-setuid-sandbox']
    });
    const page = await browser.newPage({ viewport: { width: 1280, height: 800 } });

    const errors = [];
    page.on('console', msg => { if (msg.type() === 'error') errors.push('[CONSOLE] ' + msg.text()); });
    page.on('pageerror', err => errors.push('[PAGE] ' + err.message));

    await page.goto('http://localhost:4301/auth/login', { waitUntil: 'load', timeout: 15000 });
    await new Promise(r => setTimeout(r, 3000));

    await page.screenshot({ path: '/tmp/prod-before.png', fullPage: true });
    console.log('Title:', await page.title());
    console.log('URL:', page.url());

    const inputInfo = await page.evaluate(() => {
      const inputs = document.querySelectorAll('input');
      return Array.from(inputs).map(inp => ({
        type: inp.type,
        tuiInput: inp.hasAttribute('tuiInput'),
        opacity: window.getComputedStyle(inp).opacity,
        pointerEvents: window.getComputedStyle(inp).pointerEvents,
        position: window.getComputedStyle(inp).position,
        color: window.getComputedStyle(inp).color,
        bgColor: window.getComputedStyle(inp).backgroundColor,
        width: Math.round(inp.getBoundingClientRect().width),
        height: Math.round(inp.getBoundingClientRect().height)
      }));
    });
    console.log('Inputs:', JSON.stringify(inputInfo));

    const emailInput = page.locator('input[type="email"]');
    console.log('Email count:', await emailInput.count());

    if (await emailInput.count() > 0) {
      await emailInput.click({ force: true });
      await new Promise(r => setTimeout(r, 200));
      await page.keyboard.type('user@test.com', { delay: 20 });
      await new Promise(r => setTimeout(r, 300));
      console.log('Email value:', await page.evaluate(() => document.querySelector('input[type="email"]')?.value));
    }

    const pwInput = page.locator('input[type="password"]');
    console.log('Password count:', await pwInput.count());

    if (await pwInput.count() > 0) {
      await pwInput.click({ force: true });
      await new Promise(r => setTimeout(r, 200));
      await page.keyboard.type('MyPass123!', { delay: 20 });
      await new Promise(r => setTimeout(r, 300));
      console.log('Password value:', await page.evaluate(() => document.querySelector('input[type="password"]')?.value));
    }

    console.log('Errors:', JSON.stringify(errors));
    await page.screenshot({ path: '/tmp/prod-after.png', fullPage: true });
    await browser.close();
    console.log('TEST COMPLETE');
  } catch(e) {
    console.error('TEST ERROR:', e.message);
    console.error(e.stack);
  }
  
  server.close();
});
