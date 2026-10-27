package com.ylf.cms.module.portal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 页面路由。所有管理端页面共用一个模板命名规则 /app/{page}，
 * page 走白名单，否则就成了模板路径穿越（/app/../../etc/passwd 这类尝试）。
 */
@Controller
public class PageController {

    private static final Set<String> APP_PAGES = new HashSet<>(Arrays.asList(
            "dashboard", "articles", "article-edit", "review",
            "categories", "users", "roles", "logs"
    ));

    @GetMapping({"/", "/app", "/app/"})
    public String index() {
        return "redirect:/app/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/app/{page}")
    public String appPage(@org.springframework.web.bind.annotation.PathVariable String page) {
        if (!APP_PAGES.contains(page)) {
            return "redirect:/app/dashboard";
        }
        return "app/" + page;
    }

    @GetMapping({"/portal", "/portal/"})
    public String portal() {
        return "portal/list";
    }

    @GetMapping("/portal/article/{id}")
    public String portalDetail(@org.springframework.web.bind.annotation.PathVariable Long id,
                               org.springframework.ui.Model model) {
        model.addAttribute("articleId", id);
        return "portal/detail";
    }
}
