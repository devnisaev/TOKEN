import { defineConfig, devices } from '@playwright/test';

const investorPort = 5173;
const adminPort = 5174;
const tenantPort = 5175;

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'investor-portal',
      testMatch: ['**/smoke/investor*.spec.ts', '**/full/investor*.spec.ts'],
      use: { ...devices['Desktop Chrome'], baseURL: `http://127.0.0.1:${investorPort}` },
    },
    {
      name: 'admin-dashboard',
      testMatch: '**/smoke/admin*.spec.ts',
      use: { ...devices['Desktop Chrome'], baseURL: `http://127.0.0.1:${adminPort}` },
    },
    {
      name: 'tenant-portal',
      testMatch: '**/smoke/tenant*.spec.ts',
      use: { ...devices['Desktop Chrome'], baseURL: `http://127.0.0.1:${tenantPort}` },
    },
  ],
  webServer: [
    {
      command: `npm run preview -- --port ${investorPort} --host 127.0.0.1`,
      cwd: '../investor-portal',
      port: investorPort,
      reuseExistingServer: !process.env.CI,
    },
    {
      command: `npm run preview -- --port ${adminPort} --host 127.0.0.1`,
      cwd: '../admin-dashboard',
      port: adminPort,
      reuseExistingServer: !process.env.CI,
    },
    {
      command: `npm run preview -- --port ${tenantPort} --host 127.0.0.1`,
      cwd: '../tenant-portal',
      port: tenantPort,
      reuseExistingServer: !process.env.CI,
    },
  ],
});
