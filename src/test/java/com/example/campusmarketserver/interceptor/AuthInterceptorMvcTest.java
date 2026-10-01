package com.example.campusmarketserver.interceptor;

import com.example.campusmarketserver.config.WebConfig;
import com.example.campusmarketserver.context.UserContext;
import com.example.campusmarketserver.entity.User;
import com.example.campusmarketserver.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Loads only MVC, the real interceptor registration and stub endpoints; no database or network. */
class AuthInterceptorMvcTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;
    private UserService users;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfig.class);
        context.refresh();
        users = context.getBean(UserService.class);
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
        context.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/post/list", "/post/detail/1", "/post/hot", "/post/search", "/comment/list",
            "/errand/list", "/errand/detail/1", "/second-hand/list", "/second-hand/detail/1",
            "/club/list", "/club/detail/1", "/club/activity/list", "/club/activity/detail/1"})
    void publicReadsAllowAnonymousAndResolveValidTokens(String path) throws Exception {
        UserContext.setUserId(999L);
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(content().string("anonymous"));
        verifyNoInteractions(users);
        when(users.getByToken("valid-token")).thenReturn(normalUser());
        mvc.perform(get(path).header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk()).andExpect(content().string("user:7"));
        assertThat(UserContext.getUserId()).isNull();
    }

    @Test
    void expiredTokenOnPublicRouteReturns401SoClientCanRenewLogin() throws Exception {
        mvc.perform(get("/club/detail/1").header("Authorization", "expired-token"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        assertThat(UserContext.getUserId()).isNull();
    }

    @Test
    void mineScopeRequiresAuthentication() throws Exception {
        mvc.perform(get("/errand/list").param("scope", "mine"))
                .andExpect(status().isUnauthorized());
        when(users.getByToken("valid-token")).thenReturn(normalUser());
        mvc.perform(get("/errand/list").param("scope", "mine").header("Authorization", "valid-token"))
                .andExpect(status().isOk()).andExpect(content().string("user:7"));
    }

    @Test
    void protectedWritesStillRequireLoginAndPublicPathsDoNotBypassPostAuthentication() throws Exception {
        mvc.perform(post("/post/create")).andExpect(status().isUnauthorized());
        mvc.perform(post("/post/list")).andExpect(status().isUnauthorized());
        when(users.getByToken("valid-token")).thenReturn(normalUser());
        mvc.perform(post("/post/create").header("Authorization", "bearer valid-token"))
                .andExpect(status().isOk()).andExpect(content().string("user:7"));
    }

    @Test
    void bannedUsersAreRejectedOnPublicAndProtectedRoutes() throws Exception {
        User banned = normalUser();
        banned.setStatus(1);
        when(users.getByToken("banned-token")).thenReturn(banned);
        mvc.perform(get("/post/list").header("Authorization", "banned-token"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
        mvc.perform(post("/post/create").header("Authorization", "banned-token"))
                .andExpect(status().isForbidden());
        assertThat(UserContext.getUserId()).isNull();
    }

    @Test
    void loginAndErrorDispatchAreNotReplacedWithUnauthorized() throws Exception {
        mvc.perform(post("/auth/login")).andExpect(status().isOk());
        mvc.perform(get("/error")).andExpect(status().isOk());
        verifyNoInteractions(users);
    }

    @Test
    void optionsPreflightDoesNotRequireAToken() throws Exception {
        mvc.perform(options("/post/create")).andExpect(status().isOk());
        verifyNoInteractions(users);
    }

    private User normalUser() {
        User user = new User();
        user.setId(7L);
        user.setStatus(0);
        return user;
    }

    @Configuration
    @EnableWebMvc
    @Import(WebConfig.class)
    static class TestConfig {
        @Bean UserService userService() { return mock(UserService.class); }
        @Bean AuthInterceptor authInterceptor(UserService users) { return new AuthInterceptor(users); }
        @Bean ProbeController probeController() { return new ProbeController(); }
    }

    @RestController
    static class ProbeController {
        @GetMapping({"/post/list", "/post/detail/{id}", "/post/hot", "/post/search", "/comment/list",
                "/errand/list", "/errand/detail/{id}", "/second-hand/list", "/second-hand/detail/{id}",
                "/club/list", "/club/detail/{id}", "/club/activity/list", "/club/activity/detail/{id}", "/error"})
        String read() { return identity(); }
        @PostMapping({"/post/create", "/post/list", "/auth/login"})
        String write() { return identity(); }
        private String identity() { return UserContext.getUserId() == null ? "anonymous" : "user:" + UserContext.getUserId(); }
    }
}
