package com.itmasters.icon.api.user.application;

import com.itmasters.icon.api.common.exception.NoDataException;
import com.itmasters.icon.api.user.application.command.CreateUserCommand;
import com.itmasters.icon.api.user.application.command.UpdateUserCommand;
import com.itmasters.icon.api.user.application.command.UpdateUserPwdCommand;
import com.itmasters.icon.api.user.application.result.UserResult;
import com.itmasters.icon.api.user.application.port.out.UserRepository;
import com.itmasters.icon.api.user.adapter.persistence.entity.UserEntity;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 전체 사용자 목록 조회
    public List<UserResult> findAll() {
        return userRepository.findAll().stream()
                .map(UserResult::from)
                .collect(Collectors.toList());
    }

    // 단일 사용자 조회
    public UserResult findById(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoDataException("사용자 없음"));
        return UserResult.from(user);
    }

    // 신규 사용자 등록
    @Transactional
    public UserResult createUser(CreateUserCommand command) {
        UserEntity user = UserEntity.of(
                        command.getLoginId(),
                        command.getUserName(),
                        command.getEmail());
        user.updatePassword(command.getPassword());
        userRepository.save(user);
        return UserResult.from(user);
    }

    // 기존 사용자 정보 수정
    @Transactional
    public UserResult updateUser(UpdateUserCommand command) {
        UserEntity user = userRepository.findById(command.getUserId())
                .orElseThrow(() -> new NoDataException("사용자 없음"));
        user.updateUserInfo(command.getUserName(), command.getEmail());
        userRepository.save(user);
        return UserResult.from(user);
    }

    @Transactional
    public UserResult updatePassword(UpdateUserPwdCommand command) {
        UserEntity user = userRepository.findById(command.getUserId())
                .orElseThrow(() -> new NoDataException("사용자 없음"));

        user.updatePassword(passwordEncoder.encode((command.getPassword())));
        userRepository.save(user);
        return UserResult.from(user);
    }
}
