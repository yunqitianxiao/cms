package com.ylf.cms.common.util;

import com.ylf.cms.common.exception.BizException;
import com.ylf.cms.common.result.ResultCode;

import java.util.Arrays;
import java.util.Map;

/**
 * 排序字段白名单。
 *
 * 直接把前端传来的 sort 拼进 ORDER BY 是 SQL 注入的经典入口，而且这里用不了参数化
 * （ORDER BY 后面不能是占位符）。所以只允许"逻辑字段名 → 物理列名"的映射表命中项通过。
 */
public final class SortWhitelist {

    private static final int MAX_SORT_PARTS = 3;

    private SortWhitelist() {
    }

    /**
     * @param sort    形如 "createTime,desc;title,asc"
     * @param allowed 逻辑字段名 → 物理列名
     * @param fallback 没有任何合法字段时使用的默认排序（同样是白名单内的物理列名）
     */
    public static String build(String sort, Map<String, String> allowed, String fallback) {
        return build(sort, allowed, fallback, null);
    }

    /**
     * @param tiebreaker 追加在末尾的唯一排序键。只按非唯一列排序时，值相同的行在两次查询里
     *                   顺序可能不同，翻页会出现重复或漏行。
     */
    public static String build(String sort, Map<String, String> allowed, String fallback, String tiebreaker) {
        if (sort == null || sort.trim().isEmpty()) {
            return fallback;
        }
        StringBuilder sb = new StringBuilder();
        String[] parts = sort.split(";");
        if (parts.length > MAX_SORT_PARTS) {
            throw new BizException(ResultCode.BAD_REQUEST, "排序字段最多 3 个");
        }
        for (String part : parts) {
            String[] pair = part.split(",");
            String field = pair[0].trim();
            String column = allowed.get(field);
            if (column == null) {
                throw new BizException(ResultCode.BAD_REQUEST,
                        "不支持的排序字段：" + field + "，可选：" + allowed.keySet());
            }
            String direction = "asc";
            if (pair.length > 1) {
                direction = pair[1].trim().toLowerCase();
                if (!Arrays.asList("asc", "desc").contains(direction)) {
                    throw new BizException(ResultCode.BAD_REQUEST, "排序方向只能是 asc 或 desc");
                }
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(column).append(' ').append(direction);
        }
        if (tiebreaker != null && !tiebreaker.trim().isEmpty()) {
            sb.append(", ").append(tiebreaker);
        }
        return sb.toString();
    }
}
