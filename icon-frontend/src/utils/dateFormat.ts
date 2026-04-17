import dayjs from 'dayjs';
import 'dayjs/locale/ko';
import relativeTime from 'dayjs/plugin/relativeTime';

// 플러그인 설정
dayjs.extend(relativeTime);
dayjs.locale('ko');

/**
 * 날짜 포맷 유틸리티 함수들
 */
export const formatDate = {
  /**
   * 날짜만 표시 (YYYY-MM-DD)
   */
  date: (date?: string | Date | null) => {
    if (!date) return '-';
    return dayjs(date).format('YYYY-MM-DD');
  },

  /**
   * 날짜와 시간 표시 (YYYY-MM-DD HH:mm:ss)
   */
  datetime: (date?: string | Date | null) => {
    if (!date) return '-';
    return dayjs(date).format('YYYY-MM-DD HH:mm:ss');
  },

  /**
   * 시간만 표시 (HH:mm:ss)
   */
  time: (date?: string | Date | null) => {
    if (!date) return '-';
    return dayjs(date).format('HH:mm:ss');
  },

  /**
   * 한국어 날짜 표시 (YYYY년 MM월 DD일)
   */
  koreanDate: (date?: string | Date | null) => {
    if (!date) return '-';
    return dayjs(date).format('YYYY년 MM월 DD일');
  },

  /**
   * 한국어 날짜시간 표시 (YYYY년 MM월 DD일 HH시 mm분)
   */
  koreanDatetime: (date?: string | Date | null) => {
    if (!date) return '-';
    return dayjs(date).format('YYYY년 MM월 DD일 HH시 mm분');
  },

  /**
   * 상대 시간 표시 (1일 전, 2시간 전 등)
   */
  relative: (date?: string | Date | null) => {
    if (!date) return '-';
    return dayjs(date).fromNow();
  },

  /**
   * 커스텀 포맷
   */
  custom: (date: string | Date | null | undefined, format: string) => {
    if (!date) return '-';
    return dayjs(date).format(format);
  },
};

/**
 * 날짜 유효성 검증
 */
export const isValidDate = (date?: string | Date | null): boolean => {
  if (!date) return false;
  return dayjs(date).isValid();
};

/**
 * 두 날짜 간 차이 계산
 */
export const dateDiff = {
  days: (start: string | Date, end: string | Date) => {
    return dayjs(end).diff(dayjs(start), 'day');
  },
  hours: (start: string | Date, end: string | Date) => {
    return dayjs(end).diff(dayjs(start), 'hour');
  },
  minutes: (start: string | Date, end: string | Date) => {
    return dayjs(end).diff(dayjs(start), 'minute');
  },
};