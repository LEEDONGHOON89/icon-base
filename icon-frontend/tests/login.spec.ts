import { test, expect } from '@playwright/test';

test.describe('Login Page', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
  });

  test('should display login form', async ({ page }) => {
    // Check if the login form elements are visible
    await expect(page.getByLabel('이메일')).toBeVisible();
    await expect(page.getByLabel('비밀번호')).toBeVisible();
    await expect(page.getByRole('button', { name: '로그인' })).toBeVisible();
  });

  test('should show error on invalid credentials', async ({ page }) => {
    // Fill in invalid credentials
    await page.getByLabel('이메일').fill('invalid@example.com');
    await page.getByLabel('비밀번호').fill('wrongpassword');
    
    // Click login button
    await page.getByRole('button', { name: '로그인' }).click();
    
    // Wait for error message
    await expect(page.getByText(/로그인에 실패했습니다/)).toBeVisible();
  });

  test('should login with valid credentials', async ({ page }) => {
    // Fill in valid credentials (adjust these to match your test data)
    await page.getByLabel('이메일').fill('admin@test.com');
    await page.getByLabel('비밀번호').fill('password123');
    
    // Click login button
    await page.getByRole('button', { name: '로그인' }).click();
    
    // Should redirect to home page after successful login
    await expect(page).toHaveURL('/');
  });
});