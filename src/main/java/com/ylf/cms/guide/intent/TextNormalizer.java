package com.ylf.cms.guide.intent;

import java.util.ArrayList;
import java.util.List;

/**
 * 中文意图匹配用的文本归一化与 bigram 切分。
 *
 * 不引分词库：这类查询是短句 + 固定名词（"用户管理"、"发布文章"），
 * 引入 IK 要多一份词典和依赖，收益不明显。bigram 对中文短句的召回够用，
 * 代价是会把"文章"和"文明"混在一起，所以还要靠别名和阈值兜住。
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /** 只保留汉字、字母、数字，统一小写；标点和空白全部丢掉 */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : text.toLowerCase().toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static List<String> bigrams(String normalized) {
        List<String> result = new ArrayList<>();
        if (normalized.length() < 2) {
            return result;
        }
        for (int i = 0; i + 1 < normalized.length(); i++) {
            result.add(normalized.substring(i, i + 2));
        }
        return result;
    }
}
