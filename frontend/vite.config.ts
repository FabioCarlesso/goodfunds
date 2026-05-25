/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // strictPort: falha em vez de pular para outra porta se a 5173 estiver ocupada.
    // Mantem a origem do dev server deterministica e alinhada ao CORS do backend
    // (app.cors.allowed-origins / APP_CORS_ALLOWED_ORIGINS).
    port: 5173,
    strictPort: true,
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    css: true,
  },
})
