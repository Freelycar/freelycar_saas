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
    private String userOpenId;
    private String orderId;
    private String projects;
    private String orderState;
    private String comment;
    private String accessToken;
}
