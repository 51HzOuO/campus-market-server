package com.example.campusmarketserver.controller;

import com.example.campusmarketserver.entity.User;
import com.example.campusmarketserver.service.CommentService;
import com.example.campusmarketserver.service.LikeService;
import com.example.campusmarketserver.service.PostService;
import com.example.campusmarketserver.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class UserControllerMvcTest {
    private UserService users;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        users = mock(UserService.class);
        UserController controller = new UserController(users, mock(PostService.class),
                mock(LikeService.class), mock(CommentService.class));
        mvc = standaloneSetup(controller).build();
    }

    @Test
    void becomeAdminRouteIsUnmappedAndDoesNotUpdateAnyRole() throws Exception {
        mvc.perform(post("/user/become-admin")).andExpect(status().isNotFound());

        verify(users, never()).updateById(any(User.class));
    }
}
