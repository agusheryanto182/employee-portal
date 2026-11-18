package com.employeeportal.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.employeeportal.auth.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    RefreshTokenService refreshTokenService;

    UserAdminService service;
    final UUID adminId = UUID.randomUUID();
    final UUID userId = UUID.randomUUID();
    User user;

    @BeforeEach
    void setUp() {
        service = new UserAdminService(userRepository, refreshTokenService);
        user = User.active("budi@example.com", "Budi", Role.EMPLOYEE, "hash");
        user.setId(userId);
    }

    private void userExists() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createNormalizesTheEmailAndStartsAsPending() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse created = service.create(new CreateUserRequest(" New@Example.com ", " New User ", " ", Role.EMPLOYEE));

        assertThat(created.email()).isEqualTo("new@example.com");
        assertThat(created.fullName()).isEqualTo("New User");
        assertThat(created.phone()).isNull();
        assertThat(created.status()).isEqualTo(UserStatus.PENDING);
    }

    @Test
    void createRejectsARegisteredEmail() {
        when(userRepository.existsByEmail("budi@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateUserRequest("BUDI@example.com", "Budi", null, Role.EMPLOYEE)))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    void keepingTheSameEmailIsNotAConflict() {
        userExists();

        service.update(userId, new UpdateUserRequest("Budi@Example.com", null, null, null), adminId);

        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void roleChangeRevokesSessionsButOtherEditsDoNot() {
        userExists();

        service.update(userId, new UpdateUserRequest(null, "Budi S", null, Role.EMPLOYEE), adminId);
        verify(refreshTokenService, never()).revokeAll(any());

        service.update(userId, new UpdateUserRequest(null, null, null, Role.ADMIN), adminId);
        verify(refreshTokenService).revokeAll(userId);
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void adminCannotChangeTheirOwnRoleOrDeactivateThemselves() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        user.setRole(Role.ADMIN);

        assertThatThrownBy(() -> service.update(userId, new UpdateUserRequest(null, null, null, Role.EMPLOYEE), userId))
                .isInstanceOf(SelfModificationException.class);
        assertThatThrownBy(() -> service.deactivate(userId, userId))
                .isInstanceOf(SelfModificationException.class);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(refreshTokenService, never()).revokeAll(any());
    }

    @Test
    void deactivatingTwiceRevokesSessionsOnce() {
        userExists();

        service.deactivate(userId, adminId);
        service.deactivate(userId, adminId);

        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        verify(refreshTokenService).revokeAll(userId);
    }

    @Test
    void activateOnlyChangesInactiveUsers() {
        userExists();
        User pending = User.pending("p@example.com", "Pending", null, Role.EMPLOYEE);
        UUID pendingId = UUID.randomUUID();
        pending.setId(pendingId);
        when(userRepository.findById(pendingId)).thenReturn(Optional.of(pending));

        assertThat(service.activate(pendingId).status()).isEqualTo(UserStatus.PENDING);

        user.deactivate();
        assertThat(service.activate(userId).status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void unknownUserIsNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(userId)).isInstanceOf(UserNotFoundException.class);
    }
}
