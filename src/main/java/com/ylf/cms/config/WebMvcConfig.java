package com.ylf.cms.config;

import com.ylf.cms.auth.LoginInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**", "/app/**")
                // 登录、注册、前台浏览不需要登录态
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/portal/**"
                );
    }
}
