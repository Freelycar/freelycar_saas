package com.freelycar.saas.wxutils;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.project.entity.Ark;
import com.freelycar.saas.project.entity.ConsumerOrder;
import com.freelycar.saas.project.entity.Employee;
import com.freelycar.saas.project.entity.WxUserInfo;
import com.freelycar.saas.wechat.model.OrderChangedMessage;
import org.apache.http.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.StringUtils;

/**
 * @Author: pyt
 * @Date: 2021/4/25 15:12
 * @Description: 微信小程序消息
 */
public class MiniMessage {
    //小程序订阅消息推送
    private static final String SUBSCRIBE_MESSAGE_SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=";
    //小程序订阅消息模板ID
    //用户下单成功:订单编号+备注
    public static final String CLIENT_RESERVATION_ID = "YB1_dHBNuEvd7qsiFo5m7tzBYbPHZAWA0cAq4OB_cLc";
    //用户订单取消
    public static final String CLIENT_ORDER_CANCEL_ID = "t9tfSKzE8V086vrQifvJDeTkFYagNZpW_xw11HWmrSs";
    //用户接单通知:订单编号+备注
    public static final String CLIENT_ORDER_TAKING_ID = "auPG0rNBQi2Zqj2UdMCrqr3Sv1jemf2n_PEFlDROh0g";
    //用户车辆完工通知:订单编号+备注
    public static final String CLIENT_CAR_FINISH_ID = "LYL-yC85VthW5jEAYXsViOA-yRsHPF5mDR5ZHjU23kg";
    //用户交车通知:订单编号+订单状态+备注
    public static final String CLIENT_ORDER_FINISH_ID = "L3944tN8k-tZCsJP5hXVyIFCuImComLGNurdQwOwlDo";
    //技师接单通知
    public static final String STAFF_ORDER_TAKING_ID = CLIENT_ORDER_TAKING_ID;
    //技师完工通知
    public static final String STAFF_CAR_FINISH_ID = CLIENT_CAR_FINISH_ID;
    //技师完工失败通知
    public static final String STAFF_CAR_FINISH_ERROR_ID = CLIENT_ORDER_FINISH_ID;

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
        log.info("开始推送订阅消息……");
        String miniOpenId = orderChangedMessage.getUserMiniOpenId();
        if (!StringUtils.hasText(miniOpenId)) {
            log.error("miniOpenId参数为空");
            return false;
        }
        JSONObject params = new JSONObject();
        params.put("touser", orderChangedMessage.getUserMiniOpenId());
        params.put("template_id", orderChangedMessage.getTemplateId());
        String page = "";
        //判断信息发送角色：用户指向订单详情页，技师指向订单列表
        if (orderChangedMessage.getIsClient()) {
            page = "pages/packuser/orderDetail/main?orderid=" + orderChangedMessage.getOrderId();
        } else {
            page = "pages/technician/order/main";
        }
        params.put("page", page);
        //todo 线上版本需要切换
        //跳转小程序类型：developer为开发版；trial为体验版；formal为正式版；默认为正式版
        params.put("miniprogram_state", "trial");
        params.put("lang", "zh_CN");

