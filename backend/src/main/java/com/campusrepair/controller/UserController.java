package com.campusrepair.controller;

import com.campusrepair.common.ApiResponse;
import com.campusrepair.domain.SysRole;
import com.campusrepair.domain.SysUser;
import com.campusrepair.dto.LoginRequest;
import com.campusrepair.security.CurrentUser;
import com.campusrepair.service.UserPermissionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserPermissionService userPermissionService;

    public UserController(UserPermissionService userPermissionService) {
        this.userPermissionService = userPermissionService;
    }

    @PostMapping("/auth/login")
    public ApiResponse<String> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(userPermissionService.login(request.username(), request.password()));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<SysUser>> listUsers(@RequestParam(required = false) String roleCode,
                                                 @RequestParam(required = false) Integer status) {
        return ApiResponse.success(userPermissionService.listUsers(roleCode, status));
    }

    @PostMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long userId, @RequestParam Integer status) {
        userPermissionService.updateUserStatus(userId, status);
        return ApiResponse.success(null);
    }

    @PostMapping("/users/{userId}/roles/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> assignRole(@PathVariable Long userId, @PathVariable Long roleId) {
        userPermissionService.assignRole(userId, roleId);
        return ApiResponse.success(null);
    }

    @GetMapping("/users/me/roles")
    public ApiResponse<List<SysRole>> getMyRoles(Authentication authentication) {
        return ApiResponse.success(userPermissionService.getUserRoles(CurrentUser.requireId(authentication)));
    }

    @GetMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<SysRole>> getUserRoles(@PathVariable Long userId) {
        return ApiResponse.success(userPermissionService.getUserRoles(userId));
    }
}
