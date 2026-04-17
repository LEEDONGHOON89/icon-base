package com.itmasters.icon.engine.processor.support;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectSensorEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineEventStreamEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EventStreamGroupEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.SensorEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.DetectSensorRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EventStreamGroupRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.SensorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 센서 탐지 결과 저장을 담당하는 컴포넌트.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensorDetectionRecorder {

    private final DetectSensorRepository detectSensorRepository;
    private final SensorRepository sensorRepository;
    private final EventStreamGroupRepository eventStreamGroupRepository;

    /**
     * sensorId가 실제 센서일 때 detect_sensors 테이블에 저장한다.
     */
    public void saveIfSensor(String sensorId, List<EngineEventStreamEntity> events) {
        if (sensorId == null || sensorId.isBlank()) {
            return;
        }
        if (events == null || events.isEmpty()) {
            log.debug("[SENSOR-SAVE] No events to save for sensor: {}", sensorId);
            return;
        }

        SensorEntity sensor = sensorRepository.findBySensorId(sensorId).orElse(null);
        if (sensor == null) {
            log.debug("[SENSOR-SAVE] {} is not a sensor, skipping detect_sensors save", sensorId);
            return;
        }

        int saved = save(sensorId, events);
        log.info("[SENSOR-SAVE] Saved {} detect_sensors for sensor: {}", saved, sensorId);
    }

    /**
     * detect_sensors 테이블에 저장하고 실제 저장 건수를 반환한다.
     */
    public int save(String sensorId, List<EngineEventStreamEntity> events) {
        if (sensorId == null || sensorId.isBlank() || events == null || events.isEmpty()) {
            return 0;
        }

        List<DetectSensorEntity> detectSensors = new ArrayList<>();
        for (EngineEventStreamEntity event : events) {
            String groupKey = resolveGroupKey(sensorId, event);

            boolean isDuplicate = detectSensorRepository.existsBySensorIdAndMappedStorageId(
                    sensorId, event.getMappedDataStorageId());
            if (isDuplicate) {
                log.debug("[SENSOR-SAVE] Skip duplicate detect_sensor: sensorId={}, mappedStorageId={}",
                        sensorId, event.getMappedDataStorageId());
                continue;
            }

            DetectSensorEntity entity = DetectSensorEntity.builder()
                    .sensorId(sensorId)
                    .groupKey(groupKey)
                    .mappedStorageId(event.getMappedDataStorageId())
                    .transactionId(event.getTransactionId())
                    .detectedDt(LocalDateTime.now())
                    .eventDt(event.getEventDt())
                    .build();
            detectSensors.add(entity);
        }

        if (detectSensors.isEmpty()) {
            return 0;
        }

        try {
            detectSensorRepository.saveAll(detectSensors);
            log.info("[SENSOR-SAVE] detect_sensors 저장 완료 - sensorId={}, count={}", sensorId, detectSensors.size());
            return detectSensors.size();
        } catch (Exception ex) {
            log.error("[SENSOR-SAVE] Failed to save detect_sensors for sensor {}: {}", sensorId, ex.getMessage(), ex);
            return 0;
        }
    }

    private String resolveGroupKey(String sensorId, EngineEventStreamEntity event) {
        try {
            List<EventStreamGroupEntity> groups = eventStreamGroupRepository
                    .findByEventStreamId(event.getEventStreamId());

            if (groups == null || groups.isEmpty()) {
                log.warn("[SENSOR-SAVE] No event_stream_groups found for event_stream_id={}", event.getEventStreamId());
                return null;
            }

            for (EventStreamGroupEntity group : groups) {
                if (sensorId.equals(group.getRuleId())) {
                    return group.getGroupKey();
                }
            }

            // sensorId로 매칭되는 그룹이 없으면 첫 번째 그룹 사용
            log.warn("[SENSOR-SAVE] No group matched sensorId={}, fallback to first group.", sensorId);
            return groups.get(0).getGroupKey();

        } catch (Exception ex) {
            log.error("[SENSOR-SAVE] Failed to resolve groupKey for event_stream_id={}: {}",
                    event.getEventStreamId(), ex.getMessage(), ex);
            return null;
        }
    }
}
