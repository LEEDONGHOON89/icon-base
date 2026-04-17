package com.itmasters.icon.api.config;

import com.itmasters.icon.api.auth.application.security.JwtAuthenticationFilter;
import com.itmasters.icon.api.auth.application.util.JwtAuthenticationEntryPoint;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

  public static final String[] PUBLIC_URLS = {
      "/api/v1/auth/login",
      "/api/v1/auth/refresh",
      "/api/v1/auth/logout",
      // 개발 편의: 분석/엔진 트리거/단일 인입 엔드포인트 무인증 허용
      "/api/v1/analytics/**",
      "/api/v1/engine/steps/**",
      "/api/v1/engine/ingest/**",
      "/api/v1/engine/flow", // 엔진 플로우 조회 (개발용)
      "/api/v1/detections/realtime/**", // 실시간 탐지 API (개발 편의)
      "/api/v1/entity-attributes/**", // 개발 편의: entity attributes 무인증 허용
      "/api/v1/entity-fields/**", // 개발 편의: entity fields 무인증 허용
      // 여기에 추가적인 인증 없이 접근 가능한 URL을 추가합니다.
      // "/api/v1/public/**",
      "/swagger-ui/**",
      "/v3/api-docs/**",
      "/rpc/**" // Agent WebSocket
  };

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorize -> authorize.requestMatchers(PUBLIC_URLS).permitAll().anyRequest().authenticated())
        .exceptionHandling(
            exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint));

    http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    // [2026-04-17] allowedOrigins → allowedOriginPatterns: credentials 사용 시 와일드카드 허용
    // localhost뿐 아니라 VM/개발서버 IP에서 접속해도 동작하도록 변경
    configuration.setAllowedOriginPatterns(List.of("*"));
    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")); // 허용할 HTTP 메서드
    configuration.setAllowedHeaders(List.of("*")); // 모든 헤더 허용
    configuration.setAllowCredentials(true); // 자격 증명 허용

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration); // 모든 경로에 CORS 설정 적용
    return source;
  }
}
