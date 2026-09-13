package com.campusrepair.service;

import com.campusrepair.domain.SysRole;
import com.campusrepair.domain.SysUser;

import java.util.List;

public interface UserPermissionService {
    String login(String username, String password);
    List<SysUser> listUsers(String roleCode, Integer status);
    void updateUserStatus(Long userId, Integer status);
    void assignRole(Long userId, Long roleId);
    List<SysRole> getUserRoles(Long userId);
}
