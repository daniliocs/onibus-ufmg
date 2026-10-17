const CACHE_NAME = 'onibus-ufmg-v28';
const ASSETS = [
  './',
  './index.html',
  './manifest.json',
  './Morabus.png',
  'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css',
  'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'
];

// 1. Instalação: salva os arquivos essenciais e ativa imediatamente
self.addEventListener('install', (e) => {
  self.skipWaiting();
  e.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      return cache.addAll(ASSETS);
    })
  );
});

// 2. Ativação: limpa caches de versões antigas
self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys().then((chaves) => {
      return Promise.all(
        chaves.map((chave) => {
          if (chave !== CACHE_NAME) {
            return caches.delete(chave);
          }
        })
      );
    })
  );
  return self.clients.claim();
});

// 3. Interceptação de requisições: Estratégia Network-First (Rede primeiro, Cache como fallback offline)
self.addEventListener('fetch', (e) => {
  // Ignora requisições que não sejam GET ou que sejam de APIs em tempo real (Rotas, Firebase e Clima)
  if (
    e.request.method !== 'GET' ||
    e.request.url.includes('router.project-osrm.org') ||
    e.request.url.includes('firebaseio.com') ||
    e.request.url.includes('open-meteo.com')
  ) {
    return;
  }
  
  // Quando o usuário abre o site (HTML): sempre busca a versão mais recente na rede primeiro
  if (e.request.mode === 'navigate') {
    e.respondWith(
      fetch(e.request)
        .then((networkResponse) => {
          if (networkResponse && networkResponse.status === 200) {
            const clone = networkResponse.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(e.request, clone));
          }
          return networkResponse;
        })
        .catch(() => caches.match('./index.html') || caches.match(e.request))
    );
    return;
  }

  // Demais arquivos (imagens, estilos, scripts externos): usa cache rápido com atualização em segundo plano
  e.respondWith(
    caches.match(e.request).then((cachedResponse) => {
      return (
        cachedResponse ||
        fetch(e.request).then((networkResponse) => {
          if (networkResponse && networkResponse.status === 200) {
            const clone = networkResponse.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(e.request, clone));
          }
          return networkResponse;
        })
      );
    })
  );
});
        

// 4. Recebe comando do index.html para disparar Notificação Normal ou Notificação Ao Vivo (One UI)
self.addEventListener('message', (event) => {
  if (!event.data) return;

  // CASO A: Notificação de Alerta Padrão (Vibra e faz som)
  if (event.data.type === 'SHOW_NOTIFICATION') {
    const titulo = event.data.titulo || 'Ônibus Moradias UFMG';
    const opcoes = {
      body: event.data.mensagem,
      icon: './Morabus.png',
      badge: './Morabus.png',
      vibrate: [200, 100, 200],
      tag: event.data.tag || 'onibus-alerta',
      renotify: true
    };
    self.registration.showNotification(titulo, opcoes);
  }

  // CASO B: Notificação Ao Vivo com Barra de Progresso (Atualiza em silêncio na tela de bloqueio / One UI)
  if (event.data.type === 'UPDATE_LIVE_NOTIFICATION') {
    const opcoesLive = {
      body: event.data.corpo,
      icon: './Morabus.png',
      badge: './Morabus.png',
      tag: 'morabus-live-activity',
      renotify: false,
      silent: true, // Atualiza o progresso a cada 10s sem tocar som nem vibrar
      requireInteraction: true, // Mantém fixada na tela de bloqueio igual Uber/Maps
      actions: [
        { action: 'abrir', title: '🗺️ Ver Mapa' },
        { action: 'parar_live', title: '✖️ Fechar barra' }
      ]
    };
    self.registration.showNotification(event.data.titulo, opcoesLive);
  }

  // CASO C: Encerrar a Notificação Ao Vivo quando o ônibus chega
  if (event.data.type === 'CLOSE_LIVE_NOTIFICATION') {
    self.registration.getNotifications({ tag: 'morabus-live-activity' }).then((notifs) => {
      notifs.forEach((n) => n.close());
    });
  }
});

// 5. Ao tocar na notificação ou nos botões de ação
self.addEventListener('notificationclick', (event) => {
  if (event.action === 'parar_live') {
    event.notification.close();
    return;
  }

  event.notification.close();

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if ('focus' in client) {
          return client.focus();
        }
      }
      if (clients.openWindow) {
        return clients.openWindow('./');
      }
    })
  );
});

