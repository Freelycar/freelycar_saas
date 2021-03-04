package com.freelycar.saas.wxutils;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: pyt
 * @Date: 2021/3/1 17:17
 * @Description:
 */
@Component
public class MiniProgramUtils {
    private static Logger logger = LoggerFactory.getLogger(MiniProgramUtils.class);
    //登录code换取openid
    public static final String JSCODE_TO_SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private MiniProgramConfig config;

    //缓存accessToken
    public static Map<String, JSONObject> cacheVariable = new ConcurrentHashMap<>();
    //设置token的过期时间 为一个半小时
    private final static int TIME_OUT = 5400 * 1000;

    /**
     * 小程序登录
     * code换取用户信息
     *
     * @param code
     * @return {"unionid":"oBz9p024nr43V049h6VPu4BwraPQ",
     * "openid":"oL_VK4zD3sjwBzhzVcHLp1HghS-M",
     * "session_key":"FpIuP6cSz9AW7S1Ynkd2Ow=="}
     * @throws RuntimeException
     */
    public JSONObject code2Session(String code) throws RuntimeException {
        String url = JSCODE_TO_SESSION_URL + "?appid=" + config.getMiniAppId() + "&secret="
                + config.getMiniAppSecret() + "&js_code=" + code
                + "&grant_type=authorization_code";
        String userInfo = HttpRequest.getCall(url, null, null);

        JSONObject resultObject = null;
        try {
            resultObject = JSONObject.parseObject(userInfo);
        } catch (JSONException e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            throw new RuntimeException("MiniProgramConfig#获取的json字符串解析失败", e);
        }
        return resultObject;
    }

    /**
     * 获取小程序全局唯一后台接口调用凭据（access_token）
     * @return
     */
    public JSONObject getAccessTokenForInteface() {
        JSONObject tokenJSON = cacheVariable.get("itoken");
        if (tokenJSON != null && (System.currentTimeMillis() - tokenJSON.getLong("get_time")) < TIME_OUT) {
            logger.debug("从缓存中拿的itoken:" + tokenJSON.getString("access_token"));
            return tokenJSON;
        }
        String tokenUrl = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                + config.getMiniAppId() + "&secret=" + config.getMiniAppSecret();
        String call = HttpRequest.getCall(tokenUrl, null, null);
        JSONObject obj = null;
        try {
            obj = JSONObject.parseObject(call);
            obj.put("get_time", System.currentTimeMillis());//此处设置获取时间，用于比对过期时间
        } catch (JSONException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
        cacheVariable.put("itoken", obj);
        logger.debug("新获取的itoken:" + obj.getString("access_token"));
        return obj;
    }
}
