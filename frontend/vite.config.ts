import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  plugins: [
    react(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['icon.svg'],
      manifest: {
        name: '東京終電検索 / Tokyo Last Train Finder',
        short_name: '終電検索',
        description: '東京の地下鉄・私鉄の終電経路を検索',
        theme_color: '#e94560',
        background_color: '#f5f6f8',
        display: 'standalone',
        orientation: 'portrait',
        lang: 'ja',
        start_url: '/',
        scope: '/',
        icons: [
          { src: '/icon.svg', sizes: 'any', type: 'image/svg+xml', purpose: 'any maskable' },
        ],
      },
      workbox: {
        // /api/v1/last-train, /api/v1/stations/search 등은 NetworkFirst (실시간성 우선, 오프라인 fallback)
        runtimeCaching: [
          {
            urlPattern: /\/api\/v1\/(last-train|stations\/search)/,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'tlt-api-cache',
              networkTimeoutSeconds: 5,
              expiration: { maxEntries: 50, maxAgeSeconds: 60 * 60 * 24 },
              cacheableResponse: { statuses: [0, 200] },
            },
          },
          {
            urlPattern: /^https:\/\/fonts\.(googleapis|gstatic)\.com\//,
            handler: 'CacheFirst',
            options: {
              cacheName: 'tlt-fonts-cache',
              expiration: { maxEntries: 10, maxAgeSeconds: 60 * 60 * 24 * 365 },
            },
          },
        ],
      },
    }),
  ],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
    },
  },
})
