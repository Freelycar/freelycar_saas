package com.freelycar.saas.project.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @Author: pyt
 * @Date: 2021/4/20 10:52
 * @Description: 用户查询订单页订单模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryOrder {
    /**
     * 订单id
     */
    private String id;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 订单状态
     */
    private Integer state;
    /**
     * 订单包含项目
     */
    private String projectNames;
    /**
     * 车牌号
     */
    private String licensePlate;
}
