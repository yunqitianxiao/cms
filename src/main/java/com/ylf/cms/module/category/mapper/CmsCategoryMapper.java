package com.ylf.cms.module.category.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ylf.cms.module.category.entity.CmsCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CmsCategoryMapper extends BaseMapper<CmsCategory> {

    @Select("select count(1) from cms_category where parent_id = #{parentId} and deleted = 0")
    int countChildren(@Param("parentId") Long parentId);

    /** 停用保护：父分类停用后子分类仍启用，会留下"父不可见、子仍可选"的矛盾状态 */
    @Select("select count(1) from cms_category where parent_id = #{parentId} and status = 1 and deleted = 0")
    int countEnabledChildren(@Param("parentId") Long parentId);

    /** 删除保护：被文章引用的分类不允许删 */
    @Select("select count(1) from cms_article where category_id = #{categoryId} and deleted = 0")
    int countArticles(@Param("categoryId") Long categoryId);
}
