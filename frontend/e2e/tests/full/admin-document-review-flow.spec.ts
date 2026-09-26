import { expect, test } from '@playwright/test';

const gatewayUrl = process.env.E2E_GATEWAY_URL;

test.describe('Admin document review full flow', () => {
  test.skip(!gatewayUrl, 'Set E2E_GATEWAY_URL to run against a live gateway');

  test('admin can open document reviews and approve when pending', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel(/email/i).fill('admin@tokenrealty.com');
    await page.getByLabel(/password/i).fill('admin123');
    await page.getByRole('button', { name: /sign in/i }).click();

    await page.getByRole('link', { name: /document reviews/i }).click();
    await expect(page.getByRole('heading', { name: /document reviews/i })).toBeVisible();

    const approveButton = page.getByRole('button', { name: /approve document/i }).first();
    if (await approveButton.isVisible()) {
      await approveButton.click();
      await expect(page.getByText(/no documents pending review/i)).toBeVisible({ timeout: 15_000 });
    } else {
      await expect(page.getByText(/no documents pending review/i)).toBeVisible();
    }
  });
});
