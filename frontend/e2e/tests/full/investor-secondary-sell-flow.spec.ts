import { expect, test } from '@playwright/test';

const gatewayUrl = process.env.E2E_GATEWAY_URL;

test.describe('Investor secondary sell full flow', () => {
  test.skip(!gatewayUrl, 'Set E2E_GATEWAY_URL to run against a live gateway');

  test('investor can open sell form from portfolio holdings', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel(/email/i).fill('investor@tokenrealty.com');
    await page.getByLabel(/password/i).fill('investor123');
    await page.getByRole('button', { name: /sign in/i }).click();

    await page.getByRole('link', { name: /portfolio/i }).click();
    await expect(page.getByRole('heading', { name: /portfolio/i })).toBeVisible();

    const sellLink = page.getByRole('link', { name: /sell/i }).first();
    if ((await sellLink.count()) === 0) {
      test.skip(true, 'No token holdings — buy tokens first');
    }

    await sellLink.click();
    await expect(page.getByRole('heading', { name: /sell tokens/i })).toBeVisible();
    await expect(page.getByLabel(/token amount/i)).toBeVisible();
  });
});
