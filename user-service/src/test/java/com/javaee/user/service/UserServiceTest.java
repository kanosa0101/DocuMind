package com.javaee.user.service;

import com.javaee.user.UserApplication;
import com.javaee.user.dto.LoginDTO;
import com.javaee.user.dto.RegisterDTO;
import com.javaee.user.entity.User;
import com.javaee.user.mapper.UserMapper;
import com.javaee.user.service.impl.UserServiceImpl;
import com.javaee.user.vo.LoginVO;
import com.javaee.user.vo.UserVO;
import com.javaee.common.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * UserService集成测试
 * 使用@SpringBootTest启动Spring容器，@MockBean替换外部依赖
 */
@SpringBootTest(classes = UserApplication.class)
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    private UserServiceImpl userService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private BCryptPasswordEncoder passwordEncoder;

    @MockBean
    private JwtUtils jwtUtils;

    private User testUser;
    private RegisterDTO registerDTO;
    private LoginDTO loginDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setEmail("test@example.com");
        testUser.setStatus(1);  // 启用状态
        testUser.setRole("USER");  // 默认角色

        registerDTO = new RegisterDTO();
        registerDTO.setUsername("testuser");
        registerDTO.setPassword("password123");
        registerDTO.setEmail("test@example.com");

        loginDTO = new LoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("password123");
    }

    @Test
    @DisplayName("用户注册测试 - 正常注册")
    void testRegisterSuccess() {
        // Mock: 用户名不存在
        when(userMapper.selectByUsername("testuser")).thenReturn(null);
        when(userMapper.selectByEmail("test@example.com")).thenReturn(null);
        // Mock: 密码加密
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        // Mock: 插入成功 - 返回影响行数
        when(userMapper.insert(any(User.class))).thenReturn(1);

        UserVO result = userService.register(registerDTO);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());

        verify(userMapper).selectByUsername("testuser");
        verify(passwordEncoder).encode("password123");
        verify(userMapper).insert(any(User.class));
    }

    @Test
    @DisplayName("用户注册测试 - 用户名已存在")
    void testRegisterUsernameExists() {
        // Mock: 用户名已存在
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);

        assertThrows(RuntimeException.class, () -> {
            userService.register(registerDTO);
        });

        verify(userMapper).selectByUsername("testuser");
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("用户登录测试 - 成功登录")
    void testLoginSuccess() {
        // Mock: 找到用户
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        // Mock: 密码匹配
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        // Mock: JWT生成
        when(jwtUtils.generateToken(anyLong(), anyString(), anyString())).thenReturn("accessToken123");
        when(jwtUtils.generateRefreshToken(anyLong())).thenReturn("refreshToken123");

        LoginVO result = userService.login(loginDTO);

        assertNotNull(result);
        assertNotNull(result.getAccessToken());
        assertNotNull(result.getRefreshToken());

        verify(userMapper).selectByUsername("testuser");
        verify(passwordEncoder).matches("password123", "encodedPassword");
    }

    @Test
    @DisplayName("用户登录测试 - 用户不存在")
    void testLoginUserNotFound() {
        // Mock: 用户不存在
        when(userMapper.selectByUsername("testuser")).thenReturn(null);

        assertThrows(RuntimeException.class, () -> {
            userService.login(loginDTO);
        });

        verify(userMapper).selectByUsername("testuser");
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("用户登录测试 - 密码错误")
    void testLoginWrongPassword() {
        // Mock: 找到用户
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        // Mock: 密码不匹配
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> {
            userService.login(loginDTO);
        });

        verify(userMapper).selectByUsername("testuser");
        verify(passwordEncoder).matches("password123", "encodedPassword");
    }

    @Test
    @DisplayName("根据用户名查询测试 - 用户存在")
    void testGetUserByUsernameExists() {
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);

        User result = userService.getUserByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());

        verify(userMapper).selectByUsername("testuser");
    }

    @Test
    @DisplayName("根据用户名查询测试 - 用户不存在")
    void testGetUserByUsernameNotExists() {
        when(userMapper.selectByUsername("nonexistent")).thenReturn(null);

        User result = userService.getUserByUsername("nonexistent");

        assertNull(result);

        verify(userMapper).selectByUsername("nonexistent");
    }
}