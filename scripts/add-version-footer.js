const fs = require('fs');
const path = require('path');

// 1. Atualizar index.html
const indexPath = path.join(__dirname, '..', 'index.html');
let html = fs.readFileSync(indexPath, 'utf8');

function normalizeAndReplace(src, target, repl) {
  const normSrc = src.replace(/\r\n/g, '\n');
  const normTarget = target.replace(/\r\n/g, '\n');
  const normRepl = repl.replace(/\r\n/g, '\n');
  if (!normSrc.includes(normTarget)) {
    throw new Error('Target not found:\n' + normTarget.slice(0, 100));
  }
  return normSrc.replace(normTarget, normRepl).replace(/\n/g, '\r\n');
}

const targetCss = `.rodape-compartilhar { text-align: center; padding: 20px 10px 30px 10px; font-size: 13px; color: #94a3b8; }
    .rodape-compartilhar a { color: var(--primary); font-weight: 600; text-decoration: none; }`;

const replCss = `.rodape-compartilhar { text-align: center; padding: 20px 10px 40px 10px; font-size: 13px; color: var(--text-muted); }
    .rodape-compartilhar a { color: var(--primary); font-weight: 600; text-decoration: none; }
    .rodape-versao {
      margin-top: 12px;
      font-size: 12px;
      color: var(--text-muted);
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
    }
    .badge-versao {
      display: inline-flex;
      align-items: center;
      background: var(--primary-light);
      color: var(--primary);
      border: 1px solid var(--border);
      padding: 2px 8px;
      border-radius: var(--radius-pill);
      font-weight: 700;
      font-size: 11px;
      letter-spacing: 0.5px;
    }`;

html = normalizeAndReplace(html, targetCss, replCss);

const targetDark = `.txt-ok, .destaque-espera.verde, .link-acao, .rodape-compartilhar a { color: #4ade80; }`;
const replDark = `.txt-ok, .destaque-espera.verde, .link-acao, .rodape-compartilhar a { color: #4ade80; }
      .badge-versao { background: rgba(74, 222, 128, 0.15); color: #4ade80; border-color: rgba(74, 222, 128, 0.3); }`;

html = normalizeAndReplace(html, targetDark, replDark);

const targetHtml = `<div class="rodape-compartilhar">
    Útil para o seu dia a dia? <a href="#" onclick="compartilharApp(); return false;">Envie para seus amigos da Moradia!</a>
  </div>`;

const replHtml = `<div class="rodape-compartilhar">
    Útil para o seu dia a dia? <a href="#" onclick="compartilharApp(); return false;">Envie para seus amigos da Moradia!</a>
    <div class="rodape-versao">
      <span class="badge-versao">v1.5</span> • Morabus UFMG • Outubro/2026
    </div>
  </div>`;

html = normalizeAndReplace(html, targetHtml, replHtml);
fs.writeFileSync(indexPath, html, 'utf8');
console.log('index.html updated with version v1.5 footer!');

// 2. Atualizar sw.js
const swPath = path.join(__dirname, '..', 'sw.js');
let sw = fs.readFileSync(swPath, 'utf8');
sw = sw.replace(/const VERSAO = '[^']+';/, "const VERSAO = 'v1.5';");
fs.writeFileSync(swPath, sw, 'utf8');
console.log('sw.js updated to version v1.5!');
