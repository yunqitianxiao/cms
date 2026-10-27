package com.ylf.cms.guide.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class LlmSettingsRequest {

    @NotBlank(message = "密钥不能为空")
    @Size(max = 200, message = "密钥长度不能超过 200")
    private String apiKey;
}
