/**
 * 분(minutes)을 일/시/분 형태로 변환
 * @param minutes 분 단위 시간
 * @returns 사람이 읽기 쉬운 형식의 문자열
 * 
 * @example
 * formatMinutes(1440) // "1일"
 * formatMinutes(90)   // "1시간 30분"
 * formatMinutes(30)   // "30분"
 */
export function formatMinutes(minutes: number | null | undefined): string {
  if (minutes === null || minutes === undefined) {
    return '-';
  }

  const days = Math.floor(minutes / 1440);
  const hours = Math.floor((minutes % 1440) / 60);
  const mins = minutes % 60;

  const parts: string[] = [];

  if (days > 0) {
    parts.push(`${days}일`);
  }
  if (hours > 0) {
    parts.push(`${hours}시간`);
  }
  if (mins > 0) {
    parts.push(`${mins}분`);
  }

  return parts.length > 0 ? parts.join(' ') : '0분';
}