        JSONObject data = genData(orderChangedMessage);
        params.put("data", data);
        String result = invokeMessage(SUBSCRIBE_MESSAGE_SEND_URL, orderChangedMessage.getAccessToken(), params);
        log.info("微信模版消息结果：" + result);
        JSONObject res = JSONObject.parseObject(result);
        return res.getInteger("errcode") == 0;
    }

    private static JSONObject genData(OrderChangedMessage orderChangedMessage) {
        String templateId = orderChangedMessage.getTemplateId();
        if (null == templateId) return null;
        JSONObject data = new JSONObject();
        switch (templateId) {
            case CLIENT_RESERVATION_ID:
                data.put("character_string1", JSONObject.parseObject("{\"value\":\"" + orderChangedMessage.getOrderId() + "\"}"));
                data.put("thing12", JSONObject.parseObject("{\"value\":\"" + orderChangedMessage.getComment() + "\"}"));
                break;
            case CLIENT_ORDER_TAKING_ID:
                data.put("character_string1", JSONObject.parseObject("{\"value\":\"" + orderChangedMessage.getOrderId() + "\"}"));
                data.put("thing9", JSONObject.parseObject("{\"value\":\"" + orderChangedMessage.getComment() + "\"}"));
                break;
            case CLIENT_CAR_FINISH_ID:
                data.put("character_string2", JSONObject.parseObject("{\"value\":\"" + orderChangedMessage.getOrderId() + "\"}"));
                data.put("thing7", JSONObject.parseObject("{\"value\":\"" + orderChangedMessage.getComment() + "\"}"));
                break;
            case CLIENT_ORDER_FINISH_ID:
                data.put("character_string9", JSONObject.parseObject("{\"value\":\"" + orderChangedMessage.getOrderId() + "\"}"));
                data.put("thing2", JSONObject.parseObject("{\"value\":\"" + orderChangedMessage.getOrderState() + "\"}"));
                data.put("thing6", JSONObject.parseObject("{\"value\":\"" + orderChangedMessage.getComment() + "\"}"));
                break;
            case CLIENT_ORDER_CANCEL_ID:
                data.put("character_string1", JSONObject.parseObject("{\"value\":\"" + orderChangedMessage.getOrderId() + "\"}"));
                data.put("thing5", JSONObject.parseObject("{\"value\":\"" + orderChangedMessage.getComment() + "\"}"));
                break;
            default:
                break;
        }
        return data;
    }

    public static boolean sendUniformMessage(OrderChangedMessage orderChangedMessage) {
        log.info("开始推送统一服务消息……");
        String openId = orderChangedMessage.getOpenId();
        String miniOpenId = orderChangedMessage.getUserMiniOpenId();
        JSONObject params = new JSONObject();
        if (orderChangedMessage.isUseMini()) {
            params.put("touser", miniOpenId);
        } else {
            params.put("touser", openId);
        }
        JSONObject mp_template_msg = new JSONObject();
        mp_template_msg.put("appid", WechatConfig.APP_ID);
        mp_template_msg.put("template_id", ORDER_CHANGED_ID);

        JSONObject miniprogram = new JSONObject();
        miniprogram.put("appid", WechatConfig.MINI_ID);

        //判断信息发送角色：用户指向订单详情页，技师指向订单列表
        String path = "";
        if (orderChangedMessage.getIsClient()) {
            path = "pages/packuser/orderDetail/main?orderid=" + orderChangedMessage.getOrderId();
        } else {
            path = "pages/technician/order/main";
        }
        miniprogram.put("path", path);
        mp_template_msg.put("miniprogram", miniprogram);
        mp_template_msg.put("url", "");

        JSONObject data = new JSONObject();
        data.put("first", JSONObject.parseObject("{\"value\":\"" + orderChangedMessage.getFirst() + "\"}"));
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

    public static OrderChangedMessage genMessageForClient(
            ConsumerOrder consumerOrder, WxUserInfo userInfo) {
        log.info("准备订单更新模版消息……（推送给用户）");
        OrderChangedMessage message = new OrderChangedMessage();
        message.setOpenId(userInfo.getOpenId());
        message.setUserMiniOpenId(userInfo.getMiniOpenId());
        message.setUseMini(userInfo.getUseMini());
        message.setOrderId(consumerOrder.getId());

        String first;
        String stateString;
        String remark = "";
        //根据订单状态判断提示语
        String staffName = consumerOrder.getPickCarStaffName();
        String parkingLocation = consumerOrder.getParkingLocation();
        String remarkSuffix = "小易爱车竭诚为您服务！";
        String licensePlate = consumerOrder.getLicensePlate();
        Integer state = consumerOrder.getState();
        switch (state) {
            case -1:
                stateString = "待接车";
                first = "您的爱车" + licensePlate + "已被接单，我们将尽快为您服务。";
                if (StringUtils.hasText(staffName)) {
                    remark += "服务人员：" + staffName + "\n";
                }
                break;
            case 0:
                stateString = "待接单";
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
                break;
            case 3:
                stateString = "已交车";
                first = "您的爱车" + licensePlate + "已交车。期待您的再次光临。";
                break;
            default:
                stateString = "已交车";
                first = "您的爱车" + licensePlate + "已交车。期待您的再次光临";
        }
        message.setFirst(first);
        message.setOrderState(stateString);
        message.setComment(remark + remarkSuffix);
        message.setIsClient(true);
        return message;
    }

    public static OrderChangedMessage genMessageForStaff(
            ConsumerOrder consumerOrder, Ark ark,
            Employee employee, Boolean isServiceFinishfailure) {
        log.info("准备订单更新模版消息……（推送给技师）");

        OrderChangedMessage message = new OrderChangedMessage();
        message.setOpenId(employee.getOpenId());
        message.setUserMiniOpenId(employee.getMiniOpenId());
        message.setUseMini(employee.getUseMini());
        message.setOrderId(consumerOrder.getId());
        String first;
        String stateString;
        String clientName = consumerOrder.getClientName();
        String staffName = consumerOrder.getPickCarStaffName();
        String remark = "客户姓名：" + clientName
                + "\n车牌：" + consumerOrder.getLicensePlate()
                + "\n智能柜网格：" + consumerOrder.getUserKeyLocation()
                + "\n智能柜位置：" + ark.getLocation()
                + "\n车辆停放位置：" + consumerOrder.getParkingLocation();
        if (isServiceFinishfailure) {
            first = "完工操作失败";
            stateString = "技师 " + staffName + " 还车关门超时,请重新开门还车。";
        } else {
            Integer state = consumerOrder.getState();

            switch (state) {
                case -1:
                    stateString = "已接单";
                    first = "技师 " + staffName + " 已接到订单。";
                    break;
                case 0:
                    stateString = "待接单";
                    first = clientName + "刚刚预约了新的服务订单";
                    break;
                case 2:
                    stateString = "已完工";
                    first = "技师 " + staffName + " 已完工。";
                    break;
                case 4:
                    stateString = "已取消";
                    first = "客户:" + clientName + "取消订单通知";
                    break;

                default:
                    stateString = "";
                    first = "";

            }
        }
        message.setTemplateId(MiniMessage.STAFF_CAR_FINISH_ERROR_ID);
        message.setFirst(first);
        message.setOrderState(stateString);
        message.setComment(remark);
        message.setIsClient(false);
        return message;
    }

    /*public static void main(String[] args) {
        OrderChangedMessage message = new OrderChangedMessage();
        message.setUserMiniOpenId("oL_VK4-88D1rU0UDL3dBFQvdB6mI");
        message.setOrderId("A0011905080002");
        message.setOrderState("预约成功");
        message.setComment("您已成功预约车辆保养，请在预约时间内进店");
        message.setAccessToken("44_l8MubSLwNV2cDSqDj0c4aQoq_yomwZQ4xokqgVFhXhn2WsNJewJWc9lXUPDxoEqS7pw-WbSSyZwaWIkWWFrOzDb8u0Qyf8bnTrgXtyh6v6gxcBUluGmxdPxonlukw67NkU6RpFLonRWst47aAWMdAEAEAQ");
        System.out.println(sendSubscribeMessage(message));
    }*/

}
