package com.itmasters.icon.api.user.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.api.user.adapter.in.web.dto.UserCreateRequest;
import com.itmasters.icon.api.user.adapter.in.web.dto.UserUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserControllerTest {
  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;

  @Test
  @DisplayName("POST /api/v1/users - 유저 생성")
  void createUser() throws Exception {
    UserCreateRequest req = new UserCreateRequest();
    req.setLoginId("testuser");
    req.setPassword("password123");
    req.setUserName("테스트유저");
    req.setEmail("test@example.com");
    req.setDescription("설명");
    mockMvc
        .perform(
            post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loginId").value("testuser"));
  }

  @Test
  @DisplayName("GET /api/v1/users - 유저 목록 조회")
  void getUsers() throws Exception {
    mockMvc
        .perform(get("/api/v1/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray());
  }

  @Test
  @DisplayName("GET /api/v1/users/{id} - 단일 유저 조회")
  void getUserById() throws Exception {
    // 우선 유저를 생성
    UserCreateRequest req = new UserCreateRequest();
    req.setLoginId("finduser");
    req.setPassword("password123");
    req.setUserName("조회유저");
    req.setEmail("find@example.com");
    req.setDescription("설명");
    String content = objectMapper.writeValueAsString(req);
    String response =
        mockMvc
            .perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(content))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String userId = objectMapper.readTree(response).get("userId").asText();
    // 단일 조회
    mockMvc
        .perform(get("/api/v1/users/" + userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loginId").value("finduser"));
  }

  @Test
  @DisplayName("PUT /api/v1/users/{id} - 유저 정보 수정")
  void updateUser() throws Exception {
    // 유저 생성
    UserCreateRequest req = new UserCreateRequest();
    req.setLoginId("updateuser");
    req.setPassword("password123");
    req.setUserName("수정유저");
    req.setEmail("update@example.com");
    req.setDescription("설명");
    String content = objectMapper.writeValueAsString(req);
    String response =
        mockMvc
            .perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON).content(content))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String userId = objectMapper.readTree(response).get("userId").asText();
    // 수정 요청
    UserUpdateRequest updateReq = new UserUpdateRequest();
    updateReq.setUserName("수정됨");
    updateReq.setEmail("updated@example.com");
    updateReq.setDescription("수정설명");
    mockMvc
        .perform(
            put("/api/v1/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userName").value("수정됨"))
        .andExpect(jsonPath("$.email").value("updated@example.com"));
  }

  @Test
  @DisplayName("GET /api/v1/users/{id} - 없는 유저 조회시 404 반환")
  void getUserNotFound() throws Exception {
    mockMvc.perform(get("/api/v1/users/not-exist-id")).andExpect(status().is4xxClientError());
  }
}
