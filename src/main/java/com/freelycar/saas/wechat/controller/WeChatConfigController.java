package com.freelycar.saas.wechat.controller;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.WeChatException;
import com.freelycar.saas.project.entity.Ark;
import com.freelycar.saas.project.entity.Store;
import com.freelycar.saas.project.repository.ArkRepository;
import com.freelycar.saas.project.repository.StoreRepository;
import com.freelycar.saas.wechat.utils.MessageUtil;
import com.freelycar.saas.wechat.utils.WebChatUtil;
import com.freelycar.saas.wxutils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

    // 获取Ticket
    public final static String ticket_url = "https://api.weixin.qq.com/cgi-bin/qrcode/create?access_token=";
    //获取二维码
    public final static String erweima = "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=";
    private final static String MEDIATYPE_CHARSET_JSON_UTF8 = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8";

    private ArkRepository arkRepository;
    private StoreRepository storeRepository;

    @Autowired
    public void setArkRepository(ArkRepository arkRepository) {
        this.arkRepository = arkRepository;
    }

    @Autowired
    public void setStoreRepository(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    /**
     * 为微信前端提供调用JS-SDK所需的信息
     *
     * @return
     */
    @GetMapping(value = "/getJSSDKConfig")
    public ResultJsonObject getJsSDKConfig(@RequestParam(required = false) String targetUrl) {
        logger.debug("JSSDK Url:" + targetUrl);
        String errorMsg = null;
        if (StringUtils.isEmpty(targetUrl)) {
            errorMsg = "当前url为空，将采用默认url签名（注意，可能会提示 config:invalid signature）";
            logger.error(errorMsg);
            targetUrl = WechatConfig.APP_DOMAIN;
        }

        String noncestr = UUID.randomUUID().toString();
        JSONObject ticketJson = WechatConfig.getJsApiTicketByWX();
        String ticket = ticketJson.getString("ticket");
        String timestamp = String.valueOf(System.currentTimeMillis());

        int index = targetUrl.indexOf("#");
        if (index > 0) {
            targetUrl = targetUrl.substring(0, index);
        }

        // 对给定字符串key手动排序
        String param = "jsapi_ticket=" + ticket + "&noncestr=" + noncestr
                + "&timestamp=" + timestamp + "&url=" + targetUrl;

        String signature = MD5.encode("SHA1", param);

        JSONObject jsSDKConfig = new JSONObject();

        jsSDKConfig.put("appId", WechatConfig.APP_ID);
        jsSDKConfig.put("nonceStr", noncestr);
        jsSDKConfig.put("timestamp", timestamp);
        jsSDKConfig.put("signature", signature);
        if (StringUtils.hasText(errorMsg)) {
            ResultJsonObject.getDefaultResult(jsSDKConfig, errorMsg);
        }
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

    @PostMapping("/getTicket")
    public String getTicket(
            @RequestParam(name = "scene_str", required = true) String scene_str) {
        //access token
        String accessToken = WechatConfig.getAccessTokenForInteface().getString("access_token");
        //body
        String uu = "{\"action_name\": \"QR_LIMIT_STR_SCENE\", \"action_info\": {\"scene\": {\"scene_str\":" + scene_str + "}}}";
        String jsonObject = HttpRequest.postCall(ticket_url + accessToken, HttpRequest.getEntity(uu), null);
        String ticket = JSONObject.parseObject(jsonObject).getString("ticket");
        //二维码
        String ss = erweima + ticket;
        return ss;
    }

    @PostMapping("/getAccessToken")
    public JSONObject getAccessToken() {
        return WechatConfig.getAccessTokenForInteface();
    }

    @GetMapping(value = "/verify")
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        //消息来源可靠性验证
        String signature = request.getParameter("signature");// 微信加密签名
        String timestamp = request.getParameter("timestamp");// 时间戳
        String nonce = request.getParameter("nonce");       // 随机数
        String echostr = request.getParameter("echostr");//成为开发者验证
        //确认此次GET请求来自微信服务器，原样返回echostr参数内容，则接入生效，成为开发者成功，否则接入失败
        PrintWriter out = response.getWriter();
        if (WebChatUtil.checkSignature(signature, timestamp, nonce)) {
            logger.info("=======请求校验成功======{}", echostr);
            out.print(echostr);
        }
        out.close();
        out = null;
    }

    @PostMapping(value = "/verify")
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        Map<String, String> result = MessageUtil.parseXml(request);
        if (!StringUtils.isEmpty(result.get("Event"))
                && !StringUtils.isEmpty(result.get("FromUserName"))) {
            String openId = result.get("FromUserName");
            String eventKey = result.get("EventKey");
            if (result.get("Event").equals("subscribe")) {
                Map<String, String> sendMessage = new HashMap<>();
                sendMessage.put("ToUserName", openId);
                sendMessage.put("FromUserName", "gh_116309b21e24");
                sendMessage.put("CreateTime", String.valueOf(System.currentTimeMillis()).substring(0, 10));
                sendMessage.put("MsgType", "text");
                sendMessage.put("Content", "欢迎关注小易爱车！\n" +
                        "如果您是通过智能柜关注的我们，\n" +
                        "请再次扫描面前智能柜上的\n" +
                        "“预约服务”二维码\n" +
                        "进行下单操作。\n" +
                        "或<a href=\"https://www.freelycar.com/wechat/scanCode\">点击这里</a>扫码下单");
                String message = MessageUtil.mapToXml(sendMessage, true);
                out.print(message);
                out.close();
            }
            if (!StringUtils.isEmpty(eventKey) && eventKey.contains("sn:")) {
                String devicesn = eventKey.split(":")[1];
                Ark ark = arkRepository.findTopBySnAndDelStatus(devicesn, Constants.DelStatus.NORMAL.isValue());
                Store store = null;
                if (ark != null) {
                    Optional<Store> storeOptional = storeRepository.findById(ark.getStoreId());
                    if (storeOptional.isPresent()) store = storeOptional.get();
                }
                //模板消息
//                WechatTemplateMessage.remindToOrder(ark, store, openId);
                //客服消息
                CustomMessage.remindToOrder(ark, openId);
            }
            out.close();
        } else {
            out.print("");
            out.close();
        }

    }

    /*@GetMapping(value = "/chat", produces = MEDIATYPE_CHARSET_JSON_UTF8)
    public void get(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        try {
            Map<String, String> map = MessageUtil.(request);
            String ToUserName = map.get("ToUserName");
            String FromUserName = map.get("FromUserName");
            request.getSession().setAttribute("openid",FromUserName);
            String CreateTime = map.get("CreateTime");
            String MsgType = map.get("MsgType");
            String message = null;
            if (MsgType.equals(WXConstants.MESSAGE_EVENT)) {
                //从集合中，获取是哪一种事件传入
                String eventType = map.get("Event");
                //对获取到的参数进行处理
                String eventKey = map.get("EventKey");
                String[] params = eventKey.split("_");
                String code = "";
                if (params.length==2){
                    logger.info("二维码参数为-----name:"+params[0]+",code:"+params[1]);
                    if (params[0].equalsIgnoreCase("bookshelf")){
                        code = params[1];
                        request.getSession().setAttribute(WXConstants.SCAN_NAME_SHELF,code);
                    }
                }
                //扫描带参数的二维码，如果用户未关注，则可关注公众号，事件类型为subscribe；用户已关注，则事件类型为SCAN
                if (eventType.equals(WXConstants.MESSAGE_SUBSCRIBE)) {
                    //返回注册图文消息（在上一节关注并返回图文消息中已讲解）
                    message = MessageUtil.initNewsMessage(ToUserName, FromUserName);
                } else if (eventType.equals(WXConstants.MESSAGE_SCAN)) {
                    //TODO 你自己的业务需求
                }
            }
            out.print(message); //返回转换后的XML字符串
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        out.close();
    }*/
}
