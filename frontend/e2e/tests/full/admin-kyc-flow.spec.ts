import { expect, test } from '@playwright/test';

const gatewayUrl = process.env.E2E_GATEWAY_URL;

test.describe('Admin KYC full flow', () => {
  test.skip(!gatewayUrl, 'Set E2E_GATEWAY_URL to run against a live gateway');

  test('admin can open compliance review after login', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel(/email/i).fill('admin@tokenrealty.com');
    await page.getByLabel(/password/i).fill('admin123');
    await page.getByRole('button', { name: /sign in/i }).click();
    await page.getByRole('link', { name: /compliance|kyc/i }).click();
    await expect(page.getByRole('heading', { name: /kyc compliance/i })).toBeVisible();
  });
});
