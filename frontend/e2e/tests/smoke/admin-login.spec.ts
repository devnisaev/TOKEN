import { expect, test } from '@playwright/test';

test.describe('Admin dashboard smoke', () => {
  test('login page renders', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByRole('heading', { name: /admin sign in/i })).toBeVisible();
    await expect(page.getByLabel(/email/i)).toBeVisible();
  });

  test('buildings redirects to login when unauthenticated', async ({ page }) => {
    await page.goto('/buildings');
    await expect(page).toHaveURL(/\/login$/);
  });
});
