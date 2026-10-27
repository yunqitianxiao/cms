package com.ylf.cms.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ylf.cms.module.user.entity.SysRole;
import com.ylf.cms.module.user.vo.RolePermRow;
import com.ylf.cms.module.user.vo.RoleUserCountRow;
import com.ylf.cms.module.user.vo.UserRoleRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("select r.* from sys_role r join sys_user_role ur on ur.role_id = r.id "
            + "where ur.user_id = #{userId} and r.deleted = 0 order by r.sort asc, r.id asc")
    List<SysRole> selectByUserId(@Param("userId") Long userId);

    @Select("select count(1) from sys_user_role where role_id = #{roleId}")
    int countUsersOfRole(@Param("roleId") Long roleId);

    /**
     * 列表页用：一次把当前页所有用户的角色捞出来，Java 侧按 userId 分组。
     * 如果改成"先查用户、再对每个用户查一次角色"，20 条数据就是 21 条 SQL。
     */
    @Select("<script>"
            + "select ur.user_id as userId, r.id as roleId, r.role_code as roleCode, r.role_name as roleName "
            + "from sys_user_role ur join sys_role r on r.id = ur.role_id "
            + "where r.deleted = 0 and ur.user_id in "
            + "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    List<UserRoleRow> selectRoleRowsByUserIds(@Param("userIds") List<Long> userIds);

    @Select("<script>"
            + "select role_id as roleId, perm_id as permId from sys_role_permission where role_id in "
            + "<foreach collection='roleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    List<RolePermRow> selectPermRowsByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 统计"仍然有效的管理员"数量。删除/停用最后一个管理员会让系统失去管理入口，
     * 这种保护必须基于当前有效状态判断，不能数历史记录。
     */
    @Select("select count(distinct ur.user_id) from sys_user_role ur "
            + "join sys_role r on r.id = ur.role_id "
            + "join sys_user u on u.id = ur.user_id "
            + "where r.role_code = 'ADMIN' and r.status = 1 and r.deleted = 0 "
            + "and u.deleted = 0 and u.status = 1")
    int countActiveAdmins();

    @Select("select role_id as roleId, count(1) as userCount from sys_user_role group by role_id")
    List<RoleUserCountRow> selectUserCounts();
}
