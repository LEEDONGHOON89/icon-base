import { test, expect } from '@playwright/test';

test.describe('Navigation', () => {
  test('should navigate between pages', async ({ page }) => {
    // Start from the index page
    await page.goto('/');
    
    // Navigate to users page
    await page.getByRole('link', { name: '사용자' }).click();
    await expect(page).toHaveURL('/users');
    await expect(page.getByText('사용자 목록')).toBeVisible();
    
    // Navigate to rules page
    await page.getByRole('link', { name: '규칙' }).click();
    await expect(page).toHaveURL('/rules');
    await expect(page.getByText('규칙 목록')).toBeVisible();
    
    // Navigate to roles page
    await page.getByRole('link', { name: '역할' }).click();
    await expect(page).toHaveURL('/roles');
    
    // Navigate to scenarios page
    await page.getByRole('link', { name: '시나리오' }).click();
    await expect(page).toHaveURL('/scenarios');
  });
});