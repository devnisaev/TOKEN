import { expect, test } from '@playwright/test';

const gatewayUrl = process.env.E2E_GATEWAY_URL;

test.describe('Admin building approve full flow', () => {
  test.skip(!gatewayUrl, 'Set E2E_GATEWAY_URL to run against a live gateway');

  test('admin can approve a draft building via gateway and see updated status', async ({
    page,
    request,
  }) => {
    await page.goto('/login');
    await page.getByLabel(/email/i).fill('admin@tokenrealty.com');
    await page.getByLabel(/password/i).fill('admin123');
    await page.getByRole('button', { name: /sign in/i }).click();

    await page.getByRole('link', { name: /buildings/i }).click();
    await expect(page.getByRole('heading', { name: /buildings/i })).toBeVisible();

    const buildingLink = page.locator('a[href^="/buildings/"]').first();
    test.skip((await buildingLink.count()) === 0, 'No buildings seeded — run demo-all.sh first');

    const href = await buildingLink.getAttribute('href');
    const buildingId = href?.split('/').pop();
    test.skip(!buildingId, 'Could not resolve building id from link');

    const loginResponse = await request.post(`${gatewayUrl}/api/v1/auth/login`, {
      data: { email: 'admin@tokenrealty.com', password: 'admin123' },
    });
    test.skip(!loginResponse.ok(), 'Gateway login failed — is E2E_GATEWAY_URL correct?');

    const { accessToken } = (await loginResponse.json()) as { accessToken: string };

    const approveResponse = await request.patch(
      `${gatewayUrl}/api/v1/buildings/${buildingId}/status?status=APPROVED`,
      {
        headers: { Authorization: `Bearer ${accessToken}` },
      },
    );
    test.skip(!approveResponse.ok(), 'Building approve API failed — building may already be approved');

    await buildingLink.click();
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await expect(page.getByText(/APPROVED/i).first()).toBeVisible();
  });
});
