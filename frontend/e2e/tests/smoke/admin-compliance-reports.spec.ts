import { test, expect } from '@playwright/test';

test('compliance reports route requires login', async ({ page }) => {
  await page.goto('/reports/compliance');
  await expect(page).toHaveURL(/\/login/);
});

test('compliance reports page shows heading when logged in', async ({ page }) => {
  const gatewayUrl = process.env.E2E_GATEWAY_URL;
  test.skip(!gatewayUrl, 'Set E2E_GATEWAY_URL for full smoke');

  await page.goto('/login');
  await page.getByLabel(/email/i).fill(process.env.E2E_ADMIN_EMAIL ?? 'admin@demo.local');
  await page.getByLabel(/password/i).fill(process.env.E2E_ADMIN_PASSWORD ?? 'demo1234');
  await page.getByRole('button', { name: /sign in/i }).click();

  await page.goto('/reports/compliance');
  await expect(page.getByRole('heading', { name: /compliance reports/i })).toBeVisible();
});
