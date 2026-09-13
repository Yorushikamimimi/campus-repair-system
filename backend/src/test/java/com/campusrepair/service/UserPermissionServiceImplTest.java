package com.campusrepair.service;

import com.campusrepair.common.BusinessException;
import com.campusrepair.domain.SysRole;
import com.campusrepair.domain.SysUser;
import com.campusrepair.repository.UserPermissionRepository;
import com.campusrepair.security.JwtTokenProvider;
import com.campusrepair.service.impl.UserPermissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPermissionServiceImplTest {
    @Mock UserPermissionRepository repository;
    @Mock JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserPermissionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserPermissionServiceImpl(repository, tokenProvider, passwordEncoder);
    }

    @Test
    void loginWithValidCredentialsReturnsJwt() {
        SysUser user = user(7L, true);
        SysRole role = role("REPORTER");
        when(repository.findUserByUsername("alice")).thenReturn(user);
        when(repository.findUserById(7L)).thenReturn(user);
        when(repository.findRolesByUserId(7L)).thenReturn(List.of(role));
        when(tokenProvider.generateToken(eq(7L), eq(List.of("REPORTER")))).thenReturn("jwt-token");

        assertEquals("jwt-token", service.login(" alice ", "secret"));
        verify(tokenProvider).generateToken(7L, List.of("REPORTER"));
    }

    @Test
    void loginRejectsUnknownUser() {
        when(repository.findUserByUsername("missing")).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.login("missing", "secret"));
        verify(tokenProvider, never()).generateToken(any(), any());
    }

    @Test
    void loginRejectsWrongPassword() {
        SysUser user = user(7L, true);
        when(repository.findUserByUsername("alice")).thenReturn(user);

        assertThrows(BusinessException.class, () -> service.login("alice", "wrong"));
        verify(repository, never()).findRolesByUserId(7L);
    }

    @Test
    void loginRejectsDisabledUser() {
        SysUser user = user(7L, false);
        when(repository.findUserByUsername("alice")).thenReturn(user);

        assertThrows(BusinessException.class, () -> service.login("alice", "secret"));
        verify(repository, never()).findRolesByUserId(7L);
    }

    @Test
    void getUserRolesReturnsRolesFromAssociationTable() {
        SysUser user = user(7L, true);
        SysRole role = role("REPORTER");
        when(repository.findUserById(7L)).thenReturn(user);
        when(repository.findRolesByUserId(7L)).thenReturn(List.of(role));

        assertEquals(List.of(role), service.getUserRoles(7L));
    }

    private SysUser user(Long id, boolean enabled) {
        SysUser user = new SysUser();
        user.setUserId(id);
        user.setUsername("alice");
        user.setPasswordHash(passwordEncoder.encode("secret"));
        user.setStatus(enabled ? 1 : 0);
        return user;
    }

    private SysRole role(String code) {
        SysRole role = new SysRole();
        role.setRoleCode(code);
        return role;
    }
}
