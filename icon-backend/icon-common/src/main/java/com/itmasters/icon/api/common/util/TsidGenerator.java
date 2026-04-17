package com.itmasters.icon.common.util;

import io.hypersistence.tsid.TSID;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TSID 생성 유틸리티 클래스
 * Spring에 의존하지 않는 순수한 유틸리티
 * 모든 서비스에서 공통으로 사용하는 ID 생성 정책
 */
public class TsidGenerator {
    
    // Factory 캐시 - 노드별로 하나씩만 생성
    private static final Map<Integer, TSID.Factory> FACTORY_CACHE = new ConcurrentHashMap<>();
    
    /**
     * 지정된 노드 ID로 새로운 TSID 생성
     * @param nodeId 노드 ID (0-1023)
     * @return TSID 문자열 (13자)
     */
    public static String generate(int nodeId) {
        if (nodeId < 0 || nodeId > 1023) {
            throw new IllegalArgumentException("NodeId must be between 0 and 1023");
        }
        
        TSID.Factory factory = FACTORY_CACHE.computeIfAbsent(nodeId, id -> 
            TSID.Factory.builder()
                    .withNodeBits(10)
                    .withNode(id)
                    .build()
        );
        
        return factory.generate().toString();
    }
    
    /**
     * 기본 노드 ID(1)로 TSID 생성 (테스트용)
     * @return TSID 문자열 (13자)
     */
    public static String generate() {
        return generate(1); // 기본 nodeId
    }
    
    /**
     * TSID 유효성 검증
     * @param tsid TSID 문자열
     * @return 유효한 TSID인지 여부
     */
    public static boolean isValid(String tsid) {
        if (tsid == null || tsid.length() != 13) {
            return false;
        }
        try {
            TSID.from(tsid);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * TSID를 Long 형태로 생성
     * @param nodeId 노드 ID (0-1023)
     * @return TSID Long 값
     */
    public static long generateLong(int nodeId) {
        if (nodeId < 0 || nodeId > 1023) {
            throw new IllegalArgumentException("NodeId must be between 0 and 1023");
        }
        
        TSID.Factory factory = FACTORY_CACHE.computeIfAbsent(nodeId, id -> 
            TSID.Factory.builder()
                    .withNodeBits(10)
                    .withNode(id)
                    .build()
        );
        
        return factory.generate().toLong();
    }
}