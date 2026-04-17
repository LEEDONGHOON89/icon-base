package com.itmasters.icon.api.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.itmasters.icon.api.config.AppConst.SYSTEM_USER;

@Component
public class AuditorAwareImpl implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            // 시스템 사용자의 경우 고정 ID 사용
            return Optional.of(SYSTEM_USER);
        }

        try {
            // principal이 ID 문자열인 경우
            String principal = authentication.getPrincipal().toString();
            return Optional.of(principal);
        } catch (IllegalArgumentException e) {
            // 시스템 사용자 ID 반환
            return Optional.of(SYSTEM_USER);
        }
    }
}
