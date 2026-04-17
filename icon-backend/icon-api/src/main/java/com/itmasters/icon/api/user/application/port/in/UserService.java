package com.itmasters.icon.api.user.application.port.in;

import com.itmasters.icon.api.user.application.command.UserCreateCommand;
import com.itmasters.icon.api.user.application.command.UserUpdateCommand;
import com.itmasters.icon.api.user.application.command.UserPasswordUpdateCommand;
import com.itmasters.icon.api.user.application.result.UserResult;

import java.util.List;

/**
 * 사용자 관련 비즈니스 로직 인터페이스
 */
public interface UserService {
    
    /**
     * 사용자 ID로 사용자 상세 정보 조회
     */
    UserResult getUserById(String userId);
    
    /**
     * 현재 로그인한 사용자 정보 조회
     */
    UserResult getCurrentUser(String userId);

    /**
     * 사용자 ID로 사용자 상세 정보 조회
     */
    UserResult findById(String userId);

    /**
     * 전체 사용자 목록 조회
     */
    List<UserResult> findAll();

    /**
     * 새로운 사용자 생성
     */
    UserResult createUser(UserCreateCommand command);

    /**
     * 사용자 정보 업데이트
     */
    UserResult updateUser(UserUpdateCommand command);

    /**
     * 비밀번호 업데이트
     */
    UserResult updatePassword(UserPasswordUpdateCommand command);
}