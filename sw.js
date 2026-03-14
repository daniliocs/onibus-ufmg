const CACHE_NAME = 'onibus-ufmg-v3'; // Mude para v3 na próxima vez que alterar o site
const ASSETS = [
  './',
  './index.html',
  './manifest.json',
];

// Instala e força a atualização imediata
self.addEventListener('install', e => {
  self.skipWaiting(); // Expulsa a versão velha na hora!
  e.waitUntil(
    caches.open(CACHE_NAME).then(cache => cache.addAll(ASSETS))
  );
});

// Ativa e faz a faxina do cache antigo
self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys().then(chaves => {
      return Promise.all(
        chaves.map(chave => {
          if (chave !== CACHE_NAME) {
            return caches.delete(chave); // Apaga os arquivos da versão anterior
          }
        })
      );
    })
  );
  return self.clients.claim(); // Assume o controle da tela imediatamente
});

// Intercepta as requisições
self.addEventListener('fetch', e => {
  e.respondWith(
    caches.match(e.request).then(response => response || fetch(e.request))
  );
});
