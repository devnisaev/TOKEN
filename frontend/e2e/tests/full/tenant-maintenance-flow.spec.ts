import { expect, test } from '@playwright/test';

const gatewayUrl = process.env.E2E_GATEWAY_URL;

test.describe('Tenant maintenance full flow', () => {
  test.skip(!gatewayUrl, 'Set E2E_GATEWAY_URL to run against a live gateway');

  test('tenant can sign in, open maintenance, and submit a ticket when lease exists', async ({
    page,
  }) => {
    await page.goto('/login');
    await page.getByLabel(/email/i).fill('tenant@tokenrealty.com');
    await page.getByLabel(/password/i).fill('tenant123');
    await page.getByRole('button', { name: /sign in/i }).click();

    await page.goto('/maintenance');
    await expect(page.getByRole('heading', { name: /maintenance/i })).toBeVisible({ timeout: 15_000 });

    const noLease = page.getByText(/no active lease/i);
    if (await noLease.isVisible()) {
      test.skip(true, 'No active lease seeded — run seed-demo.sh first');
    }

    const ticketTitle = `E2E leak ${Date.now()}`;
    await page.getByLabel(/issue title/i).fill(ticketTitle);
    await page.getByLabel(/description/i).fill('Playwright maintenance ticket');
    await page.getByRole('button', { name: /submit request/i }).click();

    await expect(page.getByText(ticketTitle)).toBeVisible({ timeout: 15_000 });
    await expect(page.getByText(/open/i).first()).toBeVisible();
  });
});
