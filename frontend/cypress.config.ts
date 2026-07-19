import { defineConfig } from 'cypress';

export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:4200',
    supportFile: 'cypress/support/e2e.ts',
    specPattern: 'cypress/e2e/**/*.cy.ts',
    video: false,
    screenshotOnRunFailure: true,
    defaultCommandTimeout: 10000,
    requestTimeout: 15000,
    viewportWidth: 1280,
    viewportHeight: 800,
    env: {
      adminUsername: 'admin',
      adminPassword: '12345678',
      staffUsername: 'staff',
      staffPassword: '12345678',
      apiBaseUrl: 'http://localhost:8080/api/v1',
    },
  },
});
