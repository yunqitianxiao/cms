package com.ylf.cms.module.user.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户-角色关联表。复合主键在 MP 的 BaseMapper 里没有优雅表达，
 * 直接写注解 SQL 更清楚，也不需要为了三个方法造一个实体。
 */
@Mapper
public interface SysUserRoleMapper {

    @Insert("insert ignore into sys_user_role(user_id, role_id) values(#{userId}, #{roleId})")
    int insert(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Delete("delete from sys_user_role where user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Select("select role_id from sys_user_role where user_id = #{userId}")
    List<Long> selectRoleIds(@Param("userId") Long userId);

    @Select("select count(1) from sys_user_role where role_id = #{roleId}")
    int countByRoleId(@Param("roleId") Long roleId);
}
