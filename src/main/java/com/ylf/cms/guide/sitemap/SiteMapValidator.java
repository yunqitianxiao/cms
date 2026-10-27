package com.ylf.cms.guide.sitemap;

import com.ylf.cms.guide.model.GuideCardVO;
import com.ylf.cms.guide.model.SiteMapSnapshot;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 站点地图启动期校验。
 *
 * 设计目标：把"智能体知道全站"从一句承诺变成一条硬约束。
 * 任何一条不满足，服务直接起不来——而不是等到演示时才发现某个页面不在卡片里。
 *
 * 这里刻意写成纯函数（不依赖 Spring、不碰数据库），
 * 这样每条规则都能用一个会失败的单元测试验证，而不是"校验方法存在但永远不触发"。
 */
public final class SiteMapValidator {

    private SiteMapValidator() {
    }

    /**
     * @param snapshot       采集结果
     * @param knownPermCodes sys_permission 里真实存在的权限码
     * @param yamlKeys       site-map.yaml 里出现的 key
     * @return 错误描述列表，空列表表示通过
     */
    public static List<String> validate(SiteMapSnapshot snapshot,
                                        Set<String> knownPermCodes,
                                        Set<String> yamlKeys) {
        List<String> errors = new ArrayList<>();

        // C1 卡片 id 唯一
        for (String dup : snapshot.getDuplicateIds()) {
            errors.add("C1 卡片 id 重复：" + dup);
        }

        // C2 每个受控 Controller 至少被一张卡片覆盖
        for (String controller : snapshot.getUncoveredPermittedMethods()) {
            errors.add("C2 Controller 有 @RequirePermission 但整类没有 @GuideCard：" + controller
                    + "（智能体将不知道这个模块的存在）");
        }

        // C3 卡片声明的权限码必须真实存在
        for (GuideCardVO card : snapshot.getCards().values()) {
            for (String perm : card.getPermissions()) {
                if (!knownPermCodes.contains(perm)) {
                    errors.add("C3 卡片 " + card.getId() + " 引用了不存在的权限码：" + perm);
                }
            }
        }

        // C4 yaml 不允许凭空新增页面，也不允许遗漏必要文案
        for (String key : yamlKeys) {
            if (!snapshot.getCards().containsKey(key)) {
                errors.add("C4 site-map.yaml 中的 " + key + " 在代码里不存在对应卡片");
            }
        }
        for (GuideCardVO card : snapshot.getCards().values()) {
            if (!StringUtils.hasText(card.getSummary())) {
                errors.add("C4 卡片 " + card.getId() + " 缺少一句话说明（site-map.yaml 未配置 summary）");
            }
            if (!StringUtils.hasText(card.getRoute())) {
                errors.add("C5 卡片 " + card.getId() + " 没有 route，无法跳转");
            }
        }

        // C5 跳转路由唯一：两张卡指向同一个页面会让"去这里"产生歧义
        Set<String> routes = new HashSet<>();
        for (GuideCardVO card : snapshot.getCards().values()) {
            if (StringUtils.hasText(card.getRoute()) && !routes.add(card.getRoute())) {
                errors.add("C5 路由重复：" + card.getRoute() + "（卡片 " + card.getId() + "）");
            }
        }

        return errors;
    }
}
