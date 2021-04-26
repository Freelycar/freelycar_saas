package com.freelycar.saas.wxutils;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.wechat.model.OrderChangedMessage;
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: pyt
 * @Date: 2021/4/25 15:12
 * @Description: 微信小程序消息
 */
public class MiniMessage {
    //小程序订阅消息推送
    private static final String SUBSCRIBE_MESSAGE_SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=";
    //小程序订阅消息模板ID
    private static final String SUBSCRIBE_MESSAGE_ID = "L3944tN8k-tZCsJP5hXVyNnjcDem250SemJ0aJ8JkeQ";
    //小程序统一模板消息推送
    private static final String UNIFORM_MESSAGE_SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/wxopen/template/uniform_send?access_token=";
    //公众号订单状态改变模板ID
    private static final String ORDER_CHANGED_ID = "PeRe0M0iEbm7TpN6NOThhBUjwzy_aHsi6r2E7Pa8J1A";

    private static final Logger log = LogManager.getLogger(WechatTemplateMessage.class);

    private static String invokeMessage(String url, String accessToken, JSONObject params) {
        //解决中文乱码问题
        StringEntity entity = new StringEntity(params.toString(), "utf-8");
        String result = HttpRequest.postCall(url + "" + accessToken, entity, null);
        log.debug("微信模版消息结果：" + result);
        return result;
    }

    //推送订阅消息
    public static boolean sendSubscribeMessage(OrderChangedMessage orderChangedMessage) {
        JSONObject params = new JSONObject();
        params.put("touser", orderChangedMessage.getUserOpenId());
        params.put("template_id", SUBSCRIBE_MESSAGE_ID);
        params.put("page", "pages/components/index/main");
        //todo 线上版本需要切换
        //跳转小程序类型：developer为开发版；trial为体验版；formal为正式版；默认为正式版
        params.put("miniprogram_state", "trial");
        params.put("lang", "zh_CN");

        JSONObject data = new JSONObject();
        data.put("character_string9", JSONObject.parseObject("{\"value\":\"" + orderChangedMessage.getOrderId() + "\"}"));
        data.put("thing1", JSONObject.parseObject("{\"value\":\"" + orderChangedMessage.getProjects() + "\"}"));
        data.put("thing2", JSONObject.parseObject("{\"value\":\"" + orderChangedMessage.getOrderState() + "\"}"));
        data.put("thing6", JSONObject.parseObject("{\"value\":\"" + orderChangedMessage.getComment() + "\"}"));
        params.put("data", data);
        String result = invokeMessage(SUBSCRIBE_MESSAGE_SEND_URL, orderChangedMessage.getAccessToken(), params);
        log.info("微信模版消息结果：" + result);
        JSONObject res = JSONObject.parseObject(result);
        return res.getInteger("errcode") == 0;
    }

    public static boolean sendUniformMessage(OrderChangedMessage orderChangedMessage) {
        JSONObject params = new JSONObject();
        params.put("touser", orderChangedMessage.getUserOpenId());

        JSONObject mp_template_msg = new JSONObject();
        mp_template_msg.put("appid", WechatConfig.APP_ID);
        mp_template_msg.put("template_id", ORDER_CHANGED_ID);

        JSONObject miniprogram = new JSONObject();
        miniprogram.put("appid", WechatConfig.MINI_ID);
        miniprogram.put("path", "pages/components/index/main");
        mp_template_msg.put("miniprogram", miniprogram);
        mp_template_msg.put("url", "");

        JSONObject data = new JSONObject();
        data.put("first", JSONObject.parseObject("{\"value\":\"您的爱车正在服务中\"}"));
        data.put("OrderSn", JSONObject.parseObject("{\"value\":\"" + orderChangedMessage.getOrderId() + "\"}"));
        data.put("OrderStatus", JSONObject.parseObject("{\"value\":\"" + orderChangedMessage.getOrderState() + "\"}"));
        data.put("remark", JSONObject.parseObject("{\"value\":\"" + orderChangedMessage.getComment() + "\"}"));
        mp_template_msg.put("data", data);
        params.put("mp_template_msg", mp_template_msg);

        String result = invokeMessage(UNIFORM_MESSAGE_SEND_URL, orderChangedMessage.getAccessToken(), params);
        log.info("微信模版消息结果：" + result);
        JSONObject res = JSONObject.parseObject(result);
        return res.getInteger("errcode") == 0;
    }

    public static void main(String[] args) {
        OrderChangedMessage message = new OrderChangedMessage();
        message.setUserOpenId("oBaSqs857aKKQB1SSmxBgGtkHsVc");
        message.setOrderId("A0011905080002");
        message.setProjects("预约保养");
        message.setOrderState("预约成功");
        message.setComment("您已成功预约车辆保养，请在预约时间内进店");
        message.setAccessToken("44_IzmePtgf-PzvQ58aOOzEKXHzzmWETyheOedOz0fBHbheMcP4Rk3JlXgiP_EVU6kU1_O1TAdFA73IJwaFk1llvO6Q2Jwo1dzO2uxeTJNW4RhLv9X0kBSayV2ONyCS678xKgU2RU6xzK3z4d0_ZNOhAIAHVW");
        System.out.println(sendUniformMessage(message));
    }

}
