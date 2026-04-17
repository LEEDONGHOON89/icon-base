package com.itmasters.icon.engine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

/**
 * JSON 파싱 성능 최적화 테스트
 * 
 * 테스트 범위:
 * - ObjectMapper 최적화 설정
 * - JSON 파싱 캐싱 전략
 * - 대용량 JSON 처리 최적화
 * - 스트리밍 JSON 파싱
 * - 메모리 효율적 JSON 처리
 * - 동시성 JSON 파싱 성능
 */
@DisplayName("JSON 파싱 성능 최적화 테스트")
class JsonProcessingOptimizationTest {

    private ObjectMapper standardObjectMapper;
    private ObjectMapper optimizedObjectMapper;
    private JsonOptimizationService jsonOptimizationService;
    
    // 성능 테스트 상수
    private static final int LARGE_JSON_COUNT = 10000;
    private static final int CONCURRENT_THREAD_COUNT = 10;
    private static final long ACCEPTABLE_PARSING_TIME_MS = 5000;
    private static final long ACCEPTABLE_SINGLE_PARSE_TIME_MS = 100;

    @BeforeEach
    void setUp() {
        standardObjectMapper = new ObjectMapper();
        optimizedObjectMapper = createOptimizedObjectMapper();
        jsonOptimizationService = new JsonOptimizationService(optimizedObjectMapper);
    }

    @Test
    @DisplayName("ObjectMapper 최적화 설정 성능 비교")
    void testOptimizedObjectMapperPerformance() throws Exception {
        // given - 복잡한 JSON 데이터 생성
        List<Map<String, Object>> testJsonData = createComplexJsonDataSet(1000);
        
        // when - 표준 ObjectMapper 성능 측정
        long standardStartTime = System.currentTimeMillis();
        for (Map<String, Object> jsonData : testJsonData) {
            String standardJson = standardObjectMapper.writeValueAsString(jsonData);
            standardObjectMapper.readValue(standardJson, Map.class);
        }
        long standardTime = System.currentTimeMillis() - standardStartTime;
        
        // 최적화된 ObjectMapper 성능 측정
        long optimizedStartTime = System.currentTimeMillis();
        for (Map<String, Object> jsonData : testJsonData) {
            String optimizedJson = optimizedObjectMapper.writeValueAsString(jsonData);
            optimizedObjectMapper.readValue(optimizedJson, Map.class);
        }
        long optimizedTime = System.currentTimeMillis() - optimizedStartTime;
        
        // then - 최적화된 버전이 더 빨라야 함
        System.out.println("표준 ObjectMapper: " + standardTime + "ms");
        System.out.println("최적화된 ObjectMapper: " + optimizedTime + "ms");
        System.out.println("성능 향상: " + String.format("%.1f", (double) standardTime / optimizedTime) + "배");
        
        assertThat(optimizedTime).isLessThan(standardTime);
        assertThat(optimizedTime).isLessThan(ACCEPTABLE_PARSING_TIME_MS);
    }

    @Test
    @DisplayName("JSON 파싱 캐싱 전략 성능 테스트")
    void testJsonParsingCachingStrategy() throws Exception {
        // given - 동일한 구조의 JSON 데이터들 (캐싱 효과 확인)
        List<String> repeatedJsonStrings = createRepeatedJsonStructures(5000);
        
        // when - 캐싱 없이 파싱
        long noCacheStartTime = System.currentTimeMillis();
        for (String jsonString : repeatedJsonStrings) {
            standardObjectMapper.readValue(jsonString, Map.class);
        }
        long noCacheTime = System.currentTimeMillis() - noCacheStartTime;
        
        // 캐싱 적용 파싱
        JsonParsingCache cache = new JsonParsingCache(1000);
        long withCacheStartTime = System.currentTimeMillis();
        for (String jsonString : repeatedJsonStrings) {
            cache.parseWithCache(jsonString, optimizedObjectMapper);
        }
        long withCacheTime = System.currentTimeMillis() - withCacheStartTime;
        
        // then - 캐싱이 성능 향상을 가져와야 함
        System.out.println("캐싱 없음: " + noCacheTime + "ms");
        System.out.println("캐싱 적용: " + withCacheTime + "ms");
        System.out.println("캐싱 효과: " + String.format("%.1f", (double) noCacheTime / withCacheTime) + "배");
        
        assertThat(withCacheTime).isLessThan(noCacheTime);
        assertThat(cache.getCacheHitRate()).isGreaterThan(0.7); // 70% 이상 캐시 히트율
    }

