package com.freelycar.saas.wxutils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.project.entity.Ark;
import com.freelycar.saas.project.entity.Store;
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-10-21
 * Time: 15:27
 */
public class CustomMessage {
    private static final Logger log = LogManager.getLogger(CustomMessage.class);

    private static String invokeTemplateMessage(JSONObject params) {
        //解决中文乱码问题
        StringEntity entity = new StringEntity(params.toString(), "utf-8");
        String result = HttpRequest.postCall(WechatConfig.WECHAT_CUSTOM_MESSAGE_URL +
                        WechatConfig.getAccessTokenForInteface().getString("access_token"),
                entity, null);
        log.debug("微信客服消息结果：" + result);
        return result;
    }

    /**
     * 用户扫码客服消息推送
     *
     * @param ark
     * @param openId
     */
    public static void remindToOrder(Ark ark, String openId) {
        Long currentTime = System.currentTimeMillis() / 1000;
        String url = "https://www.freelycar.com/wechat/role-select/" + ark.getSn() + "?createTime=" + currentTime;
        String arkLocation = ark.getLocation();
        JSONObject params = new JSONObject();
        params.put("touser", openId);
        params.put("msgtype", "news");
        JSONObject news = new JSONObject();
        JSONArray articles = new JSONArray();
        JSONObject article = new JSONObject();
        article.put("title", "点击下单");
        article.put("description", arkLocation);
        article.put("url", url);
        article.put("picurl", "https://www.freelycar.com/upload/headimg/logo.jpg");
        articles.add(article);
        news.put("articles", articles);
        params.put("news", news);
        String result = invokeTemplateMessage(params);
        log.info("微信订单更新模版消息结果：" + result);
    }
}
