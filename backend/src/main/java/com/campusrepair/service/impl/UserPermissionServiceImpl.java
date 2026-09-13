package com.campusrepair.service.impl;

import com.campusrepair.common.BusinessException;
import com.campusrepair.common.ErrorCode;
import com.campusrepair.domain.SysRole;
import com.campusrepair.domain.SysUser;
import com.campusrepair.domain.SysUserRole;
import com.campusrepair.repository.UserPermissionRepository;
import com.campusrepair.security.JwtTokenProvider;
import com.campusrepair.service.UserPermissionService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserPermissionServiceImpl implements UserPermissionService {
    private final UserPermissionRepository userPermissionRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public UserPermissionServiceImpl(UserPermissionRepository userPermissionRepository,
                                     JwtTokenProvider jwtTokenProvider,
                                     PasswordEncoder passwordEncoder) {
        this.userPermissionRepository = userPermissionRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        SysUser user = userPermissionRepository.findUserByUsername(username.trim());
        if (user == null || !user.isEnabled() || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        List<String> roles = getUserRoles(user.getUserId()).stream()
                .map(SysRole::getRoleCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();
        return jwtTokenProvider.generateToken(user.getUserId(), roles);
    }

    @Override
    public List<SysUser> listUsers(String roleCode, Integer status) {
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户状态必须为0或1");
        }
        return userPermissionRepository.findUsers(roleCode == null ? null : roleCode.trim(), status);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long userId, Integer status) {
        SysUser user = requireUser(userId);
        user.changeStatus(status);
        if (userPermissionRepository.updateStatus(userId, status) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "用户状态更新失败");
        }
    }

    @Override
    @Transactional
    public void assignRole(Long userId, Long roleId) {
        requireUser(userId);
        if (roleId == null || userPermissionRepository.findRoleById(roleId) == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        if (userPermissionRepository.countUserRole(userId, roleId) == 0) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userPermissionRepository.saveUserRole(relation);
        }
    }

    @Override
    public List<SysRole> getUserRoles(Long userId) {
        requireUser(userId);
        return userPermissionRepository.findRolesByUserId(userId);
    }

    private SysUser requireUser(Long userId) {
        if (userId == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "用户ID不能为空");
        SysUser user = userPermissionRepository.findUserById(userId);
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        return user;
    }
}
