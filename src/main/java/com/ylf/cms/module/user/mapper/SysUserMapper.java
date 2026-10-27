package com.ylf.cms.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ylf.cms.module.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 密码列在实体上被 select=false 屏蔽，校验密码时单独取 */
    @Select("select password from sys_user where username = #{username} and deleted = 0")
    String selectPasswordByUsername(@Param("username") String username);

    @Select("select password from sys_user where id = #{id} and deleted = 0")
    String selectPasswordById(@Param("id") Long id);

    /**
     * 一次查出用户的有效权限码：停用的角色不参与授权，逻辑删除的角色同样排除。
     * 这条 SQL 是"权限取并集"的唯一来源，Service 不做二次过滤，避免两处口径不一致。
     */
    @Select("select distinct p.perm_code from sys_permission p "
            + "join sys_role_permission rp on rp.perm_id = p.id "
            + "join sys_role r on r.id = rp.role_id "
            + "join sys_user_role ur on ur.role_id = r.id "
            + "where ur.user_id = #{userId} and r.status = 1 and r.deleted = 0")
    List<String> selectPermCodes(@Param("userId") Long userId);

    @Select("select r.role_code from sys_role r "
            + "join sys_user_role ur on ur.role_id = r.id "
            + "where ur.user_id = #{userId} and r.status = 1 and r.deleted = 0")
    List<String> selectRoleCodes(@Param("userId") Long userId);
}
