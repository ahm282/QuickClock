import { defineConfig } from 'vite';

export default defineConfig({
    optimizeDeps: {
        exclude: ['@fontsource-variable/geist'],
    },
});
