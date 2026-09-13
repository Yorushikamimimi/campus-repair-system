package com.campusrepair.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusrepair.domain.SysRole;
import com.campusrepair.domain.SysUser;
import com.campusrepair.domain.SysUserRole;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserPermissionRepository extends BaseMapper<SysUser> {
    default SysUser findUserByUsername(String username) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUser>()
                .eq("username", username));
    }

    default SysUser findUserById(Long userId) {
        return selectById(userId);
    }

    @Select("SELECT r.role_id, r.role_name, r.role_code, r.remark "
            + "FROM sys_role r INNER JOIN sys_user_role ur ON ur.role_id = r.role_id "
            + "WHERE ur.user_id = #{userId} ORDER BY r.role_id")
    List<SysRole> findRolesByUserId(@Param("userId") Long userId);

    default void saveUser(SysUser user) {
        insert(user);
    }

    @Update("UPDATE sys_user SET status = #{status}, update_time = CURRENT_TIMESTAMP WHERE user_id = #{userId}")
    int updateStatus(@Param("userId") Long userId, @Param("status") Integer status);

    @Insert("INSERT INTO sys_user_role(user_id, role_id, assign_time) VALUES(#{userId}, #{roleId}, CURRENT_TIMESTAMP)")
    int saveUserRole(SysUserRole userRole);

    @Select("SELECT COUNT(*) FROM sys_user_role WHERE user_id = #{userId} AND role_id = #{roleId}")
    int countUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Select("SELECT role_id, role_name, role_code, remark FROM sys_role WHERE role_id = #{roleId}")
    SysRole findRoleById(@Param("roleId") Long roleId);

    @Select({"<script>",
            "SELECT DISTINCT u.* FROM sys_user u",
            "<if test=\"roleCode != null and roleCode != ''\">",
            "INNER JOIN sys_user_role ur ON ur.user_id = u.user_id",
            "INNER JOIN sys_role r ON r.role_id = ur.role_id",
            "</if>",
            "<where>",
            "<if test=\"roleCode != null and roleCode != ''\">AND r.role_code = #{roleCode}</if>",
            "<if test='status != null'>AND u.status = #{status}</if>",
            "</where>",
            "ORDER BY u.user_id",
            "</script>"})
    List<SysUser> findUsers(@Param("roleCode") String roleCode, @Param("status") Integer status);
}
