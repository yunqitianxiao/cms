package com.ylf.cms.guide.sitemap;

import com.ylf.cms.guide.model.GuideCardVO;
import com.ylf.cms.guide.model.GuideCopy;
import com.ylf.cms.guide.model.SiteMapSnapshot;
import com.ylf.cms.guide.model.SiteMapYaml;
import com.ylf.cms.module.user.entity.SysPermission;
import com.ylf.cms.module.user.mapper.SysPermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 站点地图的持有者。
 *
 * 初始化时机放在 ApplicationReadyEvent：此时所有 Controller 都已注册、数据库连接可用，
 * 才能同时做"反射采集"和"权限码存在性校验"。
 *
 * 校验不通过就抛异常让进程退出。这是有意的：一个"不知道某个页面存在"的引导智能体
 * 比没有智能体更糟，因为它会自信地告诉用户"没有这个功能"。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SiteMapHolder {

    private final SiteMapCollector collector;
    private final SysPermissionMapper permissionMapper;

    private volatile SiteMapSnapshot snapshot = new SiteMapSnapshot();

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        SiteMapYaml yaml = loadYaml();
        SiteMapSnapshot collected = collector.collect(yaml);

        Set<String> knownPerms = permissionMapper.selectList(null).stream()
                .map(SysPermission::getPermCode)
                .collect(Collectors.toSet());

        List<String> errors = SiteMapValidator.validate(collected, knownPerms, yaml.getCards().keySet());
        if (!errors.isEmpty()) {
            throw new IllegalStateException("站点地图校验失败（服务拒绝启动）：\n  - "
                    + String.join("\n  - ", errors));
        }
        this.snapshot = collected;
        log.info("站点地图校验通过，共 {} 张卡片", collected.getCards().size());
    }

    public List<GuideCardVO> cards() {
        return new ArrayList<>(snapshot.getCards().values());
    }

    public GuideCardVO card(String id) {
        return snapshot.getCards().get(id);
    }

    public SiteMapSnapshot snapshot() {
        return snapshot;
    }

    /** 权限码 → 权限中文名，用于"缺少 XX 权限"这类提示 */
    public Map<String, String> permissionNames() {
        Map<String, String> map = new LinkedHashMap<>();
        for (SysPermission p : permissionMapper.selectList(null)) {
            map.put(p.getPermCode(), p.getPermName());
        }
        return map;
    }

    /**
     * 用 SafeConstructor 解析：site-map.yaml 是我们自己的文件，但"能安全解析就别用不安全的方式"——
     * 默认构造器允许 YAML 里的 tag 实例化任意类，这是已知的反序列化入口。
     */
    private SiteMapYaml loadYaml() {
        SiteMapYaml result = new SiteMapYaml();
        try (InputStream in = new ClassPathResource("guide/site-map.yaml").getInputStream()) {
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Object rootObj = yaml.load(in);
            if (!(rootObj instanceof Map)) {
                throw new IllegalStateException("site-map.yaml 顶层必须是 mapping");
            }
            Object cardsObj = ((Map<?, ?>) rootObj).get("cards");
            if (!(cardsObj instanceof Map)) {
                throw new IllegalStateException("site-map.yaml 缺少 cards 节点");
            }
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) cardsObj).entrySet()) {
                Map<?, ?> node = (Map<?, ?>) entry.getValue();
                GuideCopy copy = new GuideCopy();
                copy.setSummary(asString(node.get("summary")));
                copy.setSteps(asList(node.get("steps")));
                copy.setTips(asList(node.get("tips")));
                copy.setHighlight(asString(node.get("highlight")));
                result.getCards().put(String.valueOf(entry.getKey()), copy);
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("读取 guide/site-map.yaml 失败：" + e.getMessage(), e);
        }
        return result;
    }

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private List<String> asList(Object o) {
        List<String> list = new ArrayList<>();
        if (o instanceof List) {
            for (Object item : (List<?>) o) {
                list.add(String.valueOf(item));
            }
        }
        return list;
    }
}
