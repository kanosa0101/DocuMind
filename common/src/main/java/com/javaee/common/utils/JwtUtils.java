package com.javaee.common.utils;

import com.javaee.common.constant.CommonConstant;
import com.javaee.common.constant.ErrorCodeEnum;
import com.javaee.common.exception.TokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author qxk
 * @description: JWT工具（生成/解析/验证）
 * 注意：密钥和过期时间从配置文件读取，确保安全性和可配置性
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.token-expiration:30}")
    private long tokenExpirationMinutes;

    @Value("${jwt.refresh-token-expiration:24}")
    private long refreshTokenExpirationHours;

    /**
     * 启动时验证密钥强度
     */
    @PostConstruct
    public void validateSecretKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT密钥长度不足32字节，请配置更强的密钥（建议64字符以上）");
        }
        if (secretKey.contains("DocuMind-2024") || secretKey.contains("Please-Change-This")) {
            throw new IllegalStateException("请勿使用默认JWT密钥模式，必须配置生产环境专用密钥");
        }
        log.info("JWT密钥验证通过，长度: {}字节", keyBytes.length);
    }

    /**
     * 获取签名密钥
     * @return SecretKey
     */
    private SecretKey getSigningKey() {
        // 确保密钥长度满足HMAC-SHA256的要求（至少256位/32字节）
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            log.warn("JWT密钥长度不足32字节，建议使用更长的密钥以增强安全性");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成访问令牌
     * @param userId 用户ID
     * @param username 用户名
     * @return 令牌
     */
    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CommonConstant.TOKEN_CLAIM_USER_ID, userId);
        claims.put(CommonConstant.TOKEN_CLAIM_USERNAME, username);
        return generateToken(claims, tokenExpirationMinutes * 60 * 1000);
    }

    /**
     * 生成访问令牌
     * @param userId 用户ID
     * @param username 用户名
     * @param role 角色
     * @return 令牌
     */
    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CommonConstant.TOKEN_CLAIM_USER_ID, userId);
        claims.put(CommonConstant.TOKEN_CLAIM_USERNAME, username);
        claims.put(CommonConstant.TOKEN_CLAIM_ROLE, role);
        return generateToken(claims, tokenExpirationMinutes * 60 * 1000);
    }

    /**
     * 生成刷新令牌
     * @param userId 用户ID
     * @return 刷新令牌
     */
    public String generateRefreshToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CommonConstant.TOKEN_CLAIM_USER_ID, userId);
        return generateToken(claims, refreshTokenExpirationHours * 60 * 60 * 1000);
    }

    /**
     * 生成令牌
     * @param claims 声明
     * @param expiration 过期时间（毫秒）
     * @return 令牌
     */
    private String generateToken(Map<String, Object> claims, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析令牌
     * @param token 令牌
     * @return 声明
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("JWT令牌解析失败: {}", e.getMessage());
            throw new TokenException(ErrorCodeEnum.TOKEN_ERROR);
        }
    }

    /**
     * 验证令牌
     * @param token 令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.warn("JWT令牌验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取用户ID
     * @param token 令牌
     * @return 用户ID
     */
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        return claims.get(CommonConstant.TOKEN_CLAIM_USER_ID, Long.class);
    }

    /**
     * 获取用户名
     * @param token 令牌
     * @return 用户名
     */
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get(CommonConstant.TOKEN_CLAIM_USERNAME, String.class);
    }

    /**
     * 获取角色
     * @param token 令牌
     * @return 角色
     */
    public String getRole(String token) {
        Claims claims = parseToken(token);
        return claims.get(CommonConstant.TOKEN_CLAIM_ROLE, String.class);
    }

    /**
     * 检查令牌是否过期
     * @param token 令牌
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 从请求头中提取令牌
     * @param authHeader 认证头
     * @return 令牌
     */
    public String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(CommonConstant.TOKEN_PREFIX)) {
            return authHeader.substring(CommonConstant.TOKEN_PREFIX.length());
        }
        throw new TokenException(ErrorCodeEnum.TOKEN_ERROR);
    }
}