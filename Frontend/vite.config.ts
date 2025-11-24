import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: { port: 5173, open: true, allowedHosts: ['localhost', '127.0.0.1', '0.0.0.0', 'arlean-stratagemical-kimberlie.ngrok-free.dev'] }
});


