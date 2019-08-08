package com.freelycar.saas.wechat.controller;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.WeChatException;
import com.freelycar.saas.wxutils.MD5;
import com.freelycar.saas.wxutils.WechatConfig;
import com.freelycar.saas.wxutils.WechatLoginUse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * @author tangwei - Toby
 * @date 2019-01-31
 * @email toby911115@gmail.com
 */
@RestController
@RequestMapping("/wechat/config")
public class WeChatConfigController {
    private Logger logger = LoggerFactory.getLogger(WeChatConfigController.class);


    /**
     * 为微信前端提供调用JS-SDK所需的信息
     *
     * @return
     */
    @GetMapping(value = "/getJSSDKConfig")
    public ResultJsonObject getJsSDKConfig() {
        String noncestr = UUID.randomUUID().toString();
        JSONObject ticketJson = WechatConfig.getJsApiTicketByWX();
        String ticket = ticketJson.getString("ticket");
        String timestamp = String.valueOf(System.currentTimeMillis());

        // 对给定字符串key手动排序
        String param = "jsapi_ticket=" + ticket + "&noncestr=" + noncestr
                + "&timestamp=" + timestamp + "&url=" + WechatConfig.APP_DOMAIN;

        String signature = MD5.encode("SHA1", param);

        JSONObject jsSDKConfig = new JSONObject();

        jsSDKConfig.put("appId", WechatConfig.APP_ID);
        jsSDKConfig.put("nonceStr", noncestr);
        jsSDKConfig.put("timestamp", timestamp);
        jsSDKConfig.put("signature", signature);
        return ResultJsonObject.getDefaultResult(jsSDKConfig);
    }

    /**
     * 通过微信接口去获取微信用户信息
     *
     * @param code
     * @return
     */
    @GetMapping("/getWeChatUserInfo")
    public ResultJsonObject getWeChatUserInfo(@RequestParam String code) {
        // 获取微信用户信息
        JSONObject jsonObject;
        try {
            jsonObject = WechatLoginUse.wechatInfo(code);
        } catch (WeChatException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
        logger.info("通过微信接口去获取微信用户信息：");
        logger.info(jsonObject.toString());
        return ResultJsonObject.getDefaultResult(jsonObject);
    }
}