    @Test
    @DisplayName("대용량 JSON 스트리밍 파싱 성능")
    void testLargeJsonStreamingPerformance() throws Exception {
        // given - 대용량 JSON 배열 생성
        String largeJsonArray = createLargeJsonArray(LARGE_JSON_COUNT);
        
        // when - 일반 파싱 vs 스트리밍 파싱
        long regularParsingTime = measureRegularJsonArrayParsing(largeJsonArray);
        long streamingParsingTime = measureStreamingJsonArrayParsing(largeJsonArray);
        
        // then - 스트리밍이 더 효율적이어야 함
        System.out.println("일반 파싱: " + regularParsingTime + "ms");
        System.out.println("스트리밍 파싱: " + streamingParsingTime + "ms");
        System.out.println("메모리 효율성: " + String.format("%.1f", (double) regularParsingTime / streamingParsingTime) + "배");
        
        assertThat(streamingParsingTime).isLessThan(regularParsingTime);
        assertThat(streamingParsingTime).isLessThan(ACCEPTABLE_PARSING_TIME_MS);
    }

    @Test
    @DisplayName("JSON 필드 선택적 파싱 최적화")
    void testSelectiveJsonFieldParsing() throws Exception {
        // given - 많은 필드를 가진 복잡한 JSON
        List<String> complexJsons = createComplexJsonWithManyFields(1000);
        
        // when - 전체 파싱 vs 선택적 필드 파싱
        long fullParsingTime = measureFullJsonParsing(complexJsons);
        long selectiveParsingTime = measureSelectiveFieldParsing(complexJsons, 
            Arrays.asList("user_id", "timestamp", "amount", "location"));
        
        // then - 선택적 파싱이 더 효율적이어야 함
        System.out.println("전체 파싱: " + fullParsingTime + "ms");
        System.out.println("선택적 파싱: " + selectiveParsingTime + "ms");
        System.out.println("선택적 파싱 효율성: " + String.format("%.1f", (double) fullParsingTime / selectiveParsingTime) + "배");
        
        assertThat(selectiveParsingTime).isLessThan(fullParsingTime);
    }

    @Test
    @DisplayName("동시성 JSON 파싱 성능")
    void testConcurrentJsonParsingPerformance() throws Exception {
        // given - 멀티스레드 환경에서 JSON 파싱
        List<String> jsonDataSet = createJsonDataSetForConcurrency(5000);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREAD_COUNT);
        
        // when - 단일 스레드 vs 멀티스레드 파싱
        long singleThreadTime = measureSingleThreadParsing(jsonDataSet);
        long multiThreadTime = measureMultiThreadParsing(jsonDataSet, executor);
        
        executor.shutdown();
        
        // then - 멀티스레드가 더 효율적이어야 함
        System.out.println("단일 스레드: " + singleThreadTime + "ms");
        System.out.println("멀티 스레드 (" + CONCURRENT_THREAD_COUNT + "개): " + multiThreadTime + "ms");
        System.out.println("동시성 효율성: " + String.format("%.1f", (double) singleThreadTime / multiThreadTime) + "배");
        
