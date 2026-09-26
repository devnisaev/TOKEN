import { expect, test } from '@playwright/test';

const gatewayUrl = process.env.E2E_GATEWAY_URL;

test.describe('Admin building detail full flow', () => {
  test.skip(!gatewayUrl, 'Set E2E_GATEWAY_URL to run against a live gateway');

  test('admin can open building detail and see flats', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel(/email/i).fill('admin@tokenrealty.com');
    await page.getByLabel(/password/i).fill('admin123');
    await page.getByRole('button', { name: /sign in/i }).click();

    await page.getByRole('link', { name: /buildings/i }).click();
    await expect(page.getByRole('heading', { name: /buildings/i })).toBeVisible();

    const buildingLink = page.locator('a[href^="/buildings/"]').first();
    test.skip((await buildingLink.count()) === 0, 'No buildings seeded — run demo-all.sh first');

    await buildingLink.click();
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await expect(page.getByText(/flats|tokenized|available/i).first()).toBeVisible();
  });
});
