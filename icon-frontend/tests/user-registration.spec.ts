import { test, expect } from '@playwright/test';

test.describe('사용자 등록', () => {
  test('새 사용자를 등록한다', async ({ page }) => {
    // 로그인 먼저 수행 (필요한 경우)
    await page.goto('/login');
    await page.getByLabel('이메일').fill('admin@test.com');
    await page.getByLabel('비밀번호').fill('password123');
    await page.getByRole('button', { name: '로그인' }).click();
    await page.waitForURL('/');

    // 사용자 페이지로 이동
    await page.goto('/users');
    
    // 새 사용자 추가 버튼 클릭
    await page.getByRole('button', { name: '새 사용자 추가' }).click();
    
    // 랜덤 데이터 생성
    const timestamp = Date.now();
    const userData = {
      name: `테스트사용자_${timestamp}`,
      email: `test_${timestamp}@example.com`,
      password: 'Test1234!',
      phone: `010-${Math.floor(Math.random() * 9000) + 1000}-${Math.floor(Math.random() * 9000) + 1000}`,
      department: '개발팀',
      position: '개발자'
    };
    
    // 폼 입력
    await page.getByLabel('이름').fill(userData.name);
    await page.getByLabel('이메일').fill(userData.email);
    await page.getByLabel('비밀번호').fill(userData.password);
    await page.getByLabel('전화번호').fill(userData.phone);
    await page.getByLabel('부서').fill(userData.department);
    await page.getByLabel('직급').fill(userData.position);
    
    // 등록 버튼 클릭
    await page.getByRole('button', { name: '등록' }).click();
    
    // 성공 메시지 확인
    await expect(page.getByText(/등록되었습니다/)).toBeVisible();
    
    // 목록에서 새로 등록된 사용자 확인
    await expect(page.getByText(userData.name)).toBeVisible();
    await expect(page.getByText(userData.email)).toBeVisible();
  });

  test('여러 사용자를 연속으로 등록한다', async ({ page }) => {
    // 로그인
    await page.goto('/login');
    await page.getByLabel('이메일').fill('admin@test.com');
    await page.getByLabel('비밀번호').fill('password123');
    await page.getByRole('button', { name: '로그인' }).click();
    await page.waitForURL('/');

    await page.goto('/users');
    
    // 5명의 사용자를 등록
    for (let i = 1; i <= 5; i++) {
      await page.getByRole('button', { name: '새 사용자 추가' }).click();
      
      const userData = {
        name: `테스트사용자_${Date.now()}_${i}`,
        email: `test_${Date.now()}_${i}@example.com`,
        password: 'Test1234!',
        phone: `010-${Math.floor(Math.random() * 9000) + 1000}-${Math.floor(Math.random() * 9000) + 1000}`,
        department: ['개발팀', '기획팀', '디자인팀', '마케팅팀', '인사팀'][Math.floor(Math.random() * 5)],
        position: ['사원', '대리', '과장', '차장', '부장'][Math.floor(Math.random() * 5)]
      };
      
      await page.getByLabel('이름').fill(userData.name);
      await page.getByLabel('이메일').fill(userData.email);
      await page.getByLabel('비밀번호').fill(userData.password);
      await page.getByLabel('전화번호').fill(userData.phone);
      await page.getByLabel('부서').fill(userData.department);
      await page.getByLabel('직급').fill(userData.position);
      
      await page.getByRole('button', { name: '등록' }).click();
      
      // 잠시 대기
      await page.waitForTimeout(1000);
    }
    
    // 모든 사용자가 등록되었는지 확인
    await expect(page.locator('text=/테스트사용자_/')).toHaveCount(5);
  });
});