package com.freelycar.saas.wxutils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @Author: pyt
 * @Date: 2021/3/1 16:22
 * @Description: 小程序配置
 */
@Configuration
@ConfigurationProperties(prefix = "miniprogram.app")
@PropertySource(value = "classpath:miniprogram.yml")
public class MiniProgramConfig {
    @Value("${id}")
    private String miniAppId;
    @Value("${secret}")
    private String miniAppSecret;

    public String getMiniAppId() {
        return miniAppId;
    }

    public String getMiniAppSecret() {
        return miniAppSecret;
    }

    public void setMiniAppId(String miniAppId) {
        this.miniAppId = miniAppId;
    }

    public void setMiniAppSecret(String miniAppSecret) {
        this.miniAppSecret = miniAppSecret;
    }
}
