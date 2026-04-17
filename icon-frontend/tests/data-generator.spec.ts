import { test } from '@playwright/test';
import { faker } from '@faker-js/faker/locale/ko';

// faker 설치가 필요한 경우: npm install --save-dev @faker-js/faker

test.describe('대량 데이터 생성', () => {
  test('faker를 사용한 현실적인 데이터 생성', async ({ page }) => {
    // 로그인
    await page.goto('/login');
    await page.getByLabel('이메일').fill('admin@test.com');
    await page.getByLabel('비밀번호').fill('password123');
    await page.getByRole('button', { name: '로그인' }).click();
    await page.waitForURL('/');

    // 사용자 100명 생성
    await page.goto('/users');
    
    for (let i = 0; i < 100; i++) {
      await page.getByRole('button', { name: '새 사용자 추가' }).click();
      
      const userData = {
        name: faker.person.fullName(),
        email: faker.internet.email(),
        password: 'Test1234!',
        phone: faker.phone.number('010-####-####'),
        department: faker.helpers.arrayElement(['개발팀', '기획팀', '디자인팀', '마케팅팀', '인사팀', '재무팀', '영업팀']),
        position: faker.helpers.arrayElement(['사원', '대리', '과장', '차장', '부장', '이사'])
      };
      
      await page.getByLabel('이름').fill(userData.name);
      await page.getByLabel('이메일').fill(userData.email);
      await page.getByLabel('비밀번호').fill(userData.password);
      await page.getByLabel('전화번호').fill(userData.phone);
      await page.getByLabel('부서').fill(userData.department);
      await page.getByLabel('직급').fill(userData.position);
      
      await page.getByRole('button', { name: '등록' }).click();
      
      // 서버 부하 방지를 위한 대기
      if (i % 10 === 0) {
        await page.waitForTimeout(2000);
      }
    }
  });

  test('CSV 파일에서 데이터 읽어와서 등록', async ({ page }) => {
    // CSV 데이터 (실제로는 파일에서 읽어올 수 있음)
    const csvData = `
이름,이메일,전화번호,부서,직급
김철수,chulsoo.kim@example.com,010-1234-5678,개발팀,과장
이영희,younghee.lee@example.com,010-2345-6789,기획팀,대리
박민수,minsoo.park@example.com,010-3456-7890,디자인팀,사원
정수연,sooyeon.jung@example.com,010-4567-8901,마케팅팀,차장
최준호,junho.choi@example.com,010-5678-9012,인사팀,부장
`.trim();

    const rows = csvData.split('\n').slice(1); // 헤더 제외
    
    // 로그인
    await page.goto('/login');
    await page.getByLabel('이메일').fill('admin@test.com');
    await page.getByLabel('비밀번호').fill('password123');
    await page.getByRole('button', { name: '로그인' }).click();
    await page.waitForURL('/');

    await page.goto('/users');
    
    for (const row of rows) {
      const [name, email, phone, department, position] = row.split(',');
      
      await page.getByRole('button', { name: '새 사용자 추가' }).click();
      
      await page.getByLabel('이름').fill(name);
      await page.getByLabel('이메일').fill(email);
      await page.getByLabel('비밀번호').fill('Test1234!');
      await page.getByLabel('전화번호').fill(phone);
      await page.getByLabel('부서').fill(department);
      await page.getByLabel('직급').fill(position);
      
      await page.getByRole('button', { name: '등록' }).click();
      await page.waitForTimeout(500);
    }
  });

  test('API를 직접 호출하여 대량 데이터 생성', async ({ page, request }) => {
    // 로그인하여 토큰 획득
    const loginResponse = await request.post('http://localhost:8089/api/v1/auth/login', {
      data: {
        email: 'admin@test.com',
        password: 'password123'
      }
    });
    
    const { accessToken } = await loginResponse.json();
    
    // 100개의 사용자 데이터를 API로 직접 생성
    for (let i = 0; i < 100; i++) {
      const userData = {
        name: `API테스트사용자_${i}`,
        email: `apitest_${i}_${Date.now()}@example.com`,
        password: 'Test1234!',
        phone: `010-${String(Math.floor(Math.random() * 9000) + 1000)}-${String(Math.floor(Math.random() * 9000) + 1000)}`,
        department: ['개발팀', '기획팀', '디자인팀'][i % 3],
        position: ['사원', '대리', '과장', '차장', '부장'][i % 5]
      };
      
      await request.post('http://localhost:8089/api/v1/users', {
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json'
        },
        data: userData
      });
    }
    
    // UI에서 결과 확인
    await page.goto('/users');
    await page.getByLabel('이메일').fill('admin@test.com');
    await page.getByLabel('비밀번호').fill('password123');
    await page.getByRole('button', { name: '로그인' }).click();
    
    await page.goto('/users');
    await page.waitForTimeout(2000);
  });
});