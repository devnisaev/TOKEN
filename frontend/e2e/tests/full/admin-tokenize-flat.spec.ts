import { expect, test } from '@playwright/test';

const gatewayUrl = process.env.E2E_GATEWAY_URL;

test.describe('Admin tokenize flat full flow', () => {
  test.skip(!gatewayUrl, 'Set E2E_GATEWAY_URL to run against a live gateway');

  test('admin can open tokenize form for an available flat', async ({ page }) => {
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

    const tokenizeLink = page.getByRole('link', { name: /^tokenize$/i }).first();
    test.skip((await tokenizeLink.count()) === 0, 'No AVAILABLE flats — seed demo data first');

    await tokenizeLink.click();
    await expect(page.getByRole('heading', { name: /tokenize flat/i })).toBeVisible();
    await expect(page.getByLabel(/token name/i)).toBeVisible();
    await expect(page.getByRole('button', { name: /issue tokens/i })).toBeVisible();
  });
});
