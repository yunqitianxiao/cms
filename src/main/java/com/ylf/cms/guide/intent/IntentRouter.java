package com.ylf.cms.guide.intent;

import com.ylf.cms.config.GuideProperties;
import com.ylf.cms.guide.model.GuideCardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * L0 规则意图路由：不联网、不依赖模型，永远可用。
 *
 * 打分方式：
 *   - 标题和别名里的 bigram 权重 2（"页面叫什么"比"页面能干什么"更能代表意图）
 *   - 摘要/步骤/提示里的 bigram 权重 1
 *   - 得分 = 命中权重之和 / (2 × 查询 bigram 数)，落在 [0,1]
 *   - 查询里直接包含标题或别名时给强信号 0.8（短词、口语化表达靠这条兜住）
 *
 * 页面数是十几个，每次直接算比维护倒排索引更简单；
 * 如果页面涨到几百个，再把这层换成倒排索引，接口不变。
 */
@Component
@RequiredArgsConstructor
public class IntentRouter {

    private final GuideProperties properties;

    public List<GuideCardVO> route(String message, List<GuideCardVO> cards) {
        String normalized = TextNormalizer.normalize(message);
        List<String> queryBigrams = TextNormalizer.bigrams(normalized);
        if (queryBigrams.isEmpty()) {
            return new ArrayList<>();
        }
        return cards.stream()
                .map(card -> new Scored(card, score(normalized, queryBigrams, card)))
                .filter(s -> s.score >= properties.getMinScore())
                .sorted(Comparator.comparingDouble((Scored s) -> s.score).reversed()
                        .thenComparingInt(s -> s.card.getOrder()))
                .limit(3)
                .map(s -> s.card)
                .collect(Collectors.toList());
    }

    double score(String normalizedQuery, List<String> queryBigrams, GuideCardVO card) {
        Set<String> strong = bigramsOf(join(card.getTitle(), card.getAliases()));
        Set<String> weak = bigramsOf(join(card.getSummary(), card.getSteps(), card.getTips()));

        int hits = 0;
        for (String bigram : queryBigrams) {
            if (strong.contains(bigram)) {
                hits += 2;
            } else if (weak.contains(bigram)) {
                hits += 1;
            }
        }
        double score = (double) hits / (2.0 * queryBigrams.size());

        // 包含匹配给强信号，但只认 3 个字以上的标题/别名。
        // 两字别名（"文章""内容"）会出现在几乎任何一句话里，用它加分会让
        // "我要写一篇新文章" 命中"文章列表"而不是"写新文章"。
        if (containsKey(normalizedQuery, card.getTitle())
                || card.getAliases().stream().anyMatch(a -> containsKey(normalizedQuery, a))) {
            score = Math.max(score, 0.8);
        }
        return Math.min(score, 1.0);
    }

    private static boolean containsKey(String normalizedQuery, String word) {
        String w = TextNormalizer.normalize(word);
        return w.length() >= 3 && normalizedQuery.contains(w);
    }

    private Set<String> bigramsOf(String text) {
        return new HashSet<>(TextNormalizer.bigrams(TextNormalizer.normalize(text)));
    }

    @SafeVarargs
    private String join(String first, List<String>... rests) {
        StringBuilder sb = new StringBuilder(first == null ? "" : first);
        for (List<String> rest : rests) {
            if (rest != null) {
                rest.forEach(sb::append);
            }
        }
        return sb.toString();
    }

    private static final class Scored {
        private final GuideCardVO card;
        private final double score;

        private Scored(GuideCardVO card, double score) {
            this.card = card;
            this.score = score;
        }
    }
}
