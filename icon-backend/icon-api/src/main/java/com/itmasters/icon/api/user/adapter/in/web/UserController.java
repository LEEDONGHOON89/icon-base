package com.itmasters.icon.api.user.adapter.in.web;

import com.itmasters.icon.api.common.response.ResponseList;
import com.itmasters.icon.api.user.adapter.in.web.dto.UserCreateRequest;
import com.itmasters.icon.api.user.adapter.in.web.dto.UserResponse;
import com.itmasters.icon.api.user.adapter.in.web.dto.UserUpdatePassword;
import com.itmasters.icon.api.user.adapter.in.web.dto.UserUpdateRequest;
import com.itmasters.icon.api.user.application.UserService;
import com.itmasters.icon.api.user.application.result.UserResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "User 관리 API", description = "사용자 목록/단건 조회, 등록, 수정 API")
@Validated
public class UserController {
    private final UserService userService;

    @GetMapping("/api/v1/users")
    @Operation(summary = "사용자 목록 조회")
    public ResponseList<UserResponse> getUsers() {
        List<UserResponse> responses = userService.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
        return new ResponseList<>(responses);
    }

    @GetMapping("/api/v1/users/{id}")
    @Operation(summary = "단일 사용자 조회")
    public UserResponse getUser(@PathVariable String id) {
        UserResult result = userService.findById(id);
        return UserResponse.from(result);
    }

    @PostMapping("/api/v1/users")
    @Operation(summary = "신규 사용자 등록")
    public UserResponse createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResult result = userService.createUser(request.toCommand());
        return UserResponse.from(result);
    }

    @PutMapping("/api/v1/users/{id}")
    @Operation(summary = "기존 사용자 정보 수정")
    public UserResponse updateUser(
            @PathVariable String id, @Valid @RequestBody UserUpdateRequest request) {
        UserResult result = userService.updateUser(request.toCommand(id));
        return UserResponse.from(result);
    }

    @PutMapping("/api/v1/users/{id}/password")
    public UserResponse updatePassword(
            @PathVariable String id, @Valid @RequestBody UserUpdatePassword request) {
        UserResult result = userService.updatePassword(request.toCommand(id));
        return UserResponse.from(result);
    }
}
