import { test, expect } from '@playwright/test';

test.describe('규칙 등록', () => {
  test('새 규칙을 등록한다', async ({ page }) => {
    // 로그인
    await page.goto('/login');
    await page.getByLabel('이메일').fill('admin@test.com');
    await page.getByLabel('비밀번호').fill('password123');
    await page.getByRole('button', { name: '로그인' }).click();
    await page.waitForURL('/');

    // 규칙 페이지로 이동
    await page.goto('/rules');
    
    // 새 규칙 추가 버튼 클릭
    await page.getByRole('button', { name: '새 규칙 추가' }).click();
    
    // 규칙 데이터 생성
    const ruleData = {
      name: `테스트규칙_${Date.now()}`,
      description: '자동 생성된 테스트 규칙입니다.',
      systemType: 'SYSTEM_A',
      priority: Math.floor(Math.random() * 10) + 1,
      conditions: [
        {
          field: 'amount',
          operator: '>',
          value: '10000'
        },
        {
          field: 'status',
          operator: '=',
          value: 'ACTIVE'
        }
      ]
    };
    
    // 규칙 정보 입력
    await page.getByLabel('규칙명').fill(ruleData.name);
    await page.getByLabel('설명').fill(ruleData.description);
    await page.getByLabel('시스템 타입').selectOption(ruleData.systemType);
    await page.getByLabel('우선순위').fill(ruleData.priority.toString());
    
    // 조건 추가
    for (const condition of ruleData.conditions) {
      await page.getByRole('button', { name: '조건 추가' }).click();
      await page.getByLabel('필드').last().fill(condition.field);
      await page.getByLabel('연산자').last().selectOption(condition.operator);
      await page.getByLabel('값').last().fill(condition.value);
    }
    
    // 등록 버튼 클릭
    await page.getByRole('button', { name: '등록' }).click();
    
    // 성공 메시지 확인
    await expect(page.getByText(/등록되었습니다/)).toBeVisible();
    
    // 목록에서 확인
    await expect(page.getByText(ruleData.name)).toBeVisible();
  });

  test('복잡한 규칙을 여러 개 등록한다', async ({ page }) => {
    // 로그인
    await page.goto('/login');
    await page.getByLabel('이메일').fill('admin@test.com');
    await page.getByLabel('비밀번호').fill('password123');
    await page.getByRole('button', { name: '로그인' }).click();
    await page.waitForURL('/');

    await page.goto('/rules');
    
    const ruleTemplates = [
      {
        name: '주문금액 할인 규칙',
        conditions: [
          { field: 'orderAmount', operator: '>=', value: '100000' },
          { field: 'memberGrade', operator: 'IN', value: 'GOLD,PLATINUM' }
        ]
      },
      {
        name: '신규회원 이벤트 규칙',
        conditions: [
          { field: 'registrationDate', operator: '>', value: '2024-01-01' },
          { field: 'firstPurchase', operator: '=', value: 'true' }
        ]
      },
      {
        name: '재고부족 알림 규칙',
        conditions: [
          { field: 'stockQuantity', operator: '<', value: '10' },
          { field: 'productCategory', operator: '=', value: 'ELECTRONICS' }
        ]
      }
    ];
    
    for (const template of ruleTemplates) {
      await page.getByRole('button', { name: '새 규칙 추가' }).click();
      
      const ruleName = `${template.name}_${Date.now()}`;
      await page.getByLabel('규칙명').fill(ruleName);
      await page.getByLabel('설명').fill(`${template.name}에 대한 자동화 테스트 규칙`);
      await page.getByLabel('시스템 타입').selectOption('SYSTEM_A');
      await page.getByLabel('우선순위').fill(String(Math.floor(Math.random() * 10) + 1));
      
      // 조건 추가
      for (const condition of template.conditions) {
        await page.getByRole('button', { name: '조건 추가' }).click();
        await page.getByLabel('필드').last().fill(condition.field);
        await page.getByLabel('연산자').last().selectOption(condition.operator);
        await page.getByLabel('값').last().fill(condition.value);
      }
      
      await page.getByRole('button', { name: '등록' }).click();
      await page.waitForTimeout(1000);
    }
    
    // 모든 규칙이 등록되었는지 확인
    for (const template of ruleTemplates) {
      await expect(page.getByText(new RegExp(template.name))).toBeVisible();
    }
  });
});