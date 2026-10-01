package com.example.campusmarketserver.controller;

import com.example.campusmarketserver.common.Result;
import com.example.campusmarketserver.entity.User;
import com.example.campusmarketserver.service.UserService;
import com.example.campusmarketserver.service.WechatLoginClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerTest {
    private UserService users;
    private WechatLoginClient wechat;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        users = mock(UserService.class);
        wechat = mock(WechatLoginClient.class);
        controller = new AuthController(users, wechat);
    }

    @Test
    void missingLoginCodeDoesNotCallWechatOrDatabase() {
        assertThat(controller.login(Map.of()).getCode()).isEqualTo(400);
        assertThat(controller.login(Map.of("code", " ")).getCode()).isEqualTo(400);
        assertThat(controller.login(null).getCode()).isEqualTo(400);
        verifyNoInteractions(users, wechat);
    }

    @Test
    void passesSafeWechatErrorToClientWithoutCreatingUser() {
        when(wechat.getOpenid("fresh-code")).thenThrow(new WechatLoginClient.LoginException(503, "微信登录配置不匹配"));
        Result<Map<String, Object>> response = controller.login(Map.of("code", "fresh-code"));
        assertThat(response.getCode()).isEqualTo(503);
        assertThat(response.getMessage()).isEqualTo("微信登录配置不匹配");
        verifyNoInteractions(users);
    }

    @Test
    void registersNormalUserAndReturnsPersistedTokenAndRole() {
        when(wechat.getOpenid("fresh-code")).thenReturn("new-openid");
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            ((User) invocation.getArgument(0)).setId(7L);
            return true;
        });
        when(users.updateById(any(User.class))).thenReturn(true);
        Result<Map<String, Object>> response = controller.login(Map.of("code", "fresh-code"));
        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).containsEntry("userId", 7L).containsEntry("status", 0).containsEntry("role", 0);
        assertThat(response.getData()).doesNotContainKeys("openid", "session_key");
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isZero();
        assertThat(response.getData().get("token")).isEqualTo(saved.getValue().getToken());
        assertThat(saved.getValue().getToken()).hasSize(32);
    }

    @Test
    void returnsExistingReviewerRoleAndRefreshesToken() {
        User user = existingUser(0);
        user.setRole(2);
        when(wechat.getOpenid("fresh-code")).thenReturn("existing-openid");
        when(users.getByOpenid("existing-openid")).thenReturn(user);
        when(users.updateById(user)).thenReturn(true);
        Result<Map<String, Object>> response = controller.login(Map.of("code", "fresh-code"));
        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getData()).containsEntry("role", 2);
        assertThat(response.getData().get("token")).isNotEqualTo("old-token");
        verify(users, never()).save(any(User.class));
    }

    @Test
    void bannedUserCannotReceiveANewTokenAndIsNotAutomaticallyUnbanned() {
        User user = existingUser(1);
        when(wechat.getOpenid("fresh-code")).thenReturn("existing-openid");
        when(users.getByOpenid("existing-openid")).thenReturn(user);
        Result<Map<String, Object>> response = controller.login(Map.of("code", "fresh-code"));
        assertThat(response.getCode()).isEqualTo(403);
        assertThat(response.getData()).isNull();
        assertThat(user.getStatus()).isEqualTo(1);
        assertThat(user.getToken()).isEqualTo("old-token");
        verify(users, never()).updateById(any(User.class));
    }

    @Test
    void failedRegistrationDoesNotReturnSuccessOrSaveToken() {
        when(wechat.getOpenid("fresh-code")).thenReturn("new-openid");
        when(users.save(any(User.class))).thenReturn(false);
        assertThat(controller.login(Map.of("code", "fresh-code")).getCode()).isEqualTo(503);
        verify(users, never()).updateById(any(User.class));
    }

    @Test
    void failedTokenWriteDoesNotReturnAnUnusableToken() {
        User user = existingUser(0);
        when(wechat.getOpenid("fresh-code")).thenReturn("existing-openid");
        when(users.getByOpenid("existing-openid")).thenReturn(user);
        when(users.updateById(user)).thenReturn(false);
        Result<Map<String, Object>> response = controller.login(Map.of("code", "fresh-code"));
        assertThat(response.getCode()).isEqualTo(503);
        assertThat(response.getData()).isNull();
    }

    private User existingUser(int status) {
        User user = new User();
        user.setId(7L);
        user.setStatus(status);
        user.setToken("old-token");
        return user;
    }
}
