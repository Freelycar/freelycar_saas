package com.freelycar.saas.wxutils;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.project.entity.Ark;
import com.freelycar.saas.project.entity.ConsumerOrder;
import com.freelycar.saas.project.entity.Door;
import com.freelycar.saas.project.entity.Store;
import com.freelycar.saas.util.RoundTool;
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WechatTemplateMessage {

    private static final String PAY_SUCCESS_TEMPLATE_ID = "F5CyYJ5u9_1wRcapK1ECOYRrjxcLcL3rjB0xUg_VQn0";
    private static final String PAY_FAIL_ID = "o0TOjg7KkxoL4CtQ91j--TVVmxYDSNk-dLWqoUVd8mw";
    private static final String ORDER_CHANGED_FOR_CLIENT_ID = "PeRe0M0iEbm7TpN6NOThhBUjwzy_aHsi6r2E7Pa8J1A";
    private static final String ORDER_CHANGED_FOR_STAFF_ID = "il1UVsHA-GesQsFHERczQP9zPvz-od-q240c3fqd9vM";
    private static final String ORDER_CREATE_ID = "C-m3oRNedo3vM6ugQXBa4RhOtW3qwqM7tjWNKuAgYe8";
    private static final String SCAN_CODE_ID = "_1PB1dksRGKm4P1F-h4p6ZzdfFbjLPmX34DPQynUodU";
    //关注提醒下单
    private static final String REMIND_TO_ORDER = "";

    private static final Logger log = LogManager.getLogger(WechatTemplateMessage.class);

    private static final String PAY_ERROR_DATABASE_FAIL = "服务异常";

    private static String invokeTemplateMessage(JSONObject params) {
        //解决中文乱码问题
        StringEntity entity = new StringEntity(params.toString(), "utf-8");
        String result = HttpRequest.postCall(WechatConfig.WECHAT_TEMPLATE_MESSAGE_URL +
                        WechatConfig.getAccessTokenForInteface().getString("access_token"),
                entity, null);
        log.debug("微信模版消息结果：" + result);
        return result;
    }

    /**
     * 用户扫码推送
     * {{first.DATA}}
     * 扫码时间：{{keyword1.DATA}}
     * 扫码地点：{{keyword2.DATA}}
     * {{remark.DATA}}
     */
    public static void remindToOrder(Ark ark, Store store, String openId) {
        String url = "https://www.freelycar.com/wechat/role-select/" + ark.getSn();
        String storeName = store.getName();
        String arkLocation = ark.getLocation();
        JSONObject params = new JSONObject();
        params.put("touser", openId);
        params.put("template_id", SCAN_CODE_ID);
        params.put("url", url);

        JSONObject data = new JSONObject();
        String first = "感谢您的关注，请点击开始下单~";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        data.put("first", keywordFactory(first, "#173177"));
        data.put("keyword1", keywordFactory(simpleDateFormat.format(new Date()), "#173177"));
        data.put("keyword2", keywordFactory(arkLocation, "#173177"));
        data.put("remark", keywordFactory("开始下单!"));
        params.put("data", data);

        String result = invokeTemplateMessage(params);
        log.info("微信订单更新模版消息结果：" + result);
    }

    /**
     * 推送通知：智能柜用户订单状态变化
     * {{first.DATA}}
     * 订单编号： {{OrderSn.DATA}}
     * 订单状态： {{OrderStatus.DATA}}
     * {{remark.DATA}}
     */
    public static void orderChanged(ConsumerOrder consumerOrder, String openId) {
        log.info("准备订单更新模版消息……（推送给用户）");

        String staffName = consumerOrder.getPickCarStaffName();
        Integer state = consumerOrder.getState();
        String first;
        String stateString;
        String parkingLocation = consumerOrder.getParkingLocation();
        String remark = "";
        String remarkSuffix = "小易爱车竭诚为您服务！";
        String licensePlate = consumerOrder.getLicensePlate();

        String userKeyLocationSn = consumerOrder.getUserKeyLocationSn();
        String url = WechatConfig.APP_DOMAIN + "payOrder?orderId=" + consumerOrder.getId();
        switch (state) {
            case 0:
                stateString = "待接车";
                first = "欢迎使用小易爱车智能柜服务，您的智能柜服务订单已生成成功。";
                break;
            case 1:
                stateString = "已接车";
                first = "已接到您的爱车" + licensePlate + "，我们将立即为您服务。";
                if (StringUtils.hasText(staffName)) {
                    remark += "服务人员：" + staffName + "\n";
                }
                break;
            case 2:
                stateString = "已完工";
                first = "您的爱车" + licensePlate + "已服务完成，等待您的取回。";
                if (StringUtils.hasText(staffName)) {
                    remark += "服务人员：" + staffName + "\n";
                }
                if (StringUtils.hasText(parkingLocation)) {
                    remark += "停车位置：" + parkingLocation + "\n";
                }
                if (StringUtils.hasText(userKeyLocationSn)) {
                    url = WechatConfig.APP_DOMAIN + "role-select/" + userKeyLocationSn.split(Constants.HYPHEN)[0];
                }
                break;
            case 3:
                stateString = "已交车";
                first = "您的爱车" + licensePlate + "已交车。期待您的再次光临。";
                break;
            default:
                stateString = "已交车";
                first = "您的爱车" + licensePlate + "已交车。期待您的再次光临";
        }

        JSONObject params = new JSONObject();
        JSONObject data = new JSONObject();
        params.put("touser", openId);
        params.put("template_id", ORDER_CHANGED_FOR_CLIENT_ID);
        params.put("url", url);
        data.put("first", keywordFactory(first, "#173177"));
        data.put("OrderSn", keywordFactory(consumerOrder.getId(), "#173177"));
        data.put("OrderStatus", keywordFactory(stateString, "#173177"));
        data.put("remark", keywordFactory(remark + remarkSuffix));
        params.put("data", data);
        String result = invokeTemplateMessage(params);
        log.info("微信订单更新模版消息结果：" + result);
    }


    /**
     * 推送通知：智能柜用户订单状态变化，推送给技师（客户取消订单和有技师接了某个订单）
     * {{first.DATA}}
     * 订单编号： {{OrderSn.DATA}}
     * 订单状态： {{OrderStatus.DATA}}
     * {{remark.DATA}}
     */
    public static void orderChangedForStaff(ConsumerOrder consumerOrder, String openId, Door door, Ark ark) {
        log.info("准备订单更新模版消息……（推送给技师）");
        Integer state = consumerOrder.getState();
        if (state == 4 || state == 1) {
            String clientName = consumerOrder.getClientName();
            String staffName = consumerOrder.getPickCarStaffName();
            String first;
            String stateString;
//            String parkingLocation = consumerOrder.getParkingLocation();
            String licensePlate = consumerOrder.getLicensePlate();
            String orderId = consumerOrder.getId();
            if (state == 4) {
                stateString = "已取消";
                first = "客户取消订单通知";
            } else {
                stateString = "已接车";
                first = "技师 " + staffName + " 已接到订单。";
            }

            String remark = "客户姓名：" + clientName
                    + "\n车牌：" + licensePlate
                    + "\n智能柜网格：" + door.getArkSn() + "-" + door.getDoorSn()
                    + "\n智能柜名称：" + ark.getLocation()
                    + "\n智能柜地址：" + ark.getLocation();

            JSONObject params = new JSONObject();
            JSONObject data = new JSONObject();
            params.put("touser", openId);
            params.put("template_id", ORDER_CHANGED_FOR_CLIENT_ID);
//        params.put("url", WechatConfig.APP_DOMAIN + "#/shop/servicesorder?id=" + consumerOrder.getId());
            data.put("first", keywordFactory(first, "#173177"));
            data.put("OrderSn", keywordFactory(orderId, "#173177"));
            data.put("OrderStatus", keywordFactory(stateString, "#173177"));
            data.put("remark", keywordFactory(remark));
            params.put("data", data);
            String result = invokeTemplateMessage(params);
            log.info("微信订单更新模版消息结果：" + result);
        }
    }

    /**
     * 消息推送：订单生成时告知技师
     * <p>
     * {{first.DATA}}
     * 需求时间：{{keyword1.DATA}}
     * 需求类型：{{keyword2.DATA}}
     * 车型：{{keyword3.DATA}}
     * 报价：{{keyword4.DATA}}
     * 详情：{{keyword5.DATA}}
     * {{remark.DATA}}
     */
    public static void orderCreated(ConsumerOrder consumerOrder, String projects, String openId, Door door, Ark ark) {
        log.info("准备订单生成成功的模版消息。");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");

        String first = consumerOrder.getClientName() + " 刚刚预约了新的服务订单";
        String createTime = sdf.format(consumerOrder.getCreateTime());

//        String stateString;
//        String parkingLocation = consumerOrder.getParkingLocation();
//        String remark = "";
//        String remarkSuffix = "小易爱车竭诚为您服务！";


        JSONObject params = new JSONObject();
        JSONObject data = new JSONObject();
        params.put("touser", openId);
        params.put("template_id", ORDER_CREATE_ID);
//        params.put("url", "https://" + WechatConfig.APP_DOMAIN + "#/shop/servicesorder?id=" + consumerOrder.getId());

        //第一行信息
        data.put("first", keywordFactory(first, "#173177"));
        //需求时间
        data.put("keyword1", keywordFactory(createTime, "#173177"));
        //需求类型
        data.put("keyword2", keywordFactory(projects, "#173177"));
        //车型
        data.put("keyword3", keywordFactory(consumerOrder.getCarBrand(), "#173177"));
        //报价
        data.put("keyword4", keywordFactory(String.valueOf(RoundTool.round(consumerOrder.getTotalPrice(), 2, BigDecimal.ROUND_HALF_UP)), "#173177"));
        //详情
        data.put("keyword5", keywordFactory("智能柜：" + door.getArkSn() + "-" + door.getDoorSn(), "#173177"));

        data.put("remark", keywordFactory("智能柜位置：" + ark.getLocation() + "\n车辆停放位置：" + consumerOrder.getParkingLocation() + "\n请前往对应的智能柜扫码查看（取车）"));
        params.put("data", data);
        String result = invokeTemplateMessage(params);
        log.info("微信订单更新模版消息结果：" + result);
    }

    private static JSONObject keywordFactory(String value) {
        JSONObject keyword = new JSONObject();
        keyword.put("value", value);
        return keyword;
    }

    private static JSONObject keywordFactory(String value, String color) {
        JSONObject keyword = keywordFactory(value);
        keyword.put("color", color);
        return keyword;
    }
}
