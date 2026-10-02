package com.example.campusmarketserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

/** Exchanges a fresh wx.login code without exposing credentials in errors or logs. */
@Component
public class WechatLoginClient {

    private static final Logger log = LoggerFactory.getLogger(WechatLoginClient.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final RestClient client;
    private final String appId;
    private final String appSecret;
    private final String apiUrl;

    @Autowired
    public WechatLoginClient(@Value("${wechat.app-id}") String appId,
                             @Value("${wechat.app-secret}") String appSecret,
                             @Value("${wechat.api-url:https://api.weixin.qq.com/sns/jscode2session}") String apiUrl) {
        this(createClient(), appId, appSecret, apiUrl);
    }

    WechatLoginClient(RestClient client, String appId, String appSecret) {
        this(client, appId, appSecret, "https://api.weixin.qq.com/sns/jscode2session");
    }

    WechatLoginClient(RestClient client, String appId, String appSecret, String apiUrl) {
        this.client = client;
        this.appId = appId;
        this.appSecret = appSecret;
        this.apiUrl = apiUrl;
    }

    private static RestClient createClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(factory).build();
    }

    public String getOpenid(String code) {
        if (code == null || code.isBlank()) {
            throw new LoginException(400, "缺少微信登录凭证，请重新点击登录");
        }
        if (appId == null || appId.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new LoginException(503, "服务端未配置微信登录，请配置 WX_APP_ID 和 WX_APP_SECRET");
        }
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new LoginException(503, "服务端未配置微信登录接口地址");
        }

        final String response;
        try {
            var uri = UriComponentsBuilder.fromUriString(apiUrl)
                    .queryParam("appid", appId)
                    .queryParam("secret", appSecret)
                    .queryParam("js_code", code.trim())
                    .queryParam("grant_type", "authorization_code")
                    .build().encode().toUri();
            response = client.get().uri(uri).retrieve().body(String.class);
        } catch (RestClientException ex) {
            // The exception's URL can contain appSecret and code; never log it.
            Throwable cause = ex;
            while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
            // Do not log the exception message: RestClient may include the full
            // upstream URL or response body, both of which can contain secrets.
            log.warn("WeChat login service request failed ({})", cause.getClass().getSimpleName());
            throw new LoginException(502, "暂时无法连接微信登录服务，请稍后重试");
        }

        final JsonNode session;
        try {
            session = response == null ? null : JSON.readTree(response);
        } catch (JacksonException ex) {
            throw new LoginException(502, "微信登录服务返回异常，请稍后重试");
        }
        if (session == null || !session.isObject()) {
            throw new LoginException(502, "微信登录服务返回异常，请稍后重试");
        }
        JsonNode error = session.get("errcode");
        if (error != null && error.asInt(-1) != 0) {
            int errorCode = error.asInt(-1);
            log.warn("WeChat login rejected with error code {}", errorCode);
            throw switch (errorCode) {
                case 40029, 40163 -> new LoginException(400, "微信登录凭证已失效，请重新点击登录");
                case 40013, 40001, 40125 -> new LoginException(503,
                        "微信登录配置不匹配，请检查开发者工具 AppID 与服务端 WX_APP_ID、WX_APP_SECRET");
                case 45009, 45011 -> new LoginException(429, "微信登录请求过于频繁，请稍后重试");
                default -> new LoginException(502, "微信登录失败（错误码 " + errorCode + "），请稍后重试");
            };
        }
        JsonNode openid = session.get("openid");
        if (openid == null || !openid.isString() || openid.asString().isBlank()) {
            throw new LoginException(502, "微信登录服务未返回用户标识，请重新登录");
        }
        return openid.asString();
    }

    public static class LoginException extends RuntimeException {
        private final int code;

        public LoginException(int code, String message) {
            super(message);
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
