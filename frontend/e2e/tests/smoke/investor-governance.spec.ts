import { test, expect } from '@playwright/test';

test('governance route requires login', async ({ page }) => {
  await page.goto('/governance');
  await expect(page).toHaveURL(/\/login/);
});

test('governance page shows heading when logged in', async ({ page }) => {
  const gatewayUrl = process.env.E2E_GATEWAY_URL;
  test.skip(!gatewayUrl, 'Set E2E_GATEWAY_URL for full smoke');

  await page.goto('/login');
  await page.getByLabel(/email/i).fill(process.env.E2E_INVESTOR_EMAIL ?? 'investor@demo.local');
  await page.getByLabel(/password/i).fill(process.env.E2E_INVESTOR_PASSWORD ?? 'demo1234');
  await page.getByRole('button', { name: /sign in/i }).click();

  await page.getByRole('link', { name: /governance/i }).click();
  await expect(page.getByRole('heading', { name: /governance/i })).toBeVisible();
});
