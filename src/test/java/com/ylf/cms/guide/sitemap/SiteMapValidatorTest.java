package com.ylf.cms.guide.sitemap;

import com.ylf.cms.guide.model.GuideCardVO;
import com.ylf.cms.guide.model.SiteMapSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 每条校验规则都用"会失败"的输入验证一次：
 * 只证明合法输入不报错，等于没验证——把规则删掉测试照样绿。
 */
class SiteMapValidatorTest {

    private static final Set<String> PERMS = new HashSet<>(Arrays.asList("article:read", "guide:use"));

    @Test
    @DisplayName("完整的地图通过校验")
    void validSnapshot() {
        SiteMapSnapshot snapshot = snapshotOf(card("article.list", "/app/articles", "文章列表", "article:read"));
        List<String> errors = SiteMapValidator.validate(snapshot, PERMS, Collections.singleton("article.list"));
        assertEquals(Collections.emptyList(), errors);
    }

    @Test
    @DisplayName("C1 卡片 id 重复被拦下")
    void duplicateId() {
        SiteMapSnapshot snapshot = snapshotOf(card("article.list", "/app/a", "A", "article:read"));
        snapshot.setDuplicateIds(Collections.singletonList("article.list"));
        List<String> errors = SiteMapValidator.validate(snapshot, PERMS, Collections.emptySet());
        assertTrue(errors.stream().anyMatch(e -> e.startsWith("C1")));
    }

    @Test
    @DisplayName("C2 整个 Controller 没有卡片会被拦下")
    void uncoveredController() {
        SiteMapSnapshot snapshot = snapshotOf(card("article.list", "/app/articles", "文章列表", "article:read"));
        snapshot.setUncoveredPermittedMethods(Collections.singletonList("FakeController"));
        List<String> errors = SiteMapValidator.validate(snapshot, PERMS, Collections.emptySet());
        assertTrue(errors.stream().anyMatch(e -> e.startsWith("C2") && e.contains("FakeController")));
    }

    @Test
    @DisplayName("C3 引用不存在的权限码会被拦下")
    void unknownPermission() {
        SiteMapSnapshot snapshot = snapshotOf(card("article.list", "/app/articles", "文章列表", "article:typo"));
        List<String> errors = SiteMapValidator.validate(snapshot, PERMS, Collections.emptySet());
        assertTrue(errors.stream().anyMatch(e -> e.startsWith("C3") && e.contains("article:typo")));
    }

    @Test
    @DisplayName("C4 yaml 里凭空多出的 key 会被拦下")
    void yamlKeyWithoutCard() {
        SiteMapSnapshot snapshot = snapshotOf(card("article.list", "/app/articles", "文章列表", "article:read"));
        List<String> errors = SiteMapValidator.validate(snapshot, PERMS, Collections.singleton("ghost.page"));
        assertTrue(errors.stream().anyMatch(e -> e.startsWith("C4") && e.contains("ghost.page")));
    }

    @Test
    @DisplayName("C4 卡片缺少 summary 会被拦下")
    void missingSummary() {
        SiteMapSnapshot snapshot = snapshotOf(card("article.list", "/app/articles", "  ", "article:read"));
        List<String> errors = SiteMapValidator.validate(snapshot, PERMS, Collections.emptySet());
        assertTrue(errors.stream().anyMatch(e -> e.startsWith("C4") && e.contains("article.list")));
    }

    @Test
    @DisplayName("C5 route 为空会被拦下")
    void blankRoute() {
        SiteMapSnapshot snapshot = snapshotOf(card("article.list", "", "文章列表", "article:read"));
        List<String> errors = SiteMapValidator.validate(snapshot, PERMS, Collections.emptySet());
        assertTrue(errors.stream().anyMatch(e -> e.startsWith("C5")));
    }

    @Test
    @DisplayName("C5 两张卡指向同一路由会被拦下")
    void duplicateRoute() {
        SiteMapSnapshot snapshot = new SiteMapSnapshot();
        snapshot.getCards().put("a", card("a", "/app/same", "A", "article:read"));
        snapshot.getCards().put("b", card("b", "/app/same", "B", "article:read"));
        List<String> errors = SiteMapValidator.validate(snapshot, PERMS, Collections.emptySet());
        assertTrue(errors.stream().anyMatch(e -> e.startsWith("C5") && e.contains("/app/same")));
    }

    private static SiteMapSnapshot snapshotOf(GuideCardVO card) {
        SiteMapSnapshot snapshot = new SiteMapSnapshot();
        snapshot.getCards().put(card.getId(), card);
        return snapshot;
    }

    private static GuideCardVO card(String id, String route, String summary, String perm) {
        GuideCardVO card = new GuideCardVO();
        card.setId(id);
        card.setTitle(id);
        card.setRoute(route);
        card.setSummary(summary);
        card.getPermissions().add(perm);
        return card;
    }
}
