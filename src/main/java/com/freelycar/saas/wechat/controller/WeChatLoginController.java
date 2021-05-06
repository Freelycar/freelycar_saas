package com.freelycar.saas.wechat.controller;

import cn.leancloud.core.AVOSCloud;
import cn.leancloud.sms.AVSMS;
import cn.leancloud.sms.AVSMSOption;
import cn.leancloud.types.AVNull;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.project.entity.WxUserInfo;
import com.freelycar.saas.project.service.WxUserInfoService;
import com.freelycar.saas.wxutils.HttpRequest;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.apache.http.HttpEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/wechat/login")
public class WeChatLoginController {
    private final String appid = "YPVPvcghD0yT1CtQKUOpOUGI-gzGzoHsz";
    private final String appkey = "AnrwmLo01qL7RuKNbV0NwWR4";
    private final String ContentType = "application/json";
    private final String leancloudUrlRes = "https://leancloud.cn/1.1/requestSmsCode";
    private final String leancloudUrlVer = "https://leancloud.cn/1.1/verifySmsCode";

    static {
        AVOSCloud.initialize("YPVPvcghD0yT1CtQKUOpOUGI-gzGzoHsz", "AnrwmLo01qL7RuKNbV0NwWR4", "https://ypvpvcgh.lc-cn-n1-shared.com");

    }

    @Autowired
    private WxUserInfoService wxUserInfoService;
    private Logger log = LogManager.getLogger(WeChatLoginController.class);

    //活动：用户注册
    @PostMapping("/register")
    public ResultJsonObject register(
            @RequestParam String phone) {
        WxUserInfo wxUserInfo = wxUserInfoService.register(phone);
        if (null == wxUserInfo) {
            return ResultJsonObject.getErrorResult(null, "已领取");
        }
        return ResultJsonObject.getDefaultResult(null, "领取成功");
    }

    //发送短信验证码请求
    @RequestMapping(value = "/getSmsCode", method = RequestMethod.POST)
    public ResultJsonObject getSmsCode(String phone) {
        return getVerification(phone);
    }

    //发送语音验证码请求
    @RequestMapping(value = "/getVoiceCode", method = RequestMethod.POST)
    public ResultJsonObject getVoiceCode(String phone) {
        return getVerification(phone, true);
    }


    public ResultJsonObject getVerification(String phone) {
        AVSMSOption option = new AVSMSOption();
        option.setTtl(1);
        option.setApplicationName("小易爱车");
        option.setOperation("短信认证");
        final boolean[] flag = {true};
        final String[] message = {""};
        AVSMS.requestSMSCodeInBackground(phone, option)
                .subscribe(new Observer<AVNull>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(AVNull avNull) {
                        flag[0] = true;
                    }

                    @Override
                    public void onError(Throwable e) {
                        flag[0] = false;
                        message[0] = e.getMessage();
                        System.out.println("Result: Failed to send verification code. Reason: " + e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
        if (flag[0]) {
            return ResultJsonObject.getDefaultResult(null);
        } else {
            return ResultJsonObject.getErrorResult(null, "短信发送过快，请稍后重试。");
        }
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


    /**
     * 注册验证结果请求
     *
     * @param phone
     * @param smscode
     * @param openId
     * @param headimgurl
     * @param nickName
     * @return
     */
    @RequestMapping(value = "/verifySmsCode", method = RequestMethod.POST)
    public ResultJsonObject verifySmsCode(
            @RequestParam String phone,
            @RequestParam String smscode,
            @RequestParam(required = false) String openId,
            @RequestParam(required = false) String miniOpenId,
            @RequestParam(required = false) String unionid,
            @RequestParam(required = false) String headimgurl,
            @RequestParam(required = false) String nickName) {
        log.info("验证短信码方法接收到的微信用户信息：");
        if (null != openId) {
            log.info("openId:" + openId);
        }
        if (null != miniOpenId) {
            log.info("miniOpenId:" + miniOpenId);
        }
        if (null != unionid) {
            log.info("unionid:" + unionid);
        }
        if (null != headimgurl) {
            log.info("headimgurl:" + headimgurl);
        }
        if (null != nickName) {
            log.info("nickName:" + nickName);
        }
        //openId、miniOpenId必传一个
        if (!StringUtils.hasText(openId) && !StringUtils.hasText(miniOpenId)) {
            String paramMsg = "参数openId和miniOpenId不能同时为空，请重新微信授权后再试。";
            log.error(paramMsg);
            return ResultJsonObject.getErrorResult(null, paramMsg);
        }
        //添加后台验证，阻断前端传送字符串“undefined”到后台
        if (StringUtils.hasText(openId) && "undefined".equalsIgnoreCase(openId) &&
                StringUtils.hasText(miniOpenId) && "undefined".equalsIgnoreCase(miniOpenId)) {
            String paramMsg = "接收到参数openId、miniOpenId为undefined，请重新微信授权后再试。";
            log.error(paramMsg);
            return ResultJsonObject.getErrorResult(null, paramMsg);
        }

        //验证码验证
        JSONObject json = this.verifySmsCode(phone, smscode);
        if (StringUtils.hasText(json.getString("error"))) {
            log.error(phone + ";code:" + smscode + " 验证失败。。。");
            return ResultJsonObject.getErrorResult(json, json.getString("error"));
        } else {
            return wxUserInfoService.wechatLogin(phone, openId, miniOpenId, unionid, headimgurl, nickName);
        }
    }


    @PostMapping("/changePhone")
    public ResultJsonObject changePhone(@RequestParam String phone, @RequestParam String smsCode, @RequestParam String id) {
        JSONObject json = this.verifySmsCode(phone, smsCode);
        if (StringUtils.hasText(json.getString("error"))) {
            log.debug(phone + ";code:" + smsCode + " 验证失败。。。");
            return ResultJsonObject.getErrorResult(json, json.getString("error"));
        } else {
            return wxUserInfoService.changePhone(phone, id);
        }
    }

    /**
     * 验证码验证方法
     *
     * @param phone
     * @param smscode
     * @return
     */
    private JSONObject verifySmsCode(String phone, String smscode) {
        /*Map<String, Object> head = setLeancloudHead();
        String result = HttpRequest.postCall(leancloudUrlVer + "/" + smscode + "?mobilePhoneNumber=" + phone, null, head);
        log.debug("绑定手机短信验证, phone:" + phone + ", smscode:" + smscode + "。 短信验证结果：" + result);
        JSONObject json = null;
        try {
            json = JSONObject.parseObject(result);
        } catch (JSONException e) {
            e.printStackTrace();
            log.error("解析验证验证码返回结果错误");
        }
        log.debug("解析后结果：" + json);

        return json;*/
        JSONObject jsonObject = new JSONObject();
        AVSMS.verifySMSCodeInBackground(smscode, phone).subscribe(new Observer<AVNull>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onNext(AVNull avNull) {
                jsonObject.put("success", "1");
//                System.out.println("Result: Successfully verified the number.");
            }

            @Override
            public void onError(Throwable throwable) {
                jsonObject.put("error", throwable.getMessage());
//                System.out.println("Result: Failed to verify the number. Reason: " + throwable.getMessage());
            }

            @Override
            public void onComplete() {
            }
        });
        return jsonObject;
    }

    /**
     * 设置Lean Cloud短信服务的请求头
     *
     * @return
     */
    private Map<String, Object> setLeancloudHead() {
        Map<String, Object> head = new HashMap<String, Object>();
        head.put("X-LC-Id", appid);
        head.put("X-LC-Key", appkey);
        head.put("Content-Type", ContentType);
        return head;
    }
}
