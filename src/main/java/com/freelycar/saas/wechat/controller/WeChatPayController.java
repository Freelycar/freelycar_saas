package com.freelycar.saas.wechat.controller;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.aop.LoggerManage;
import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.basic.wrapper.ResultCode;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.project.entity.ConsumerOrder;
import com.freelycar.saas.project.model.PayOrder;
import com.freelycar.saas.project.repository.ConsumerOrderRepository;
import com.freelycar.saas.project.service.ConsumerOrderService;
import com.freelycar.saas.util.RandomStringGenerator;
import com.freelycar.saas.wechat.model.OrderPay;
import com.freelycar.saas.wxutils.HttpRequest;
import com.freelycar.saas.wxutils.WeChatSignatureUtil;
import com.freelycar.saas.wxutils.WechatConfig;
import com.freelycar.saas.wxutils.XMLParser;
import io.swagger.annotations.ApiOperation;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author tangwei - Toby
 * @date 2019-02-02
 * @email toby911115@gmail.com
 */
@RestController
@RequestMapping("/wechat/pay")
public class WeChatPayController {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ConsumerOrderRepository consumerOrderRepository;

    @Autowired
    private ConsumerOrderService consumerOrderService;


    @PostMapping("/payOrderByWechat")
    @LoggerManage(description = "调用智能柜微信支付接口")
    public ResultJsonObject payOrderByWechat(@RequestBody OrderPay orderPay, HttpServletRequest request) {
        String openId = orderPay.getOpenId();
        String orderId = orderPay.getOrderId();
        float totalPrice = orderPay.getTotalPrice();

        //添加参数验证
        if (StringUtils.isEmpty(openId) || StringUtils.isEmpty(orderId)) {
            return ResultJsonObject.getErrorResult(orderPay, "传入的参数有缺失，请核实。");
        }


        //微信支付
        logger.info("执行微信支付：");

        JSONObject jsonObject = new JSONObject();
        Map<String, Object> map = new HashMap<>();
        //微信接口配置
        String ip = request.getHeader("x-forwarded-for") == null ? request.getRemoteAddr() : request.getHeader("x-forwarded-for");
        //判断订单
        ConsumerOrder consumerOrder = consumerOrderRepository.findById(orderId).orElse(null);
        if (null == consumerOrder) {
            return ResultJsonObject.getErrorResult(null, "未找到id为：" + orderId + " 的单据信息");
        }
        map.put("body", "carService");
        map.put("out_trade_no", consumerOrder.getId());


        map.put("appid", WechatConfig.APP_ID);

        map.put("device_info", "WEB");
        map.put("mch_id", WechatConfig.MCH_ID);// 商户号，微信商户平台里获取
        //随机32位
        map.put("nonce_str", RandomStringGenerator.getRandomStringByLength(32));

        //返回结果	自己调用自己的接口
        String notifyUrl = WechatConfig.API_URL + "wechat/pay/wechatPayResult";
        logger.info(notifyUrl);
        map.put("notify_url", notifyUrl);

        map.put("openid", openId);
        map.put("spbill_create_ip", ip);
        map.put("total_fee", (int) (totalPrice * 100));
        map.put("trade_type", "JSAPI");
        // 签名
        String sig = WeChatSignatureUtil.getSig(map);
        map.put("sign", sig);

        HttpEntity entity = HttpRequest.getEntity(XMLParser.getXmlFromMap(map));
        logger.info("entity: " + XMLParser.getXmlFromMap(map));
        String result = HttpRequest.postCall(WechatConfig.ORDER_URL, entity, null);
        //第一步请求完成
        logger.info("请求微信支付结果：" + result);

        Map<String, Object> resultMap = XMLParser.getMapFromXML(result);
        if (!resultMap.isEmpty()) {
            if (resultMap.get("return_code").toString().equals("SUCCESS")) {
                // 预支付id
                String prepareId = resultMap.get("prepay_id").toString();
                Map<String, Object> payMap = new HashMap<String, Object>();
                payMap.put("appId", WechatConfig.APP_ID);
                payMap.put("timeStamp", Long.toString(System.currentTimeMillis()));
                payMap.put("nonceStr", RandomStringGenerator.getRandomStringByLength(32));
                payMap.put("package", "prepay_id=" + prepareId);
                payMap.put("signType", "MD5");
                // 签名
                String pagSign = WeChatSignatureUtil.getSig(payMap);
                payMap.put("paySign", pagSign);
                return ResultJsonObject.getDefaultResult(payMap);
            }
        }
        logger.error(result);
        return ResultJsonObject.getCustomResult(result, ResultCode.CALL_PORT_ERROR.code(), ResultCode.ORDER_ERROR.message());
    }


    @RequestMapping(value = "/wechatPayResult")
    @LoggerManage(description = "执行智能柜微信支付结果回调方法")
    public void wechatResult(HttpServletRequest request, HttpServletResponse response) {
        logger.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!准备回调!!!!!!!!!");
        logger.info("callback payOrderByWechat");
        //Map
        Map<String, Object> map = XMLParser.requestToXml(request);
        Map<String, Object> responseMap = new HashMap<>();
        if (map != null && !map.isEmpty()) {
            if ("SUCCESS".equals(map.get("return_code"))) {
                if (!WeChatSignatureUtil.isCorrect(map)) {
                    responseMap.put("return_code", "FAIL");
                    responseMap.put("return_msg", "签名失败");
                } else {
                    responseMap.put("return_code", "SUCCESS");
                    responseMap.put("return_msg", "OK");
                    String orderId = map.get("out_trade_no").toString();

                    //支付成功，处理支付结果，同步到数据库
                    logger.info("支付成功，处理支付结果，更新订单状态到数据库------------------------");
                    logger.info("orderId：" + orderId);

                    ResultJsonObject resultJsonObject = consumerOrderService.wechatPaySuccess(orderId);

                    logger.info("paySuccess本地处理结果:" + resultJsonObject.toString());
                    if (resultJsonObject.getCode() != ResultCode.SUCCESS.code()) {
                        responseMap.put(Constants.RESPONSE_CODE_KEY, resultJsonObject.getCode());
                        logger.error("paySuccess本地处理结果:" + resultJsonObject.toString());
                    }
                }
                logger.debug(String.valueOf(responseMap));
                try {
                    OutputStream os = response.getOutputStream();
                    os.write(XMLParser.getXmlFromMap(responseMap).getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 结算（同web端结算逻辑）
     *
     * @param payOrder
     * @return
     */
    @ApiOperation(value = "单据结算（支付）", produces = "application/json")
    @PostMapping("/payOrderByCard")
    @LoggerManage(description = "调用方法：单据结算（支付）")
    public ResultJsonObject payOrderByCard(@RequestBody PayOrder payOrder) {
        try {
            return consumerOrderService.payment(payOrder);
        } catch (ObjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }

//    @GetMapping("/paySuccess")
//    public ResultJsonObject paySuccess(@RequestParam String orderId) {
//        return consumerOrderService.wechatPaySuccess(orderId);
//    }
}
