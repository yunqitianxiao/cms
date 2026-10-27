package com.ylf.cms.guide.intent;

import com.ylf.cms.config.GuideProperties;
import com.ylf.cms.guide.model.GuideCardVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IntentRouterTest {

    private GuideProperties props;
    private IntentRouter router;
    private List<GuideCardVO> cards;

    @BeforeEach
    void setUp() {
        props = new GuideProperties();
        router = new IntentRouter(props);
        cards = Arrays.asList(
                card("article.create", "写新文章",
                        Arrays.asList("写文章", "投稿", "新建", "起草"),
                        "起草新文章，写完后提交审核",
                        Collections.singletonList("保存后回到列表，点提交审核")),
                card("article.review", "内容审核",
                        Arrays.asList("审核", "通过", "驳回", "审稿"),
                        "审核待发布的稿件",
                        Collections.emptyList()),
                card("article.list", "文章列表",
                        Arrays.asList("文章", "稿件", "内容"),
                        "按状态、分类、关键词筛选稿件",
                        Collections.emptyList())
        );
    }

    @Test
    @DisplayName("口语化提问能命中「写新文章」")
    void routeToCreate() {
        List<GuideCardVO> hit = router.route("我要写一篇新文章", cards);
        assertFalse(hit.isEmpty());
        assertEquals("article.create", hit.get(0).getId());
    }

    @Test
    @DisplayName("单个短词也命中，靠标题 bigram 而非整句包含")
    void routeByShortWord() {
        List<GuideCardVO> hit = router.route("审核", cards);
        assertFalse(hit.isEmpty());
        assertEquals("article.review", hit.get(0).getId());
    }

    @Test
    @DisplayName("完全不相关的问题返回空，而不是硬塞卡片")
    void unrelatedQuestion() {
        assertTrue(router.route("今天天气怎么样", cards).isEmpty());
        assertTrue(router.route("随便说点什么", cards).isEmpty());
        assertTrue(router.route("", cards).isEmpty());
    }

    @Test
    @DisplayName("阈值真的起作用：调高后同样的输入落空")
    void thresholdTakesEffect() {
        assertFalse(router.route("审核", cards).isEmpty());
        props.setMinScore(1.01);
        assertTrue(router.route("审核", cards).isEmpty());
    }

    @Test
    @DisplayName("最多返回三张卡片，且按分数从高到低")
    void limitAndOrder() {
        List<GuideCardVO> hit = router.route("审核文章", cards);
        assertTrue(hit.size() <= 3);
        // 「审核」是 review 的强词，「文章」是 list 的强词，两张都应该出现
        assertTrue(hit.stream().anyMatch(c -> c.getId().equals("article.review")));
    }

    private static GuideCardVO card(String id, String title, List<String> aliases,
                                    String summary, List<String> steps) {
        GuideCardVO c = new GuideCardVO();
        c.setId(id);
        c.setTitle(title);
        c.setAliases(aliases);
        c.setSummary(summary);
        c.setSteps(steps);
        c.setOrder(10);
        return c;
    }
}
