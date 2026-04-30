package com.itmasters.icon.api.datasource.adapter.in.web;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * [2026-04-22] JSONB 확장 필터 파라미터 — TODO-001/002 공통
 * 형식: key:op:value (예: cust_no:eq:12345678, account_id:like:ACC%)
 * 보안: key 는 ^[a-zA-Z0-9_]+$ 패턴만 허용 — SQL Injection 방지
 */
public record JsonFilterParam(String key, Op op, String value) {

    private static final Pattern VALID_KEY = Pattern.compile("^[a-zA-Z0-9_]+$");

    public enum Op {
        eq, like, neq
    }

    /** raw "key:op:value" 문자열을 파싱해 유효한 경우에만 Optional 반환 */
    public static Optional<JsonFilterParam> parse(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String[] parts = raw.split(":", 3);
        if (parts.length != 3) return Optional.empty();
        String key = parts[0].trim();
        if (!VALID_KEY.matcher(key).matches()) return Optional.empty();
        try {
            Op op = Op.valueOf(parts[1].trim().toLowerCase());
            return Optional.of(new JsonFilterParam(key, op, parts[2]));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** 요청 파라미터 리스트를 파싱하여 유효한 필터만 반환 */
    public static List<JsonFilterParam> parseAll(List<String> raws) {
        if (raws == null) return List.of();
        return raws.stream()
                .map(JsonFilterParam::parse)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}
