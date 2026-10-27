package com.ylf.cms.module.user.controller;

import com.ylf.cms.auth.LoginUser;
import com.ylf.cms.auth.UserContext;
import com.ylf.cms.common.annotation.OperLog;
import com.ylf.cms.common.constant.LogType;
import com.ylf.cms.common.constant.SessionKeys;
import com.ylf.cms.common.result.Result;
import com.ylf.cms.module.user.dto.LoginRequest;
import com.ylf.cms.module.user.dto.RegisterRequest;
import com.ylf.cms.module.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @OperLog(type = LogType.LOGIN, value = "登录")
    public Result<LoginUser> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        LoginUser user = authService.authenticate(request.getUsername(), request.getPassword());
        // 防会话固定：登录成功后换一个新 Session，旧 SessionId 作废
        HttpSession old = httpRequest.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        httpRequest.getSession(true).setAttribute(SessionKeys.LOGIN_USER_ID, user.getId());
        return Result.ok(user);
    }

    @PostMapping("/logout")
    @OperLog(type = LogType.LOGIN, value = "退出登录")
    public Result<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return Result.ok();
    }

    @PostMapping("/register")
    @OperLog(type = LogType.USER, value = "注册")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    /** 当前登录人，前端启动时用它决定侧边栏显示哪些菜单 */
    @GetMapping("/me")
    public Result<LoginUser> me() {
        return Result.ok(UserContext.require());
    }
}
