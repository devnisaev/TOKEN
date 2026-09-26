import { expect, test } from '@playwright/test';

const gatewayUrl = process.env.E2E_GATEWAY_URL;

function currentPeriod(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

test.describe('Tenant rent full flow', () => {
  test.skip(!gatewayUrl, 'Set E2E_GATEWAY_URL to run against a live gateway');

  test('tenant can sign in, view lease, and pay rent', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel(/email/i).fill('tenant@tokenrealty.com');
    await page.getByLabel(/password/i).fill('tenant123');
    await page.getByRole('button', { name: /sign in/i }).click();

    await expect(page.getByRole('heading', { name: /my lease/i })).toBeVisible({ timeout: 15_000 });
    await expect(page.getByText(/\$650\.00/i)).toBeVisible();

    const payButton = page.getByRole('button', { name: /pay \$/i });
    const alreadyPaid = page.getByText(/already paid/i);

    if (await payButton.isVisible()) {
      await expect(page.getByLabel(/period/i)).toHaveValue(currentPeriod());
      await payButton.click();
      await expect(page.getByText(/recorded/i)).toBeVisible({ timeout: 15_000 });
      await expect(page.getByText(currentPeriod())).toBeVisible();
    } else {
      await expect(alreadyPaid).toBeVisible();
    }
  });
});
