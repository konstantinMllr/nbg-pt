import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react' // oder dein entsprechendes Plugin

export default defineConfig({
  plugins: [react()],
  server: {
    allowedHosts: [
      'nbg-pt.de',
      'www.nbg-pt.de'
    ],
    // Falls du es noch nicht hast, hilft das hier auch:
    host: true, 
    strictPort: true,
  }
})