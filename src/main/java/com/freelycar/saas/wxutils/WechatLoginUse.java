package com.freelycar.saas.wxutils;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.exception.WeChatException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StringUtils;

public class WechatLoginUse {

    private static Logger log = LogManager.getLogger(WechatLoginUse.class);

    public static JSONObject wechatInfo(String code) throws WeChatException {
        //返回前台
        JSONObject wechatInfo = new JSONObject();

        //access token
        JSONObject resultJson = WechatConfig.getAccessToken(code);
        String openid = resultJson.getString("openid");
        String accessToken = resultJson.getString("access_token");
        log.debug("获取登陆时access_token: " + accessToken + " ; openid: " + openid);

        if (StringUtils.isEmpty(openid) || StringUtils.isEmpty(accessToken)) {
            log.error(resultJson);
            throw new WeChatException(resultJson.toJSONString());
        }

        // 获取微信用户信息
        JSONObject userInfoJson = WechatConfig.getWXUserInfo(accessToken, openid);

        //判断获取信息是否包含错误码
        String errCode = userInfoJson.getString("errcode");
        if (StringUtils.hasText(errCode)) throw new WeChatException("微信接口获取用户信息失败，微信接口返回信息：" + userInfoJson);

        //获取是否关注了公众号
        boolean subscribe = WechatConfig.isUserFollow(openid);
        String nickName = userInfoJson.getString("nickname");
        String gender;

        switch (userInfoJson.getInteger("sex")) {
            case 1:
                gender = "男";
                break;
            case 2:
                gender = "女";
                break;
            default:
                gender = "未知";
        }

        String headImgUrl = userInfoJson.getString("headimgurl");
        String province = userInfoJson.getString("province");
        String city = userInfoJson.getString("city");

        wechatInfo.put("subscribe", subscribe);
        wechatInfo.put("openid", openid);
        wechatInfo.put("nickname", nickName);
        wechatInfo.put("gender", gender);
        wechatInfo.put("headimgurl", headImgUrl);
        wechatInfo.put("province", province);
        wechatInfo.put("city", city);

        return wechatInfo;

    }
}
