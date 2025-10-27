import { defineConfig } from 'cypress';

export default defineConfig({

  e2e: {
    baseUrl: 'http://localhost:4200',
    specPattern: 'cypress/integration/**/*.spec.{js,jsx,ts,tsx}',
    supportFile: 'cypress/support/e2e.ts',
    viewportWidth: 1280,
    viewportHeight: 720,
    setupNodeEvents(on, config) {
      return config;
    },
  },
});
