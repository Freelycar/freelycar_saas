package com.freelycar.saas.wxutils;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import org.apache.http.HttpEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author puyuting
 * @date 2019/12/20
 * @email 2630451673@qq.com
 */
@Component
public class LeanCloudUtils {

    private final String ContentType = "application/json";
    @Value("${leancloud.app.id}")
    private String appid;
    @Value("${leancloud.app.key}")
    private String appkey;
    @Value("${leancloud.url.res}")
    private String leancloudUrlRes;
    @Value("${leancloud.url.ver}")
    private String leancloudUrlVer;

    private Logger log = LogManager.getLogger(LeanCloudUtils.class);

    private Map<String, Object> setLeancloudHead() {
        Map<String, Object> head = new HashMap<String, Object>();
        head.put("X-LC-Id", appid);
        head.put("X-LC-Key", appkey);
        head.put("Content-Type", ContentType);
        return head;
    }

    public ResultJsonObject getVerification(String phone, boolean isVoice) {
        JSONObject param = new JSONObject();
        param.put("mobilePhoneNumber", phone);
        if (isVoice) {
            param.put("smsType", "voice");
        }
        HttpEntity entity = HttpRequest.getEntity(param);
        Map<String, Object> head = setLeancloudHead();
        String result = HttpRequest.postCall(leancloudUrlRes, entity, head);
        log.error("leancloud的返回码：" + result);
        JSONObject json;
        try {
            json = JSONObject.parseObject(result);
        } catch (JSONException e) {
            e.printStackTrace();
            log.error("发送验证码,解析返回结果错误");
            return ResultJsonObject.getErrorResult(null, "解析返回结果错误");
        }

        if (StringUtils.hasText(json.getString("error"))) {
            return ResultJsonObject.getErrorResult(json, json.getString("error"));
        }
        return ResultJsonObject.getDefaultResult(json);
    }

    public ResultJsonObject sendTemplate(String phone, Integer password, String link) {
        JSONObject param = new JSONObject();
        param.put("mobilePhoneNumber", phone);
        param.put("template", "通知e代驾");
        param.put("link", link);
        param.put("sign", "小易爱车");
        param.put("password", password);
        HttpEntity entity = HttpRequest.getEntity(param);
        Map<String, Object> head = setLeancloudHead();
        String result = HttpRequest.postCall(leancloudUrlRes, entity, head);
        log.error("leancloud的返回码：" + result);
        JSONObject json;
        try {
            json = JSONObject.parseObject(result);
        } catch (JSONException e) {
            e.printStackTrace();
            log.error("发送短信模板,解析返回结果错误");
            return ResultJsonObject.getErrorResult(null, "解析返回结果错误");
        }

        if (StringUtils.hasText(json.getString("error"))) {
            return ResultJsonObject.getErrorResult(json, json.getString("error"));
        }
        return ResultJsonObject.getDefaultResult(json);
    }

    public ResultJsonObject sendVerifyCode(String phone, String verifyCode) {
        JSONObject param = new JSONObject();
        param.put("mobilePhoneNumber", phone);
        param.put("template", "e代驾目的地验证码");
        param.put("sign", "小易爱车");
        param.put("code", verifyCode);
        HttpEntity entity = HttpRequest.getEntity(param);
        Map<String, Object> head = setLeancloudHead();
        String result = HttpRequest.postCall(leancloudUrlRes, entity, head);
        log.error("leancloud的返回码：" + result);
        JSONObject json;
        try {
            json = JSONObject.parseObject(result);
        } catch (JSONException e) {
            e.printStackTrace();
            log.error("发送短信模板,解析返回结果错误");
            return ResultJsonObject.getErrorResult(null, "解析返回结果错误");
        }

        if (StringUtils.hasText(json.getString("error"))) {
            return ResultJsonObject.getErrorResult(json, json.getString("error"));
        }
        return ResultJsonObject.getDefaultResult(json);
    }

    public Integer getPassword() {
        int min = 10000000;
        int max = 99999999;
        return  min + (int)(Math.random() * ((max - min) + 1));
    }

}
