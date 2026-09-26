import { expect, test } from '@playwright/test';

const gatewayUrl = process.env.E2E_GATEWAY_URL;

test.describe('Tenant rent full flow', () => {
  test.skip(!gatewayUrl, 'Set E2E_GATEWAY_URL to run against a live gateway');

  test('tenant can sign in and view lease page', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel(/email/i).fill('tenant@tokenrealty.com');
    await page.getByLabel(/password/i).fill('tenant123');
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page.getByRole('heading', { name: /lease|rent/i })).toBeVisible();
  });
});
