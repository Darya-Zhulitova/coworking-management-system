package com.hse.userservice.feature.user.service;

import com.hse.userservice.common.context.CurrentUserService;
import com.hse.userservice.common.exception.ResourceConflictException;
import com.hse.userservice.common.security.AuthenticatedUserPrincipal;
import com.hse.userservice.common.security.JwtService;
import com.hse.userservice.common.security.UserPrincipalDetailsService;
import com.hse.userservice.feature.user.domain.User;
import com.hse.userservice.feature.user.dto.LoginRequest;
import com.hse.userservice.feature.user.dto.RegisterRequest;
import com.hse.userservice.feature.user.dto.UpdateUserRequest;
import com.hse.userservice.feature.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @Mock UserPrincipalDetailsService userDetailsService;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock CurrentUserService currentUserService;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(
                authenticationManager,
                jwtService,
                userDetailsService,
                userRepository,
                passwordEncoder,
                currentUserService
        );
    }

    @Test
    void loginAuthenticatesWithTrimmedEmailAndGeneratesJwtWithSubjectIdClaim() {
        AuthenticatedUserPrincipal principal = principal(11L, "daria@example.test");
        when(userDetailsService.loadUserByUsername("daria@example.test")).thenReturn(principal);
        when(jwtService.generateToken(eq(Map.<String, Object>of("subjectId", 11L)), eq(principal))).thenReturn("jwt-token");

        var response = service.login(new LoginRequest("  daria@example.test  ", "secret"));

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authCaptor.capture());
        assertThat(authCaptor.getValue().getName()).isEqualTo("daria@example.test");
        assertThat(authCaptor.getValue().getCredentials()).isEqualTo("secret");
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.userId()).isEqualTo(11L);
    }

    @Test
    void registerNormalizesEmailTrimsProfileFieldsEncodesPasswordAndReturnsLoginToken() {
        AuthenticatedUserPrincipal principal = principal(11L, "daria@example.test");
        when(userRepository.existsByEmailIgnoreCase("daria@example.test")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(11L);
            return user;
        });
        when(userDetailsService.loadUserByUsername("daria@example.test")).thenReturn(principal);
        when(jwtService.generateToken(eq(Map.<String, Object>of("subjectId", 11L)), eq(principal))).thenReturn("jwt-token");

        var response = service.register(new RegisterRequest(
                "  DARIA@Example.TEST  ",
                "secret123",
                "  Daria  ",
                "  Product designer  "
        ));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("daria@example.test");
        assertThat(userCaptor.getValue().getName()).isEqualTo("Daria");
        assertThat(userCaptor.getValue().getDescription()).isEqualTo("Product designer");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("encoded-password");
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.userId()).isEqualTo(11L);
    }

    @Test
    void registerRejectsDuplicateEmailBeforeSavingUser() {
        when(userRepository.existsByEmailIgnoreCase("daria@example.test")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest(
                " DARIA@example.test ",
                "secret123",
                "Daria",
                null
        ))).isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("email");

        verify(userRepository, never()).save(any());
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void getCurrentUserProfileMapsCurrentUser() {
        User user = user(11L, "daria@example.test", "Daria", "Description");
        when(currentUserService.getCurrentUser()).thenReturn(user);

        var profile = service.getCurrentUserProfile();

        assertThat(profile.id()).isEqualTo(11L);
        assertThat(profile.email()).isEqualTo("daria@example.test");
        assertThat(profile.name()).isEqualTo("Daria");
        assertThat(profile.description()).isEqualTo("Description");
    }

    @Test
    void updateCurrentUserProfileTrimsNameAndStoresNullForBlankDescription() {
        User user = user(11L, "daria@example.test", "Old", "Old description");
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        var profile = service.updateCurrentUserProfile(new UpdateUserRequest("  New name  ", "   "));

        assertThat(user.getName()).isEqualTo("New name");
        assertThat(user.getDescription()).isNull();
        assertThat(profile.name()).isEqualTo("New name");
        assertThat(profile.description()).isNull();
    }

    @Test
    void updateCurrentUserProfileTrimsNonBlankDescription() {
        User user = user(11L, "daria@example.test", "Old", null);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        var profile = service.updateCurrentUserProfile(new UpdateUserRequest("  New name  ", "  About me  "));

        assertThat(profile.name()).isEqualTo("New name");
        assertThat(profile.description()).isEqualTo("About me");
    }

    private static AuthenticatedUserPrincipal principal(Long userId, String email) {
        return new AuthenticatedUserPrincipal(userId, email, "hash", true, true, true, true, List.of());
    }

    private static User user(Long id, String email, String name, String description) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(name);
        user.setDescription(description);
        user.setPasswordHash("hash");
        return user;
    }
}
