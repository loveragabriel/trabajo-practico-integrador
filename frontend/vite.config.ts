import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    // imports absolutos desde src: '@/shared/...' en vez de '../../../shared/...'
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    port: 5173,
    strictPort: true,
    // en desarrollo, las llamadas a /api se reenvían al backend Spring Boot
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
