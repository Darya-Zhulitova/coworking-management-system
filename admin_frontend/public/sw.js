// const OFFLINE_URL = '/offline.html';
// self.addEventListener('install', (event) => {
//   event.waitUntil(caches.open('offline-fallback-v1').then((cache) => cache.add(OFFLINE_URL)));
//   self.skipWaiting();
// });
// self.addEventListener('activate', (event) => {
//   event.waitUntil(self.clients.claim());
// });
// self.addEventListener('fetch', (event) => {
//   if (event.request.method !== 'GET' || event.request.mode !== 'navigate') return;
//   event.respondWith(fetch(event.request).catch(async () => {
//     const cache = await caches.open('offline-fallback-v1');
//     return (await cache.match(OFFLINE_URL)) || Response.error();
//   }));
// });
