package com.dev.ecommerce.service;

import com.dev.ecommerce.config.JwtProperties;
import com.dev.ecommerce.dto.request.ForgotPasswordRequest;
import com.dev.ecommerce.dto.request.LoginRequest;
import com.dev.ecommerce.dto.request.RefreshTokenRequest;
import com.dev.ecommerce.dto.request.RegisterRequest;
import com.dev.ecommerce.dto.request.ResetPasswordRequest;
import com.dev.ecommerce.dto.response.AuthResponse;
import com.dev.ecommerce.entity.Role;
import com.dev.ecommerce.entity.User;
import com.dev.ecommerce.entity.enums.RoleName;
import com.dev.ecommerce.exception.BusinessException;
import com.dev.ecommerce.repository.RoleRepository;
import com.dev.ecommerce.repository.UserRepository;
import com.dev.ecommerce.security.JwtTokenProvider;
import com.dev.ecommerce.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private TokenService tokenService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private Role userRole;
    private User user;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        userRole = new Role(RoleName.USER);
        userRole.setId(1L);

        user = new User("test@example.com", "encoded-password", "Test User");
        user.setId(1L);
        user.getRoles().add(userRole);

        userPrincipal = new UserPrincipal(user);
    }

    @Test
    void register_shouldCreateUserAndReturnTokens() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setFullName("New User");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtTokenProvider.extractTokenId("refresh-token")).thenReturn("refresh-id");
        when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(900_000L);

        AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUser().getEmail()).isEqualTo("new@example.com");
        verify(tokenService).storeRefreshToken(eq(2L), eq("refresh-id"), eq("refresh-token"));
    }

    @Test
    void register_shouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Email already registered")
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void login_shouldReturnTokens() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities()));
        when(jwtTokenProvider.generateAccessToken(userPrincipal)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(userPrincipal)).thenReturn("refresh-token");
        when(jwtTokenProvider.extractTokenId("refresh-token")).thenReturn("refresh-id");
        when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(900_000L);

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUser().getRoles()).contains("USER");
    }

    @Test
    void refreshToken_shouldRejectInvalidTokenType() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-token");

        when(jwtTokenProvider.extractTokenType("invalid-token")).thenReturn("access");

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void forgotPassword_shouldSendEmailWhenUserExists() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        authService.forgotPassword(request);

        verify(tokenService).storePasswordResetToken(anyString(), eq("test@example.com"), eq(900_000L));
        verify(emailService).sendPasswordResetEmail(eq("test@example.com"), anyString());
    }

    @Test
    void forgotPassword_shouldNotRevealMissingUser() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("missing@example.com");

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword(request);

        verify(tokenService, never()).storePasswordResetToken(anyString(), anyString(), anyLong());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void resetPassword_shouldRejectInvalidToken() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("bad-token");
        request.setNewPassword("newpassword123");

        when(tokenService.getPasswordResetEmail("bad-token")).thenReturn(null);

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Invalid or expired reset token");
    }
}
