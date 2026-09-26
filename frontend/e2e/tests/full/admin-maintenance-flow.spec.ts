import { expect, test } from '@playwright/test';

const gatewayUrl = process.env.E2E_GATEWAY_URL;

test.describe('Admin maintenance full flow', () => {
  test.skip(!gatewayUrl, 'Set E2E_GATEWAY_URL to run against a live gateway');

  test('admin can open maintenance queue and resolve open tickets', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel(/email/i).fill('admin@tokenrealty.com');
    await page.getByLabel(/password/i).fill('admin123');
    await page.getByRole('button', { name: /sign in/i }).click();

    await page.goto('/maintenance');
    await expect(page.getByRole('heading', { name: /maintenance tickets/i })).toBeVisible({
      timeout: 15_000,
    });

    const resolveButton = page.getByRole('button', { name: /mark resolved/i }).first();
    if ((await resolveButton.count()) === 0) {
      await expect(page.getByText(/no tickets|tenant maintenance requests will appear/i)).toBeVisible();
      return;
    }

    await resolveButton.click();
    await expect(page.getByText(/^RESOLVED$/i).first()).toBeVisible({ timeout: 15_000 });
  });
});
