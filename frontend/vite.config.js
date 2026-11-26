import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const proxy = {
  '/api': { target: process.env.BUS_API_TARGET || 'http://localhost:8080' },
};

export default defineConfig({
  plugins: [react()],
  server: { proxy },
  preview: { proxy },
});
