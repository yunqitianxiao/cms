package com.ylf.cms.guide.controller;

import com.ylf.cms.common.annotation.OperLog;
import com.ylf.cms.common.annotation.RequirePermission;
import com.ylf.cms.common.constant.LogType;
import com.ylf.cms.common.constant.Perm;
import com.ylf.cms.common.result.Result;
import com.ylf.cms.guide.annotation.GuideCard;
import com.ylf.cms.guide.model.GuideCardVO;
import com.ylf.cms.guide.model.GuideChatRequest;
import com.ylf.cms.guide.model.GuideChatResponse;
import com.ylf.cms.guide.model.GuideSuggestion;
import com.ylf.cms.guide.model.LlmSettingsRequest;
import com.ylf.cms.guide.model.LlmSettingsVO;
import com.ylf.cms.guide.model.LlmTestResult;
import com.ylf.cms.guide.service.GuideLlmService;
import com.ylf.cms.guide.service.GuideService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/guide")
@RequiredArgsConstructor
// page = false：引导助手自己不是"页面"，不出卡片；标注它是为了让 C2 知道
// 这个受控 Controller 不是被漏掉的模块，而不是让校验为它破例。
@GuideCard(id = "guide.assistant", title = "站内引导", group = "系统",
        route = "/", page = false)
public class GuideController {

    private final GuideService guideService;
    private final GuideLlmService guideLlmService;

    /** 全站页面卡片（按当前用户权限标记可用性） */
    @GetMapping("/pages")
    @RequirePermission(Perm.GUIDE_USE)
    public Result<List<GuideCardVO>> pages() {
        return Result.ok(guideService.pages());
    }

    @PostMapping("/chat")
    @RequirePermission(Perm.GUIDE_USE)
    public Result<GuideChatResponse> chat(@Valid @RequestBody GuideChatRequest request) {
        return Result.ok(guideService.chat(request.getMessage()));
    }

    @GetMapping("/suggestions")
    @RequirePermission(Perm.GUIDE_USE)
    public Result<List<GuideSuggestion>> suggestions() {
        return Result.ok(guideService.suggestions());
    }

    /** 模型通道状态：只回显"配没配、尾号"，不回显密钥 */
    @GetMapping("/llm")
    @RequirePermission(Perm.GUIDE_USE)
    public Result<LlmSettingsVO> llmSettings() {
        return Result.ok(guideLlmService.settings());
    }

    /** 用户填写自己的密钥，只存进当前会话（不落库、不进日志明文） */
    @PutMapping("/llm")
    @RequirePermission(Perm.GUIDE_USE)
    @OperLog(type = LogType.AI_QUERY, value = "配置模型密钥")
    public Result<LlmSettingsVO> saveLlmKey(@Valid @RequestBody LlmSettingsRequest request) {
        return Result.ok(guideLlmService.save(request.getApiKey()));
    }

    @DeleteMapping("/llm")
    @RequirePermission(Perm.GUIDE_USE)
    @OperLog(type = LogType.AI_QUERY, value = "清除模型密钥")
    public Result<LlmSettingsVO> clearLlmKey() {
        return Result.ok(guideLlmService.clear());
    }

    /** 连通性探测，失败也返回 code=0，由 data.ok 表达结果，避免前端把诊断当异常 */
    @PostMapping("/llm/test")
    @RequirePermission(Perm.GUIDE_USE)
    public Result<LlmTestResult> testLlm() {
        return Result.ok(guideLlmService.test());
    }

    /** 自检：站点地图是否完整覆盖了所有受控接口 */
    @GetMapping("/selfcheck")
    @RequirePermission(Perm.LOG_READ)
    public Result<Map<String, Object>> selfcheck() {
        return Result.ok(guideService.selfcheck());
    }
}
