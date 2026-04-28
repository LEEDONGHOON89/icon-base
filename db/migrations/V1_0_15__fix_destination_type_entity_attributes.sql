-- [2026-04-24] profiles 테이블의 잘못된 destination_type 값 수정
-- ENTITY_ATTRIBUTES 는 존재하지 않는 enum 값 (실제 enum: EVENT_STREAM, ENTITY, BOTH)
-- 의미상 "event_stream + entity_attributes 양쪽 저장" = BOTH 로 변환
UPDATE profiles
SET destination_type = 'BOTH'
WHERE destination_type = 'ENTITY_ATTRIBUTES';