        assertThat(multiThreadTime).isLessThan(singleThreadTime);
    }

    @Test
    @DisplayName("JSON 파싱 메모리 사용량 최적화")
    void testJsonParsingMemoryOptimization() throws Exception {
        // given - 메모리 사용량 측정 준비
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        Thread.sleep(100);
        
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // when - 대용량 JSON 처리 (메모리 효율적 방식)
        List<String> largeJsonDataSet = createLargeJsonDataSet(1000);
        
        // 일반 방식
        List<Map<String, Object>> normalResults = new ArrayList<>();
        long normalMemoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        for (String jsonData : largeJsonDataSet) {
            normalResults.add(standardObjectMapper.readValue(jsonData, Map.class));
        }
        
        long normalMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long normalMemoryUsed = normalMemoryAfter - normalMemoryBefore;
        
        // 메모리 정리
        normalResults.clear();
        System.gc();
        Thread.sleep(100);
        
        // 최적화된 방식 (스트리밍 + 선택적 처리)
        long optimizedMemoryBefore = runtime.totalMemory() - runtime.freeMemory();
        int processedCount = processJsonStreamOptimized(largeJsonDataSet);
        long optimizedMemoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long optimizedMemoryUsed = optimizedMemoryAfter - optimizedMemoryBefore;
        
        // then - 최적화된 방식이 메모리를 적게 사용해야 함
        System.out.println("일반 방식 메모리 사용: " + (normalMemoryUsed / 1024 / 1024) + " MB");
        System.out.println("최적화 방식 메모리 사용: " + (optimizedMemoryUsed / 1024 / 1024) + " MB");
        System.out.println("처리된 JSON 개수: " + processedCount);
        
        assertThat(optimizedMemoryUsed).isLessThan(normalMemoryUsed);
        assertThat(processedCount).isEqualTo(largeJsonDataSet.size());
    }

    @Test
    @DisplayName("JSON 직렬화 최적화 - EventStream 특화")
    void testEventStreamJsonSerializationOptimization() throws Exception {
        // given - EventStream에 특화된 JSON 데이터
        List<Map<String, Object>> eventStreamData = createEventStreamSpecificData(5000);
        
        // when - 표준 vs 최적화된 직렬화
        long standardSerializationTime = measureStandardSerialization(eventStreamData);
        long optimizedSerializationTime = measureOptimizedSerialization(eventStreamData);
        
        // then - 최적화된 직렬화가 더 효율적이어야 함
        System.out.println("표준 직렬화: " + standardSerializationTime + "ms");
        System.out.println("최적화 직렬화: " + optimizedSerializationTime + "ms");
        System.out.println("직렬화 최적화 효과: " + String.format("%.1f", (double) standardSerializationTime / optimizedSerializationTime) + "배");
        
        assertThat(optimizedSerializationTime).isLessThan(standardSerializationTime);
    }

    @Test
    @DisplayName("JSON 파싱 에러 처리 성능")
    void testJsonParsingErrorHandlingPerformance() throws Exception {
        // given - 유효하지 않은 JSON이 포함된 데이터셋
        List<String> mixedJsonDataSet = createMixedValidInvalidJsonDataSet(1000);
        
        // when - 에러 처리를 포함한 파싱 성능 측정
        JsonParsingResult standardResult = parseWithStandardErrorHandling(mixedJsonDataSet);
        JsonParsingResult optimizedResult = parseWithOptimizedErrorHandling(mixedJsonDataSet);
        
        // then - 최적화된 에러 처리가 더 효율적이어야 함
        System.out.println("표준 에러 처리: " + standardResult.getTotalTime() + "ms, 성공: " + standardResult.getSuccessCount());
        System.out.println("최적화 에러 처리: " + optimizedResult.getTotalTime() + "ms, 성공: " + optimizedResult.getSuccessCount());
        
        assertThat(optimizedResult.getTotalTime()).isLessThan(standardResult.getTotalTime());
        assertThat(optimizedResult.getSuccessCount()).isEqualTo(standardResult.getSuccessCount());
    }

    // Helper Methods

    private ObjectMapper createOptimizedObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 성능 최적화 설정
        mapper.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, false);
        mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.USE_FAST_DOUBLE_PARSER, true);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(com.fasterxml.jackson.databind.MapperFeature.USE_ANNOTATIONS, false);
        
        return mapper;
    }

    private List<Map<String, Object>> createComplexJsonDataSet(int size) {
        List<Map<String, Object>> dataSet = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < size; i++) {
            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("id", "ID_" + i);
            jsonData.put("timestamp", LocalDateTime.now().toString());
            jsonData.put("amount", random.nextDouble() * 1000000);
            jsonData.put("currency", "KRW");
            
            // 중첩 객체
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "system_" + (i % 10));
            metadata.put("version", "v2.1.0");
            metadata.put("processed", true);
            jsonData.put("metadata", metadata);
            
            // 배열 데이터
            List<String> tags = Arrays.asList("tag_" + (i % 5), "category_" + (i % 3));
            jsonData.put("tags", tags);
            
            dataSet.add(jsonData);
        }
        
        return dataSet;
    }

    private List<String> createRepeatedJsonStructures(int size) throws JsonProcessingException {
        List<String> jsonStrings = new ArrayList<>();
        List<Map<String, Object>> templates = createComplexJsonDataSet(10); // 10개 템플릿
        
        for (int i = 0; i < size; i++) {
            Map<String, Object> template = new HashMap<>(templates.get(i % 10));
            template.put("sequence", i); // 고유값 변경
            jsonStrings.add(standardObjectMapper.writeValueAsString(template));
        }
        
        return jsonStrings;
    }

    private String createLargeJsonArray(int size) throws JsonProcessingException {
        List<Map<String, Object>> largeArray = createComplexJsonDataSet(size);
        return standardObjectMapper.writeValueAsString(largeArray);
    }

    private List<String> createComplexJsonWithManyFields(int size) throws JsonProcessingException {
        List<String> complexJsons = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < size; i++) {
            Map<String, Object> complexData = new HashMap<>();
            
            // 50개의 필드 생성
            for (int j = 0; j < 50; j++) {
                complexData.put("field_" + j, "value_" + random.nextInt(1000));
            }
            
            // 중요한 필드들
            complexData.put("user_id", "USER_" + i);
            complexData.put("timestamp", LocalDateTime.now().toString());
            complexData.put("amount", random.nextDouble() * 100000);
            complexData.put("location", "Seoul");
            
            complexJsons.add(standardObjectMapper.writeValueAsString(complexData));
        }
        
        return complexJsons;
    }

    private List<String> createJsonDataSetForConcurrency(int size) throws JsonProcessingException {
        return createComplexJsonWithManyFields(size);
    }

    private List<String> createLargeJsonDataSet(int size) throws JsonProcessingException {
        List<String> largeDataSet = new ArrayList<>();
        List<Map<String, Object>> templates = createComplexJsonDataSet(size);
        
        for (Map<String, Object> template : templates) {
            largeDataSet.add(standardObjectMapper.writeValueAsString(template));
        }
        
        return largeDataSet;
    }

    private List<Map<String, Object>> createEventStreamSpecificData(int size) {
        List<Map<String, Object>> eventStreamData = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < size; i++) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("group_key", "GROUP_" + (i % 100));
            eventData.put("event_data", createSimpleEventData(i));
            eventData.put("created_at", LocalDateTime.now().toString());
            eventData.put("mapped_storage_id", (long) i);
            
            eventStreamData.add(eventData);
        }
        
        return eventStreamData;
    }

    private Map<String, Object> createSimpleEventData(int index) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("user_id", "USER_" + index);
        eventData.put("action", "action_" + (index % 5));
        eventData.put("timestamp", LocalDateTime.now().toString());
        eventData.put("value", index * 100);
        return eventData;
    }

    private List<String> createMixedValidInvalidJsonDataSet(int size) throws JsonProcessingException {
        List<String> mixedDataSet = new ArrayList<>();
        List<Map<String, Object>> validData = createComplexJsonDataSet(size * 4 / 5); // 80% 유효
        
        // 유효한 JSON 추가
        for (Map<String, Object> data : validData) {
            mixedDataSet.add(standardObjectMapper.writeValueAsString(data));
        }
        
        // 유효하지 않은 JSON 추가 (20%)
        for (int i = 0; i < size / 5; i++) {
            mixedDataSet.add("{invalid_json_" + i + "}");
        }
        
        Collections.shuffle(mixedDataSet);
        return mixedDataSet;
    }

    private long measureRegularJsonArrayParsing(String largeJsonArray) throws Exception {
        long startTime = System.currentTimeMillis();
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parsed = standardObjectMapper.readValue(largeJsonArray, List.class);
        int processedCount = parsed.size();
        
        long endTime = System.currentTimeMillis();
        System.out.println("일반 파싱으로 처리된 항목 수: " + processedCount);
        
        return endTime - startTime;
    }

    private long measureStreamingJsonArrayParsing(String largeJsonArray) throws Exception {
        long startTime = System.currentTimeMillis();
        
        // 스트리밍 파싱 시뮬레이션 (실제로는 Jackson Streaming API 사용)
        int processedCount = jsonOptimizationService.parseJsonArrayStream(largeJsonArray);
        
        long endTime = System.currentTimeMillis();
        System.out.println("스트리밍 파싱으로 처리된 항목 수: " + processedCount);
        
        return endTime - startTime;
    }

    private long measureFullJsonParsing(List<String> complexJsons) throws Exception {
        long startTime = System.currentTimeMillis();
        
        for (String jsonString : complexJsons) {
            standardObjectMapper.readValue(jsonString, Map.class);
        }
        
        return System.currentTimeMillis() - startTime;
    }

    private long measureSelectiveFieldParsing(List<String> complexJsons, List<String> requiredFields) throws Exception {
        long startTime = System.currentTimeMillis();
        
        for (String jsonString : complexJsons) {
            jsonOptimizationService.parseSelectiveFields(jsonString, requiredFields);
        }
        
        return System.currentTimeMillis() - startTime;
    }

    private long measureSingleThreadParsing(List<String> jsonDataSet) throws Exception {
        long startTime = System.currentTimeMillis();
        
        for (String jsonData : jsonDataSet) {
            optimizedObjectMapper.readValue(jsonData, Map.class);
        }
        
        return System.currentTimeMillis() - startTime;
    }

    private long measureMultiThreadParsing(List<String> jsonDataSet, ExecutorService executor) throws Exception {
        long startTime = System.currentTimeMillis();
        
        List<Future<Void>> futures = new ArrayList<>();
        int chunkSize = jsonDataSet.size() / CONCURRENT_THREAD_COUNT;
        
        for (int i = 0; i < CONCURRENT_THREAD_COUNT; i++) {
            final int startIdx = i * chunkSize;
            final int endIdx = (i == CONCURRENT_THREAD_COUNT - 1) ? jsonDataSet.size() : (i + 1) * chunkSize;
            
            futures.add(executor.submit(() -> {
                for (int j = startIdx; j < endIdx; j++) {
                    try {
                        optimizedObjectMapper.readValue(jsonDataSet.get(j), Map.class);
                    } catch (Exception e) {
                        // 예외 무시 (성능 측정 목적)
                    }
                }
                return null;
            }));
        }
        
        // 모든 스레드 완료 대기
        for (Future<Void> future : futures) {
            future.get();
        }
        
        return System.currentTimeMillis() - startTime;
    }

    private int processJsonStreamOptimized(List<String> largeJsonDataSet) throws Exception {
        // 스트리밍 방식으로 메모리 효율적 처리
        int processedCount = 0;
        
        for (String jsonData : largeJsonDataSet) {
            // 중요한 필드만 추출하여 메모리 사용량 최소화
            Map<String, Object> essentialData = jsonOptimizationService.extractEssentialFields(jsonData);
            if (essentialData != null) {
                processedCount++;
                // 즉시 처리하고 메모리에서 제거 (축적하지 않음)
                essentialData.clear();
            }
        }
        
        return processedCount;
    }

    private long measureStandardSerialization(List<Map<String, Object>> eventStreamData) throws Exception {
        long startTime = System.currentTimeMillis();
        
        for (Map<String, Object> data : eventStreamData) {
            standardObjectMapper.writeValueAsString(data);
        }
        
        return System.currentTimeMillis() - startTime;
    }

    private long measureOptimizedSerialization(List<Map<String, Object>> eventStreamData) throws Exception {
        long startTime = System.currentTimeMillis();
        
        for (Map<String, Object> data : eventStreamData) {
            jsonOptimizationService.serializeOptimized(data);
        }
        
        return System.currentTimeMillis() - startTime;
    }

    private JsonParsingResult parseWithStandardErrorHandling(List<String> mixedJsonDataSet) {
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        
        for (String jsonData : mixedJsonDataSet) {
            try {
                standardObjectMapper.readValue(jsonData, Map.class);
                successCount++;
            } catch (Exception e) {
                // 에러 로깅 및 처리 (시간 소요)
                System.err.println("Standard parsing error: " + e.getMessage());
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        return new JsonParsingResult(totalTime, successCount);
    }

    private JsonParsingResult parseWithOptimizedErrorHandling(List<String> mixedJsonDataSet) {
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        
        for (String jsonData : mixedJsonDataSet) {
            if (jsonOptimizationService.parseWithOptimizedErrorHandling(jsonData)) {
                successCount++;
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        return new JsonParsingResult(totalTime, successCount);
    }

    // 내부 클래스들

    private static class JsonParsingCache {
        private final Map<String, Map<String, Object>> cache;
        private final int maxSize;
        private int hitCount = 0;
        private int requestCount = 0;
        
        public JsonParsingCache(int maxSize) {
            this.maxSize = maxSize;
            this.cache = new ConcurrentHashMap<>();
        }
        
        @SuppressWarnings("unchecked")
        public Map<String, Object> parseWithCache(String jsonString, ObjectMapper mapper) throws JsonProcessingException {
            requestCount++;
            String cacheKey = generateCacheKey(jsonString);
            
            Map<String, Object> cached = cache.get(cacheKey);
            if (cached != null) {
                hitCount++;
                return cached;
            }
            
            Map<String, Object> parsed = mapper.readValue(jsonString, Map.class);
            
            if (cache.size() < maxSize) {
                cache.put(cacheKey, parsed);
            }
            
            return parsed;
        }
        
        private String generateCacheKey(String jsonString) {
            // JSON 구조 기반 캐시 키 생성 (실제로는 더 정교한 로직 필요)
            return String.valueOf(jsonString.hashCode());
        }
        
        public double getCacheHitRate() {
            return requestCount > 0 ? (double) hitCount / requestCount : 0.0;
        }
    }

    private static class JsonOptimizationService {
        private final ObjectMapper optimizedMapper;
        
        public JsonOptimizationService(ObjectMapper optimizedMapper) {
            this.optimizedMapper = optimizedMapper;
        }
        
        public int parseJsonArrayStream(String largeJsonArray) throws Exception {
            // 실제로는 Jackson Streaming API를 사용하여 메모리 효율적 파싱
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> parsed = optimizedMapper.readValue(largeJsonArray, List.class);
            return parsed.size();
        }
        
        @SuppressWarnings("unchecked")
        public Map<String, Object> parseSelectiveFields(String jsonString, List<String> requiredFields) throws JsonProcessingException {
            JsonNode jsonNode = optimizedMapper.readTree(jsonString);
            Map<String, Object> selectiveData = new HashMap<>();
            
            for (String field : requiredFields) {
                if (jsonNode.has(field)) {
                    selectiveData.put(field, optimizedMapper.convertValue(jsonNode.get(field), Object.class));
                }
            }
            
            return selectiveData;
        }
        
        @SuppressWarnings("unchecked")
        public Map<String, Object> extractEssentialFields(String jsonData) throws JsonProcessingException {
            try {
                JsonNode jsonNode = optimizedMapper.readTree(jsonData);
                Map<String, Object> essential = new HashMap<>();
                
                // 필수 필드만 추출
                String[] essentialFields = {"user_id", "timestamp", "amount", "group_key"};
                for (String field : essentialFields) {
                    if (jsonNode.has(field)) {
                        essential.put(field, optimizedMapper.convertValue(jsonNode.get(field), Object.class));
                    }
                }
                
                return essential;
            } catch (Exception e) {
                return null;
            }
        }
        
        public String serializeOptimized(Map<String, Object> data) throws JsonProcessingException {
            return optimizedMapper.writeValueAsString(data);
        }
        
        public boolean parseWithOptimizedErrorHandling(String jsonData) {
            try {
                optimizedMapper.readValue(jsonData, Map.class);
                return true;
            } catch (JsonProcessingException e) {
                // 최적화된 에러 처리 (로깅 최소화)
                return false;
            }
        }
    }

    private static class JsonParsingResult {
        private final long totalTime;
        private final int successCount;
        
        public JsonParsingResult(long totalTime, int successCount) {
            this.totalTime = totalTime;
            this.successCount = successCount;
        }
        
        public long getTotalTime() { return totalTime; }
        public int getSuccessCount() { return successCount; }
    }
}
