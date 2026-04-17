package com.itmasters.icon.api.rule.adapter.in.web;

import com.itmasters.icon.api.common.response.ResponseList;
import com.itmasters.icon.api.rule.dto.SensorDto;
import com.itmasters.icon.api.rule.application.service.SensorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Sensor 관리 API", description = "센서 목록/단건 조회, 등록, 수정 API")
@Validated
public class SensorController {

    private final SensorService sensorService;
    
    @GetMapping("/api/v1/sensors")
    @Operation(summary = "센서 목록 조회")
    public ResponseList<SensorDto.Response> getSensors(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false, defaultValue = "name") String sort,
            @RequestParam(required = false, defaultValue = "asc") String dir,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        if (page != null || size != null || q != null || active != null) {
            int p = page == null ? 1 : page;
            int s = size == null ? 20 : size;
            return sensorService.findPaged(q, active, sort, dir, p, s);
        }
        List<SensorDto.Response> responses = sensorService.findAll().stream()
                .map(SensorDto.Response::from)
                .collect(Collectors.toList());
        return new ResponseList<>(responses);
    }

    @GetMapping("/api/v1/sensors/{id}")
    @Operation(summary = "단일 센서 조회")
    public SensorDto.Response getSensor(@PathVariable String id) {
        SensorDto.Info result = sensorService.findById(id);
        return SensorDto.Response.from(result);
    }

    @GetMapping("/api/v1/sensors/active")
    @Operation(summary = "활성화된 센서 조회")
    public ResponseList<SensorDto.Response> getActiveSensors() {
        List<SensorDto.Response> responses = sensorService.findActiveSensors().stream()
                .map(SensorDto.Response::from)
                .collect(Collectors.toList());
        return new ResponseList<>(responses);
    }

    @PostMapping("/api/v1/sensors")
    @Operation(summary = "신규 센서 등록")
    public SensorDto.Response createSensor(@Valid @RequestBody SensorDto.CreateRequest request) {
        SensorDto.CreateCommand command = request.toCommand();

        SensorDto.Info result = sensorService.createSensor(command);
        return SensorDto.Response.from(result);
    }

    @PutMapping("/api/v1/sensors/{id}")
    @Operation(summary = "기존 센서 정보 수정")
    public SensorDto.Response updateSensor(
            Principal principal,
            @PathVariable String id, @Valid @RequestBody SensorDto.UpdateRequest request) {
        SensorDto.UpdateCommand command = request.toCommand(id); // version은 서비스에서 처리
        SensorDto.Info result = sensorService.updateSensor(command, principal.getName());
        return SensorDto.Response.from(result);
    }
}
