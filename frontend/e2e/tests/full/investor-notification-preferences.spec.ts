import { expect, test } from '@playwright/test';

const gatewayUrl = process.env.E2E_GATEWAY_URL;

test.describe('Investor notification preferences full flow', () => {
  test.skip(!gatewayUrl, 'Set E2E_GATEWAY_URL to run against a live gateway');

  test('investor can view and save notification preferences', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel(/email/i).fill('investor@tokenrealty.com');
    await page.getByLabel(/password/i).fill('investor123');
    await page.getByRole('button', { name: /sign in/i }).click();

    await page.getByRole('link', { name: /notifications/i }).click();
    await expect(page.getByRole('heading', { name: /notification preferences/i })).toBeVisible();
    await expect(page.getByLabel(/trade alerts/i)).toBeVisible();

    const tradeAlerts = page.getByLabel(/trade alerts/i);
    const wasChecked = await tradeAlerts.isChecked();
    await tradeAlerts.setChecked(!wasChecked);

    await page.getByRole('button', { name: /save preferences/i }).click();
    await expect(page.getByText(/preferences saved/i)).toBeVisible({ timeout: 10_000 });

    await tradeAlerts.setChecked(wasChecked);
    await page.getByRole('button', { name: /save preferences/i }).click();
    await expect(page.getByText(/preferences saved/i)).toBeVisible({ timeout: 10_000 });
  });
});
