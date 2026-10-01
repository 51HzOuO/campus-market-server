package com.example.campusmarketserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class WechatLoginClientTest {
    private MockRestServiceServer server;
    private WechatLoginClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new WechatLoginClient(builder.build(), "test-app", "test-secret");
    }

    @Test
    void parsesFormattedJsonAndAcceptsExplicitSuccessErrorCode() {
        server.expect(requestTo("https://api.weixin.qq.com/sns/jscode2session?appid=test-app&secret=test-secret&js_code=fresh-code&grant_type=authorization_code"))
                .andRespond(withSuccess("{\n \"errcode\": 0, \"openid\" : \"user-openid\", \"session_key\": \"private-key\"\n}", MediaType.APPLICATION_JSON));
        assertThat(client.getOpenid("fresh-code")).isEqualTo("user-openid");
        server.verify();
    }

    @ParameterizedTest
    @CsvSource({"40029,400", "40163,400", "40013,503", "40125,503", "45011,429", "-1,502"})
    void returnsActionableErrorsWithoutUpstreamMessageOrSecrets(int upstreamCode, int resultCode) {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://api.weixin.qq.com/")))
                .andRespond(withSuccess("{\"errcode\":" + upstreamCode + ",\"errmsg\":\"test-secret private-key\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.getOpenid("fresh-code"))
                .isInstanceOfSatisfying(WechatLoginClient.LoginException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(resultCode);
                    assertThat(ex.getMessage()).doesNotContain("test-secret", "private-key", "fresh-code");
                });
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "{}", "null", "[]", "{\"openid\":\"\"}", "{\"openid\":123}"})
    void rejectsMalformedOrIncompleteSessions(String body) {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://api.weixin.qq.com/")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.getOpenid("fresh-code"))
                .isInstanceOfSatisfying(WechatLoginClient.LoginException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(502));
        server.verify();
    }

    @Test
    void sanitizesHttpErrors() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://api.weixin.qq.com/")))
                .andRespond(withServerError().body("test-secret"));
        assertSafeNetworkFailure();
    }

    @Test
    void sanitizesConnectionErrors() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("https://api.weixin.qq.com/")))
                .andRespond(withException(new IOException("secret=test-secret&js_code=fresh-code")));
        assertSafeNetworkFailure();
    }

    @Test
    void blankCodeAndMissingConfigurationNeverSendARequest() {
        assertThatThrownBy(() -> client.getOpenid(" "))
                .isInstanceOfSatisfying(WechatLoginClient.LoginException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400));
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer emptyServer = MockRestServiceServer.bindTo(builder).build();
        WechatLoginClient unconfigured = new WechatLoginClient(builder.build(), "", "");
        assertThatThrownBy(() -> unconfigured.getOpenid("fresh-code"))
                .isInstanceOfSatisfying(WechatLoginClient.LoginException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(503));
        server.verify();
        emptyServer.verify();
    }

    private void assertSafeNetworkFailure() {
        assertThatThrownBy(() -> client.getOpenid("fresh-code"))
                .isInstanceOfSatisfying(WechatLoginClient.LoginException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(502);
                    assertThat(ex.getMessage()).doesNotContain("test-secret", "fresh-code");
                    assertThat(ex.getCause()).isNull();
                });
        server.verify();
    }
}
