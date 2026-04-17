package com.itmasters.icon.api.common.util;

/**
 * 문자열 처리 유틸리티
 * Spring에 의존하지 않는 순수 Java 유틸리티
 */
public class StringUtil {
    
    /**
     * 문자열이 null이거나 빈 문자열인지 확인
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 문자열이 null이 아니고 빈 문자열이 아닌지 확인
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    /**
     * 문자열을 안전하게 trim (null 처리)
     */
    public static String safeTrim(String str) {
        return str == null ? null : str.trim();
    }
    
    /**
     * 기본값과 함께 문자열 반환
     */
    public static String defaultIfEmpty(String str, String defaultValue) {
        return isEmpty(str) ? defaultValue : str;
    }
    
    /**
     * 문자열 마스킹 (이메일, 전화번호 등)
     */
    public static String mask(String str, int visibleStart, int visibleEnd, char maskChar) {
        if (isEmpty(str) || str.length() <= visibleStart + visibleEnd) {
            return str;
        }
        
        StringBuilder masked = new StringBuilder();
        masked.append(str.substring(0, visibleStart));
        
        int maskLength = str.length() - visibleStart - visibleEnd;
        for (int i = 0; i < maskLength; i++) {
            masked.append(maskChar);
        }
        
        masked.append(str.substring(str.length() - visibleEnd));
        return masked.toString();
    }
}