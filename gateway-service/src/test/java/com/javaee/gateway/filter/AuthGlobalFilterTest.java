package com.javaee.gateway.filter;

import com.javaee.common.util.RabbitMQUtil;
import com.javaee.gateway.config.JwtConfig;
import com.javaee.gateway.config.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AuthGlobalFilter单元测试
 * 使用Mockito mock响应式组件
 */
@ExtendWith(MockitoExtension.class)
class AuthGlobalFilterTest {

    @Mock
    private RabbitMQUtil rabbitMQUtil;

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private AuthGlobalFilter authGlobalFilter;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    private org.springframework.http.server.RequestPath requestPath;

    @BeforeEach
    void setUp() {
        // 创建RequestPath mock（在setUp中创建避免嵌套mock问题）
        requestPath = mock(org.springframework.http.server.RequestPath.class);

        // 使用lenient()放宽stubbing规则（避免UnnecessaryStubbingException）
        lenient().when(exchange.getRequest()).thenReturn(request);
        lenient().when(exchange.getResponse()).thenReturn(response);
        lenient().when(request.getHeaders()).thenReturn(mock(HttpHeaders.class));
        lenient().when(request.getPath()).thenReturn(requestPath);
        lenient().when(chain.filter(any())).thenReturn(Mono.empty());
        lenient().when(response.setStatusCode(any())).thenReturn(true);
        lenient().when(response.setComplete()).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("白名单路径测试 - 登录路径应放行")
    void testWhiteListLoginPath() {
        when(requestPath.value()).thenReturn("/api/users/login");
        when(request.getMethod()).thenReturn(HttpMethod.POST);

        Mono<Void> result = authGlobalFilter.filter(exchange, chain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(jwtConfig, never()).validateToken(anyString());
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("白名单路径测试 - 注册路径应放行")
    void testWhiteListRegisterPath() {
        when(requestPath.value()).thenReturn("/api/users/register");
        when(request.getMethod()).thenReturn(HttpMethod.POST);

        Mono<Void> result = authGlobalFilter.filter(exchange, chain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(jwtConfig, never()).validateToken(anyString());
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("白名单路径测试 - WebSocket端点应放行")
    void testWhiteListWebSocketPath() {
        when(requestPath.value()).thenReturn("/ws/progress");
        when(request.getMethod()).thenReturn(HttpMethod.GET);

        Mono<Void> result = authGlobalFilter.filter(exchange, chain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(jwtConfig, never()).validateToken(anyString());
        verify(chain).filter(exchange);
    }

    @Test
    @DisplayName("无令牌请求测试 - 应返回401")
    void testNoTokenRequest() {
        when(requestPath.value()).thenReturn("/api/files/list");
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        Mono<Void> result = authGlobalFilter.filter(exchange, chain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("无效令牌格式测试 - 应返回401")
    void testInvalidTokenFormat() {
        when(requestPath.value()).thenReturn("/api/files/list");
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("InvalidToken");

        Mono<Void> result = authGlobalFilter.filter(exchange, chain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("无效令牌测试 - 应返回401")
    void testInvalidToken() {
        when(requestPath.value()).thenReturn("/api/files/list");
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer invalid.token.here");
        when(jwtConfig.validateToken("invalid.token.here")).thenReturn(false);

        Mono<Void> result = authGlobalFilter.filter(exchange, chain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(jwtConfig).validateToken("invalid.token.here");
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("有效令牌测试 - 应放行")
    void testValidToken() {
        when(requestPath.value()).thenReturn("/api/files/list");
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid.token.here");
        when(jwtConfig.validateToken("valid.token.here")).thenReturn(true);
        when(jwtConfig.getUserId("valid.token.here")).thenReturn(1L);
        when(jwtConfig.getUsername("valid.token.here")).thenReturn("testuser");
        when(jwtConfig.getRole("valid.token.here")).thenReturn("USER");

        // Mock request.mutate()
        ServerHttpRequest.Builder builder = mock(ServerHttpRequest.Builder.class);
        ServerHttpRequest newRequest = mock(ServerHttpRequest.class);
        lenient().when(request.mutate()).thenReturn(builder);
        lenient().when(builder.header(anyString(), anyString())).thenReturn(builder);
        lenient().when(builder.build()).thenReturn(newRequest);

        // Mock exchange.mutate()
        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        ServerWebExchange newExchange = mock(ServerWebExchange.class);
        lenient().when(exchange.mutate()).thenReturn(exchangeBuilder);
        lenient().when(exchangeBuilder.request(any(ServerHttpRequest.class))).thenReturn(exchangeBuilder);
        lenient().when(exchangeBuilder.build()).thenReturn(newExchange);
        lenient().when(newExchange.getRequest()).thenReturn(newRequest);

        Mono<Void> result = authGlobalFilter.filter(exchange, chain);

        StepVerifier.create(result)
                .verifyComplete();

        verify(jwtConfig).validateToken("valid.token.here");
        verify(jwtConfig).getUserId("valid.token.here");
        verify(jwtConfig).getUsername("valid.token.here");
    }

    @Test
    @DisplayName("过滤器顺序测试 - 应返回-100")
    void testGetOrder() {
        assertEquals(-100, authGlobalFilter.getOrder());
    }
}