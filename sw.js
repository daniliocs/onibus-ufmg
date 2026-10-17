/* =================================================================
   MORABUS - Service Worker
   ⚠️ Sempre que alterar o index.html ou este arquivo, aumente a versão
   ================================================================= */
const VERSAO = 'v29';
const CACHE_APP = `onibus-ufmg-app-${VERSAO}`;
const CACHE_MAPA = 'onibus-ufmg-tiles'; // tiles do mapa (mantido entre versões)
const LIMITE_TILES = 300;               // evita encher o armazenamento do celular
const TAG_LIVE = 'morabus-live-activity';

const ASSETS = [
  './',
  './index.html',
  './manifest.json',
  './Morabus.png',
  'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css',
  'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'
];

// Serviços em tempo real: NUNCA passam pelo cache
const HOSTS_TEMPO_REAL = [
  'firebaseio.com',
  'firebasedatabase.app',
  'open-meteo.com',
  'router.project-osrm.org',
  'routing.openstreetmap.de'
];

/* 1. Instalação: guarda os arquivos essenciais */
self.addEventListener('install', (e) => {
  e.waitUntil(
    caches.open(CACHE_APP)
      .then((cache) => cache.addAll(ASSETS))
      .then(() => self.skipWaiting())
  );
});

/* 2. Ativação: apaga caches de versões antigas (preserva os tiles) */
self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys()
      .then((chaves) => Promise.all(
        chaves
          .filter((c) => c !== CACHE_APP && c !== CACHE_MAPA)
          .map((c) => caches.delete(c))
      ))
      .then(() => self.clients.claim())
  );
});

async function limitarCache(nome, maximo) {
  const cache = await caches.open(nome);
  const chaves = await cache.keys();
  if (chaves.length > maximo) {
    await Promise.all(chaves.slice(0, chaves.length - maximo).map((k) => cache.delete(k)));
  }
}

/* 3. Requisições */
self.addEventListener('fetch', (e) => {
  const req = e.request;
  if (req.method !== 'GET') return;

  const url = new URL(req.url);
  if (!url.protocol.startsWith('http')) return;
  if (HOSTS_TEMPO_REAL.some((h) => url.hostname.includes(h))) return;

  // 3a. HTML: rede primeiro (sempre a versão mais nova), cache se estiver offline
  if (req.mode === 'navigate') {
    e.respondWith(
      fetch(req)
        .then((resp) => {
          if (resp && resp.ok) {
            const copia = resp.clone();
            e.waitUntil(caches.open(CACHE_APP).then((c) => c.put('./index.html', copia)));
          }
          return resp;
        })
        .catch(async () =>
          (await caches.match('./index.html')) ||
          (await caches.match('./')) ||
          new Response('<h1>Sem conexão</h1><p>Abra o app novamente quando estiver online.</p>',
            { headers: { 'Content-Type': 'text/html; charset=utf-8' } })
        )
    );
    return;
  }

  // 3b. Tiles do mapa: cache primeiro, com limite de quantidade
  if (url.hostname.includes('tile.openstreetmap.org')) {
    e.respondWith(
      caches.open(CACHE_MAPA).then(async (cache) => {
        const emCache = await cache.match(req);
        if (emCache) return emCache;
        const resp = await fetch(req);
        if (resp && (resp.ok || resp.type === 'opaque')) {
          e.waitUntil(cache.put(req, resp.clone()).then(() => limitarCache(CACHE_MAPA, LIMITE_TILES)));
        }
        return resp;
      })
    );
    return;
  }

  // 3c. Demais arquivos: stale-while-revalidate (responde do cache e atualiza em segundo plano)
  e.respondWith(
    caches.open(CACHE_APP).then(async (cache) => {
      const emCache = await cache.match(req);
      const daRede = fetch(req)
        .then((resp) => {
          if (resp && resp.ok) cache.put(req, resp.clone());
          return resp;
        })
        .catch(() => emCache);
      if (emCache) {
        e.waitUntil(daRede);
        return emCache;
      }
      return daRede;
    })
  );
});

/* 4. Mensagens vindas da página (mantido por compatibilidade) */
self.addEventListener('message', (event) => {
  const d = event.data;
  if (!d) return;

  if (d.type === 'SHOW_NOTIFICATION') {
    event.waitUntil(self.registration.showNotification(d.titulo || 'Ônibus Moradias UFMG', {
      body: d.mensagem,
      icon: './Morabus.png',
      badge: './Morabus.png',
      vibrate: [200, 100, 200],
      tag: d.tag || 'onibus-alerta',
      renotify: true
    }));
  }

  if (d.type === 'UPDATE_LIVE_NOTIFICATION') {
    event.waitUntil(self.registration.showNotification(d.titulo, {
      body: d.corpo,
      icon: './Morabus.png',
      badge: './Morabus.png',
      tag: TAG_LIVE,
      renotify: false,
      silent: true,
      requireInteraction: true,
      actions: [
        { action: 'abrir', title: '🗺️ Ver Mapa' },
        { action: 'parar_live', title: '✖️ Fechar barra' }
      ]
    }));
  }

  if (d.type === 'CLOSE_LIVE_NOTIFICATION') {
    event.waitUntil(
      self.registration.getNotifications({ tag: TAG_LIVE })
        .then((lista) => lista.forEach((n) => n.close()))
    );
  }

  if (d.type === 'SKIP_WAITING') self.skipWaiting();
});

/* 5. Toque na notificação ou nos botões */
self.addEventListener('notificationclick', (event) => {
  event.notification.close();

  event.waitUntil((async () => {
    const janelas = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });

    // "Fechar barra": avisa a página para não recriar a notificação desta viagem
    if (event.action === 'parar_live') {
      janelas.forEach((c) => c.postMessage({ type: 'LIVE_DISMISSED' }));
      return;
    }

    for (const c of janelas) {
      if ('focus' in c) return c.focus();
    }
    if (self.clients.openWindow) return self.clients.openWindow('./');
  })());
});

/* 6. Fechar a barra ao vivo arrastando também conta como "dispensar" */
self.addEventListener('notificationclose', (event) => {
  if (event.notification.tag !== TAG_LIVE) return;
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true })
      .then((janelas) => janelas.forEach((c) => c.postMessage({ type: 'LIVE_DISMISSED' })))
  );
});
