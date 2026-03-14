const CACHE_NAME = 'onibus-ufmg-v2';
const ASSETS = [
  './',
  './index.html',
  './manifest.json',
  // O Leaflet precisa de internet, então deixamos de fora para o app não travar offline
];

// Instala o Service Worker e salva os arquivos
self.addEventListener('install', e => {
  e.waitUntil(
    caches.open(CACHE_NAME).then(cache => cache.addAll(ASSETS))
  );
});

// Intercepta as requisições para funcionar sem internet
self.addEventListener('fetch', e => {
  e.respondWith(
    caches.match(e.request).then(response => response || fetch(e.request))
  );
});
