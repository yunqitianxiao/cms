package com.ylf.cms.guide.sitemap;

import com.ylf.cms.common.annotation.RequirePermission;
import com.ylf.cms.guide.annotation.GuideCard;
import com.ylf.cms.guide.model.GuideCardVO;
import com.ylf.cms.guide.model.GuideCopy;
import com.ylf.cms.guide.model.SiteMapSnapshot;
import com.ylf.cms.guide.model.SiteMapYaml;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 站点地图采集器：反射扫描全部 Controller 映射，把"页面"变成数据。
 *
 * 这是"智能体知道网站结构"的实现基础。它不读文档、不爬前端，
 * 读的是 Spring MVC 在启动时已经注册好的映射表——也就是框架真正会路由的那份数据。
 * 所以只要页面存在，它就一定知道；不存在的东西它也无从编造。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SiteMapCollector {

    private final ApplicationContext applicationContext;

    public SiteMapSnapshot collect(SiteMapYaml yaml) {
        // 用 ApplicationContext 按名字取，而不是构造器注入：
        // 直接注入 RequestMappingHandlerMapping 会触发 MVC 基础设施提前初始化，
        // 在某些配置下会造成循环依赖。
        RequestMappingHandlerMapping mapping =
                applicationContext.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);

        SiteMapSnapshot snapshot = new SiteMapSnapshot();
        Set<String> seenIds = new HashSet<>();
        Set<String> classesWithPermission = new HashSet<>();
        Set<String> classesWithCard = new HashSet<>();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : mapping.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = entry.getKey();
            HandlerMethod handler = entry.getValue();
            String className = handler.getBeanType().getSimpleName();

            GuideCard card = handler.getMethodAnnotation(GuideCard.class);
            if (card == null) {
                card = AnnotatedElementUtils.findMergedAnnotation(handler.getBeanType(), GuideCard.class);
            }
            RequirePermission permission = handler.getMethodAnnotation(RequirePermission.class);
            if (permission != null) {
                classesWithPermission.add(className);
            }
            if (card != null) {
                classesWithCard.add(className);
            }

            if (card == null) {
                continue;
            }
            if (!card.page()) {
                continue;
            }
            if (!seenIds.add(card.id())) {
                snapshot.getDuplicateIds().add(card.id());
                continue;
            }

            GuideCardVO vo = new GuideCardVO();
            vo.setId(card.id());
            vo.setTitle(card.title());
            vo.setGroup(card.group());
            vo.setRoute(card.route());
            vo.setOrder(card.order());
            vo.setAliases(new ArrayList<>(List.of(card.aliases())));
            vo.setApiPath(firstPattern(info));
            vo.setHttpMethod(firstMethod(info));
            if (permission != null) {
                vo.getPermissions().addAll(List.of(permission.value()));
            }

            GuideCopy copy = yaml.getCards().get(card.id());
            if (copy != null) {
                vo.setSummary(copy.getSummary());
                vo.setSteps(copy.getSteps());
                vo.setTips(copy.getTips());
                vo.setHighlight(copy.getHighlight());
            }
            snapshot.getCards().put(card.id(), vo);
        }

        // 受控 Controller 但一张卡片都没有 → 整个模块对智能体是黑箱
        classesWithPermission.stream()
                .filter(c -> !classesWithCard.contains(c))
                .sorted()
                .forEach(snapshot.getUncoveredPermittedMethods()::add);

        log.info("站点地图采集完成：{} 张卡片，{} 个未覆盖 Controller",
                snapshot.getCards().size(), snapshot.getUncoveredPermittedMethods().size());
        return snapshot;
    }

    /**
     * 一个处理方法可能映射多个路径（如 /api/users 和 /api/users/）。
     * 取不含路径变量的那个作为"这张卡代表哪个接口"，没有就取字典序第一个，保证结果稳定。
     */
    private String firstPattern(RequestMappingInfo info) {
        if (info.getPatternsCondition() == null) {
            return "";
        }
        Set<String> patterns = new TreeSet<>(info.getPatternsCondition().getPatterns());
        for (String p : patterns) {
            if (!p.contains("{")) {
                return p;
            }
        }
        return patterns.isEmpty() ? "" : patterns.iterator().next();
    }

    private String firstMethod(RequestMappingInfo info) {
        if (info.getMethodsCondition() == null || info.getMethodsCondition().getMethods().isEmpty()) {
            return "ANY";
        }
        return info.getMethodsCondition().getMethods().iterator().next().name();
    }
}
