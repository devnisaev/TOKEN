import { expect, test } from '@playwright/test';

const gatewayUrl = process.env.E2E_GATEWAY_URL;
const SETTLED_TIMEOUT_MS = 180_000;

test.describe('Investor buy until settled', () => {
  test.skip(!gatewayUrl, 'Set E2E_GATEWAY_URL to run against a live gateway');

  test('investor buy order reaches SETTLED status', async ({ page }) => {
    test.setTimeout(SETTLED_TIMEOUT_MS + 30_000);

    await page.goto('/login');
    await page.getByLabel(/email/i).fill('investor@tokenrealty.com');
    await page.getByLabel(/password/i).fill('investor123');
    await page.getByRole('button', { name: /sign in/i }).click();
    await expect(page).toHaveURL(/\/(\?.*)?$/);

    const listingLink = page.locator('a[href^="/listings/"]').first();
    test.skip((await listingLink.count()) === 0, 'No listings seeded — run seed-tokenize-demo.sh first');

    await listingLink.click();
    await expect(page.getByRole('heading', { name: /buy tokens/i })).toBeVisible();

    await page.getByLabel(/token amount/i).fill('1');
    await page.getByRole('button', { name: /place buy order/i }).click();

    await expect(page).toHaveURL(/\/orders\/[0-9a-f-]+/i, { timeout: 15_000 });
    await expect(page.getByText(/^SETTLED$/i).first()).toBeVisible({ timeout: SETTLED_TIMEOUT_MS });
  });
});
