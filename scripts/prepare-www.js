const fs = require('fs');
const path = require('path');

const rootDir = path.resolve(__dirname, '..');
const wwwDir = path.join(rootDir, 'www');

if (!fs.existsSync(wwwDir)) {
  fs.mkdirSync(wwwDir, { recursive: true });
}

const arquivos = ['index.html', 'manifest.json', 'sw.js', 'Morabus.png'];

for (const arq of arquivos) {
  const origem = path.join(rootDir, arq);
  const destino = path.join(wwwDir, arq);
  if (fs.existsSync(origem)) {
    fs.copyFileSync(origem, destino);
    console.log(`[build:www] Copiado: ${arq}`);
  }
}
console.log('[build:www] Pasta www/ pronta para sincronizar com o Android (Capacitor).');
