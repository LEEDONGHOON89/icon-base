package com.itmasters.icon.api.auth.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 사용자 정보 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 정보 응답")
public class UserInfoResponse {
    
    @Schema(description = "사용자 ID", example = "01HZ9ABCD1234")
    private String userId;
    
    @Schema(description = "로그인 ID", example = "admin")
    private String loginId;
    
    @Schema(description = "사용자 이름", example = "관리자")
    private String name;
    @Schema(description = "사용자 역할", example = "ADMIN")
    private String role;
}
