package com.freelycar.saas.wechat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author tangwei - Toby
 * @date 2019-02-18
 * @email toby911115@gmail.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseOrderInfo {
    /**
     * 订单id
     */
    private String id;
    /**
     * 是否为缓冲页面
     */
    private Boolean isBuffer;
    /**
     * 是否为用户缓冲
     */
    private Boolean isUser;
    /**
     * 服务商名称
     */
    private String rspName;
    private String rspPhone;
    /**
     * 车牌号
     */
    private String licensePlate;
    /**
     * 车牌
     */
    private String carBrand;
    private String carType;
    /**
     * 车主姓名
     */
    private String clientName;
    private String projectNames;
    private String cardName;
    private String couponName;
    private Date createTime;
    private Date pickTime;
    private Date finishTime;
    private Integer state;
    private Double actualPrice;
    private Double totalPrice;
    private Integer payState;
    private String staffOrderImgUrl;
    private String clientOrderImgUrl;
    /**
     * 用户存放钥匙位置
     */
    private String userKeyLocation;
    /**
     * 技师存放钥匙位置
     */
    private String staffKeyLocation;
    /**
     * 车辆位置
     */
    private String parkingLocation;
    /**
     * 备注
     */
    private String comment;
}
