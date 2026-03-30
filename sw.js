const CACHE_NAME = 'onibus-ufmg-v18'; // Atualizado para v18
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

// --- NOVA PARTE: NOTIFICAÇÕES ---

// Intercepta a mensagem vinda do index.html para mostrar a notificação
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SHOW_NOTIFICATION') {
    const titulo = event.data.titulo || 'Ônibus Moradias UFMG';
    const opcoes = {
      body: event.data.mensagem,
      icon: './icone.png', 
      badge: './icone.png', 
      vibrate: [200, 100, 200], 
      tag: 'onibus-alerta', // Evita spam se disparar várias vezes
    };

    // Pede ao sistema operacional para mostrar a notificação
    self.registration.showNotification(titulo, opcoes);
  }
});

// Limpa a notificação da tela se o usuário clicar nela
self.addEventListener('notificationclick', (event) => {
  event.notification.close(); 
});
