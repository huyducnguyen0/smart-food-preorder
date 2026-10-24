import react from '@vitejs/plugin-react'
import { loadEnv } from 'vite'
import { defineConfig } from 'vitest/config'

// File .env dùng chung nằm ở 03-SourceCodes/, một cấp trên thư mục frontend/.
const envDir = '..'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, envDir, '')
  const backendPort = env.BACKEND_PORT || '8080'

  return {
    plugins: [react()],
    test: {
      environment: 'jsdom',
      setupFiles: './src/test/setup.ts',
    },
    envDir,
    server: {
      port: 5173,
      strictPort: true,
      // Trình duyệt gọi /api trên cùng origin với trang (5173); Vite chuyển tiếp sang backend.
      // Nhờ vậy khi phát triển không phát sinh request cross-origin, backend không cần cấu hình CORS.
      proxy: {
        '/api': `http://localhost:${backendPort}`,
      },
    },
  }
})
