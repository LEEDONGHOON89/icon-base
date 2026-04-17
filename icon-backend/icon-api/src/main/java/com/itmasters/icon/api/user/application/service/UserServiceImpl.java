package com.itmasters.icon.api.user.application.service;

import com.itmasters.icon.api.user.application.port.in.UserService;
import com.itmasters.icon.api.user.application.port.out.UserRepository;
import com.itmasters.icon.api.user.application.result.UserResult;
import com.itmasters.icon.api.user.adapter.persistence.entity.UserEntity;
import com.itmasters.icon.api.user.application.command.UserCreateCommand;
import com.itmasters.icon.api.user.application.command.UserUpdateCommand;
import com.itmasters.icon.api.user.application.command.UserPasswordUpdateCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 사용자 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_ROLE = "ADMIN";

    @Override
    public UserResult getUserById(String userId) {
        return getCurrentUser(userId);
    }

    @Override
    public UserResult getCurrentUser(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다. ID: " + userId));

        return new UserResult(
                user.getUserId(),
                user.getLoginId(),
                user.getUserName(),
                user.getEmail(),
                user.getIsActive(),
                DEFAULT_ROLE
        );
    }

    @Override
    public UserResult findById(String userId) {
        return getUserById(userId);
    }

    @Override
    public List<UserResult> findAll() {
        List<UserEntity> users = userRepository.findAll();
        return users.stream()
                .map(user -> new UserResult(
                        user.getUserId(),
                        user.getLoginId(),
                        user.getUserName(),
                        user.getEmail(),
                        user.getIsActive(),
                        DEFAULT_ROLE
                ))
                .toList();
    }

    @Override
    @Transactional
    public UserResult createUser(UserCreateCommand command) {
        // 이메일 중복 검사
        if (userRepository.existsByEmail(command.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 존재하는 이메일입니다: " + command.getEmail());
        }

        // 새 사용자 생성
        UserEntity user = UserEntity.of(
                command.getLoginId(),
                command.getUserName(),
                command.getEmail()
        );
        
        // 비밀번호 암호화
        user.updatePassword(passwordEncoder.encode(command.getPassword()));
        
        UserEntity savedUser = userRepository.save(user);
        
        return new UserResult(
                savedUser.getUserId(),
                savedUser.getLoginId(),
                savedUser.getUserName(),
                savedUser.getEmail(),
                savedUser.getIsActive(),
                DEFAULT_ROLE
        );
    }

    @Override
    @Transactional
    public UserResult updateUser(UserUpdateCommand command) {
        UserEntity user = userRepository.findById(command.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다. ID: " + command.getUserId()));

        user.updateUserInfo(command.getUserName(), command.getEmail());
        UserEntity updatedUser = userRepository.save(user);
        
        return new UserResult(
                updatedUser.getUserId(),
                updatedUser.getLoginId(),
                updatedUser.getUserName(),
                updatedUser.getEmail(),
                updatedUser.getIsActive(),
                DEFAULT_ROLE
        );
    }

    @Override
    @Transactional
    public UserResult updatePassword(UserPasswordUpdateCommand command) {
        UserEntity user = userRepository.findById(command.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + command.getUserId()));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(command.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호 설정
        user.updatePassword(passwordEncoder.encode(command.getNewPassword()));
        UserEntity updatedUser = userRepository.save(user);
        
        return new UserResult(
                updatedUser.getUserId(),
                updatedUser.getLoginId(),
                updatedUser.getUserName(),
                updatedUser.getEmail(),
                updatedUser.getIsActive(),
                DEFAULT_ROLE
        );
    }
}
