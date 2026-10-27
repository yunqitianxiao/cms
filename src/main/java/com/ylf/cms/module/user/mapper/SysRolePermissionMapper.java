package com.ylf.cms.module.user.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRolePermissionMapper {

    @Insert("insert ignore into sys_role_permission(role_id, perm_id) values(#{roleId}, #{permId})")
    int insert(@Param("roleId") Long roleId, @Param("permId") Long permId);

    @Delete("delete from sys_role_permission where role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Select("select perm_id from sys_role_permission where role_id = #{roleId}")
    List<Long> selectPermIds(@Param("roleId") Long roleId);
}
