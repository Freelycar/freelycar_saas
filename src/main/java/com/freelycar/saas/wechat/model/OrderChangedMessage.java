package com.freelycar.saas.wechat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: pyt
 * @Date: 2021/4/25 15:39
 * @Description:
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderChangedMessage {
    //小程序用户openId
    private String openId;
    private String userMiniOpenId;
    //是否优先使用小程序openId
    private boolean useMini;
    private String templateId;
    private String first;
    private String orderId;
    private String orderState;
    private String comment;
    private String accessToken;
    //true-client,false-staff
    private Boolean isClient;

    @Override
    public String toString() {
        return "OrderChangedMessage{" +
                "openId='" + openId + '\'' +
                ", userMiniOpenId='" + userMiniOpenId + '\'' +
                ", useMini=" + useMini +
                ", templateId='" + templateId + '\'' +
                ", first='" + first + '\'' +
                ", orderId='" + orderId + '\'' +
                ", orderState='" + orderState + '\'' +
                ", comment='" + comment + '\'' +
                ", accessToken='" + accessToken + '\'' +
                ", isClient=" + isClient +
                '}';
    }
}
