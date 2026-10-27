package com.ylf.cms.guide.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class GuideChatRequest {

    @NotBlank(message = "请输入你的问题")
    @Size(max = 200, message = "问题太长了，请控制在 200 字以内")
    private String message;
}
