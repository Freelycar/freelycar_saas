package com.freelycar.saas.project.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.validator.constraints.Length;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author puyuting
 * @date 2019/12/24
 * @email 2630451673@qq.com
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class EOrder implements Serializable {
    private static final long serialVersionUID = 8743212398027491004L;
    @Id
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @GeneratedValue(generator = "uuid")
    @NotNull
    @Length(max = 50)
    private String id;

    /**
     * 订单id
     */
    private String consumerOrderId;

    /**
     * 验证码
     */
    private Integer verifyCode;
    /**
     * 渠道号
     */
    @NonNull
    private Integer channel;
    /**
     * 商户id
     */
    @NonNull
    private String customerId;
    /**
     * 下单人手机号
     */
    private String createMobile;

    /**
     * e代驾返回订单号
     */
    private Integer orderId;
    /**
     * 订单类型
     */
    @NonNull
    private Integer type;
    /**
     * 订单成单模式
     */
    @NonNull
    private Integer mode;
    /**
     * 订单状态
     */
    private Integer status;
    /**
     * 车主姓名
     */
    private String username;
    /**
     * 车牌号
     */
    private String carNo;
    /**
     * 车主手机号
     */
    private String mobile;

    /**
     * 取车地址联系人姓名
     */
    private String pickupContactName;
    /**
     * 取车地址联系人手机号
     */
    private String pickupContactPhone;
    /**
     * 取车地址
     */
    private String pickupAddress;
    /**
     * 取车地址经度
     */
    private Double pickupAddressLng;
    /**
     * 取车地址纬度
     */
    private Double pickupAddressLat;

    /**
     * 还车地址联系人姓名
     */
    private String returnContactName;
    /**
     * 还车地址联系人手机号
     */
    private String returnContactPhone;
    /**
     * 还车地址
     */
    private String returnAddress;
    /**
     * 还车地址经度
     */
    private Double returnAddressLng;
    /**
     * 还车地址纬度
     */
    private Double returnAddressLat;

    /**
     * 预约时间
     * yyyyMMddHHmmss
     */
    private String bookingTime;
    /**
     * 车辆品牌名称
     */
    private String carBrandName;
    /**
     * 车辆车系名称
     */
    private String carSeriesName;
    /**
     * 订单支付方式，默认为0
     * 0-vip扣款
     * 1-用户付款
     */
    private Integer payMode;
    /**
     * 0-发送短信
     * 1-不发短信
     * 默认是0
     */
    private Integer pushSms;

    /**
     * 取车/还车，delivery or return
     * 0/1
     */
    private Integer dor;
}
