package com.itmasters.icon.api.auth.application.security;

import com.itmasters.icon.api.auth.application.util.JwtTokenProvider;
import com.itmasters.icon.api.config.SecurityConfig;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SecurityException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider tokenProvider;
  private final UserDetailsService userDetailsService; // Spring Security의 UserDetailsService

  private static final AntPathMatcher pathMatcher = new AntPathMatcher();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestURI = request.getRequestURI();

    // PUBLIC_URLS는 필터 통과
    for (String pattern : SecurityConfig.PUBLIC_URLS) {
      if (pathMatcher.match(pattern, requestURI)) {
        filterChain.doFilter(request, response);
        return;
      }
    }

    String jwt = getJwtFromRequest(request);

    if (!StringUtils.hasText(jwt)) {
      request.setAttribute("jwtExceptionMessage", "JWT 토큰이 필요합니다.");
      request.setAttribute("jwtExceptionCode", "JWT_REQUIRED");
      throw new AuthenticationException("인증된 토큰이 필요합니다.") {};
    }

    if (StringUtils.hasText(jwt)) {
      try {
        if (tokenProvider.validateToken(jwt)) {
          String userId = tokenProvider.getLoginIdFromToken(jwt);

          // UserDetailsService를 통해 UserDetails 객체 로드
          UserDetails userDetails = userDetailsService.loadUserByUsername(userId);
          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(
                  userDetails.getUsername(), null, userDetails.getAuthorities());
          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

          SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
          request.setAttribute("jwtExceptionMessage", "유효하지 않은 JWT 토큰입니다.");
          request.setAttribute("jwtExceptionCode", "JWT_INVALID");
          throw new org.springframework.security.core.AuthenticationException(
              "유효하지 않은 JWT 토큰입니다.") {};
        }
      } catch (ExpiredJwtException e) {
        request.setAttribute("jwtExceptionMessage", "만료된 JWT 토큰입니다.");
        request.setAttribute("jwtExceptionCode", "JWT_EXPIRED");
        throw new org.springframework.security.core.AuthenticationException("만료된 JWT 토큰입니다.") {};
      } catch (MalformedJwtException | SecurityException e) {
        request.setAttribute("jwtExceptionMessage", "잘못된 JWT 서명입니다.");
        request.setAttribute("jwtExceptionCode", "JWT_INVALID");
        throw new org.springframework.security.core.AuthenticationException("잘못된 JWT 서명입니다.") {};
      } catch (UnsupportedJwtException e) {
        request.setAttribute("jwtExceptionMessage", "지원되지 않는 JWT 토큰입니다.");
        request.setAttribute("jwtExceptionCode", "JWT_UNSUPPORTED");
        throw new org.springframework.security.core.AuthenticationException(
            "지원되지 않는 JWT 토큰입니다.") {};
      } catch (IllegalArgumentException e) {
        request.setAttribute("jwtExceptionMessage", "JWT 토큰이 잘못되었습니다.");
        request.setAttribute("jwtExceptionCode", "JWT_ILLEGAL");
        throw new org.springframework.security.core.AuthenticationException("JWT 토큰이 잘못되었습니다.") {};
      } catch (Exception e) {
        throw new org.springframework.security.core.AuthenticationException(e.getMessage()) {};
      }
    }
    //    } catch (org.springframework.security.core.AuthenticationException ex) {
    //      // EntryPoint로 위임
    //      SecurityContextHolder.clearContext();
    //      // 바로 EntryPoint로 위임
    //      request.setAttribute("SPRING_SECURITY_LAST_EXCEPTION", ex);
    //      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    //      // EntryPoint가 JSON 응답을 내려주도록 filterChain을 종료
    //      return;
    //    } catch (Exception ex) {
    //      logger.error("Could not set user authentication in security context", ex);
    //    }

    filterChain.doFilter(request, response);
  }

  private String getJwtFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }
}
