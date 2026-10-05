package com.example.campusmarketserver.controller;

import com.example.campusmarketserver.service.CommentService;
import com.example.campusmarketserver.service.LikeService;
import com.example.campusmarketserver.service.PostService;
import com.example.campusmarketserver.service.UserService;
import com.example.campusmarketserver.context.UserContext;
import com.example.campusmarketserver.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerSecurityTest {
    private MockMvc mvc;
    private UserService users;
    private UserController controller;

    @BeforeEach
    void setUp() {
        users = mock(UserService.class);
        controller = new UserController(users, mock(PostService.class), mock(LikeService.class), mock(CommentService.class));
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void formerSelfPromotionEndpointIsNotMapped() throws Exception {
        mvc.perform(post("/user/become-admin")).andExpect(status().isNotFound());
    }

    @Test
    void roleManagementCannotGrantAdministratorRole() {
        User admin = new User();
        admin.setRole(1);
        when(users.getById(10L)).thenReturn(admin);
        UserContext.setUserId(10L);

        assertThat(controller.adminUpdateRole(Map.of("userId", 20, "role", 1)).getCode()).isEqualTo(400);

        verify(users).getById(10L);
        verifyNoMoreInteractions(users);
    }

    @Test
    void nonAdministratorCannotChangeRoles() {
        User ordinaryUser = new User();
        ordinaryUser.setRole(0);
        when(users.getById(10L)).thenReturn(ordinaryUser);
        UserContext.setUserId(10L);

        assertThat(controller.adminUpdateRole(Map.of("userId", 20, "role", 2)).getCode()).isEqualTo(403);

        verify(users).getById(10L);
        verifyNoMoreInteractions(users);
    }
}
