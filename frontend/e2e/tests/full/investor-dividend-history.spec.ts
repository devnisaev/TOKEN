import { expect, test } from '@playwright/test';

const gatewayUrl = process.env.E2E_GATEWAY_URL;

test.describe('Investor dividend history full flow', () => {
  test.skip(!gatewayUrl, 'Set E2E_GATEWAY_URL to run against a live gateway');

  test('investor can open dividend history page', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel(/email/i).fill('investor@tokenrealty.com');
    await page.getByLabel(/password/i).fill('investor123');
    await page.getByRole('button', { name: /sign in/i }).click();

    await page.getByRole('link', { name: /dividends/i }).click();
    await expect(page.getByRole('heading', { name: /dividends/i })).toBeVisible();
  });
});
