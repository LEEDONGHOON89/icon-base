package com.itmasters.icon.api.config;

import com.itmasters.icon.common.domain.EntityType;
import com.itmasters.icon.common.domain.IdGenerator;
import com.itmasters.icon.api.user.application.port.out.UserRepository;
import com.itmasters.icon.api.user.adapter.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import static com.itmasters.icon.api.config.AppConst.SYSTEM_USER;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;

    @Override
    public void run(String... args) throws Exception {
        // SYSTEM 계정 확인 및 생성
        userRepository.findById(SYSTEM_USER).ifPresentOrElse(
                user -> System.out.println("SYSTEM user already exists."),
                () -> {
                    UserEntity systemUser = UserEntity.of(
                            SYSTEM_USER, // loginId
                            "시스템 관리자", // userName
                            "system@itmasters.com" // email
                    );
                    systemUser.assignId(SYSTEM_USER);
                    systemUser.updatePassword(passwordEncoder.encode("system_admin"));
                    userRepository.save(systemUser);
                    System.out.println("SYSTEM user created.");
                }
        );

        // ADMIN 계정 확인 생성
        userRepository.findByLoginId("admin").ifPresentOrElse(
                user -> System.out.println("ADMIN user already exists."),
                () -> {
                    UserEntity adminUser = UserEntity.of(
                            "admin", // loginId
                            "관리자", // userName
                            "admin@itmasters.com" // email
                    );
                    adminUser.assignId(idGenerator.generateId(EntityType.USER));
                    adminUser.updatePassword(passwordEncoder.encode("qwer1234!"));
                    userRepository.save(adminUser);
                    System.out.println("ADMIN user created.");
                }
        );
    }
}
