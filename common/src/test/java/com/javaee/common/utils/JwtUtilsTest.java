package com.javaee.common.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtils单元测试
 */
class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        // 手动注入配置值（测试环境）
        ReflectionTestUtils.setField(jwtUtils, "secretKey",
            "test-secret-key-for-unit-testing-must-be-at-least-32-bytes-long-please-change-this");
        ReflectionTestUtils.setField(jwtUtils, "tokenExpirationMinutes", 30);
        ReflectionTestUtils.setField(jwtUtils, "refreshTokenExpirationHours", 24);
    }

    @Test
    @DisplayName("JWT生成测试 - 正常生成")
    void testGenerateToken() {
        String token = jwtUtils.generateToken(1L, "testuser");

        assertNotNull(token);
        assertTrue(token.length() > 50);
        assertTrue(token.split("\\.").length == 3); // JWT三段格式
    }

    @Test
    @DisplayName("JWT生成测试 - 带角色")
    void testGenerateTokenWithRole() {
        String token = jwtUtils.generateToken(1L, "testuser", "admin");

        assertNotNull(token);
        String role = jwtUtils.getRole(token);
        assertEquals("admin", role);
    }

    @Test
    @DisplayName("JWT解析测试 - 提取用户ID")
    void testGetUserId() {
        String token = jwtUtils.generateToken(123L, "testuser");

        Long userId = jwtUtils.getUserId(token);

        assertEquals(123L, userId);
    }

    @Test
    @DisplayName("JWT解析测试 - 提取用户名")
    void testGetUsername() {
        String token = jwtUtils.generateToken(1L, "myusername");

        String username = jwtUtils.getUsername(token);

        assertEquals("myusername", username);
    }

    @Test
    @DisplayName("JWT验证测试 - 有效token")
    void testValidateTokenValid() {
        String token = jwtUtils.generateToken(1L, "testuser");

        assertTrue(jwtUtils.validateToken(token));
    }

    @Test
    @DisplayName("JWT验证测试 - 无效token")
    void testValidateTokenInvalid() {
        String invalidToken = "invalid.jwt.token";

        assertFalse(jwtUtils.validateToken(invalidToken));
    }

    @Test
    @DisplayName("JWT验证测试 - 空token")
    void testValidateTokenEmpty() {
        assertFalse(jwtUtils.validateToken(""));
        assertFalse(jwtUtils.validateToken(null));
    }

    @Test
    @DisplayName("JWT过期测试 - 未过期token")
    void testIsTokenExpiredNotExpired() {
        String token = jwtUtils.generateToken(1L, "testuser");

        assertFalse(jwtUtils.isTokenExpired(token));
    }

    @Test
    @DisplayName("刷新令牌生成测试")
    void testGenerateRefreshToken() {
        String refreshToken = jwtUtils.generateRefreshToken(1L);

        assertNotNull(refreshToken);
        assertTrue(refreshToken.length() > 50);

        // 刷新令牌应包含userId
        Long userId = jwtUtils.getUserId(refreshToken);
        assertEquals(1L, userId);
    }

    @Test
    @DisplayName("令牌解析测试 - 解析成功")
    void testParseTokenSuccess() {
        String token = jwtUtils.generateToken(1L, "testuser", "admin");

        var claims = jwtUtils.parseToken(token);

        assertNotNull(claims);
        assertEquals(1L, claims.get("userId", Long.class));
        assertEquals("testuser", claims.get("username", String.class));
        assertEquals("admin", claims.get("role", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    @DisplayName("令牌提取测试 - 从Authorization header提取")
    void testExtractToken() {
        String authHeader = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";

        String token = jwtUtils.extractToken(authHeader);

        assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test", token);
    }
}