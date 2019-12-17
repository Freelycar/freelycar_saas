package com.freelycar.saas.project.model;

import lombok.Data;

/**
 * 系统三项统计
 * 1.微信累计用户数（如果查询当天为当前日期，则显示上一日累计用户数）
 * 2.注册用户数
 * 3.有效下单数
 */
@Data
public class CumulateThree {
    private String refDate;

    private Long cumulateUserCount;

    /**
     * 关注数是昨天数据（标识）
     */
    private boolean yesterdayFlag = false;

    private Long registerUserCount;

    private Long orderCount;
}
