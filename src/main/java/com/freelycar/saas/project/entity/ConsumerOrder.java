package com.freelycar.saas.project.entity;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.validator.constraints.Length;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * @author tangwei - Toby
 * @date 2018/10/22
 * @email toby911115@gmail.com
 */
@Entity
@Table
@DynamicInsert
@DynamicUpdate
@Data
public class ConsumerOrder implements Serializable {
    private static final long serialVersionUID = 11L;

    @Id
    @Length(max = 50)
    private String id;

    @Column(nullable = false, columnDefinition = "bit default 0")
    private Boolean delStatus;

    @Column(nullable = false, columnDefinition = "datetime default NOW()")
    private Timestamp createTime;

    @Column
    private String storeId;

    /**
     * 车辆信息对应主键ID
     */
    @Column
    private String carId;

    /**
     * 车辆品牌
     */
    @Column
    private String carBrand;

    /**
     * 车辆型号
     */
    @Column
    private String carType;

    /**
     * 车牌号码
     */
    @Column
    private String licensePlate;

    /**
     * 车辆颜色
     */
    @Column
    private String carColor;

    /**
     * 车辆图片
     */
    @Column
    private String carImageUrl;


    /**
     * 用户信息对应主键ID
     */
    @Column
    private String clientId;

    /**
     * 用户姓名
     */
    @Column
    private String clientName;

    /**
     * 性别
     */
    @Column
    private String gender;

    /**
     * 手机号
     */
    @Column
    private String phone;

    /**
     * 是否会员
     */
    @Column(nullable = false, columnDefinition = "bit default 0")
    private Boolean isMember;

    /**
     * 交车时间
     */
    @Column
    private Timestamp deliverTime;

    /**
     * 完工时间
     */
    @Column
    private Timestamp finishTime;

    /**
     * 接车时间
     */
    @Column
    private Timestamp pickTime;

    /**
     * 接单时间
     */
    @Column
    private Timestamp orderTakingTime;


    /**
     * 上次里程
     */
    @Column
    private Integer lastMiles;

    /**
     * 本次里程
     */
    @Column
    private Integer miles;

    /**
     * 停车位置
     */
    @Column
    private String parkingLocation;

    /**
     * 订单状态
     */
    @Column(columnDefinition = "int default 0")
    private Integer state;

    /**
     * 订单总价
     */
    @Column(nullable = false, columnDefinition = "double default 0")
    private Double totalPrice;

    /**
     * 会员订单总价
     */
    @Column(nullable = false, columnDefinition = "double default 0")
    private Double memberPrice;

    /**
     * 实付价格=第一实付价格+第二实付价格
     */
    @Column(columnDefinition = "double default 0")
    private Double actualPrice;

    /**
     * 第一支付方式
     */
    @Column
    private Integer firstPayMethod;

    /**
     * 第一实付价格
     */
    @Column(columnDefinition = "double default 0")
    private Double firstActualPrice;

    /**
     * 第一支付使用的卡ID
     */
    @Column
    private String firstCardId;

    /**
     * 第二支付方式
     */
    @Column
    private Integer secondPayMethod;

    /**
     * 第二实付价格
     */
    @Column(columnDefinition = "double default 0")
    private Double secondActualPrice;

    /**
     * 第二支付使用的卡ID
     */
    @Column
    private String secondCardId;

    /**
     * 支付状态（0：未结算；1：已结算）
     */
    @Column(columnDefinition = "int default 0")
    private Integer payState;

    /**
     * 取车人员ID
     */
    @Column
    private String pickCarStaffId;

    /**
     * 取车人员姓名
     */
    @Column
    private String pickCarStaffName;

    /**
     * 订单类型（1.服务开单；2.智能柜开单；3.办卡/续卡/抵用券）
     */
    @Column
    private Integer orderType;

    /**
     * 办理的会员卡、抵用券业务的ID（卡ID或者券ID）
     */
    @Column
    private String cardOrCouponId;

    /**
     * 故障描述
     */
    @Column
    private String faultDescription;

    /**
     * 取消时间（智能柜预约服务取消）
     */
    @Column
    private Timestamp cancelTime;

    @Column
    private String userKeyLocation;

    @Column
    private String userKeyLocationSn;

    @Column
    private String staffKeyLocation;

    @Column
    private String staffKeyLocationSn;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":\"")
                .append(id).append('\"');
        sb.append(",\"delStatus\":")
                .append(delStatus);
        sb.append(",\"createTime\":\"")
                .append(createTime).append('\"');
        sb.append(",\"storeId\":\"")
                .append(storeId).append('\"');
        sb.append(",\"carId\":\"")
                .append(carId).append('\"');
        sb.append(",\"carBrand\":\"")
                .append(carBrand).append('\"');
        sb.append(",\"carType\":\"")
                .append(carType).append('\"');
        sb.append(",\"licensePlate\":\"")
                .append(licensePlate).append('\"');
        sb.append(",\"carColor\":\"")
                .append(carColor).append('\"');
        sb.append(",\"carImageUrl\":\"")
                .append(carImageUrl).append('\"');
        sb.append(",\"clientId\":\"")
                .append(clientId).append('\"');
        sb.append(",\"clientName\":\"")
                .append(clientName).append('\"');
        sb.append(",\"gender\":\"")
                .append(gender).append('\"');
        sb.append(",\"phone\":\"")
                .append(phone).append('\"');
        sb.append(",\"isMember\":")
                .append(isMember);
        sb.append(",\"deliverTime\":\"")
                .append(deliverTime).append('\"');
        sb.append(",\"finishTime\":\"")
                .append(finishTime).append('\"');
        sb.append(",\"pickTime\":\"")
                .append(pickTime).append('\"');
        sb.append(",\"lastMiles\":")
                .append(lastMiles);
        sb.append(",\"miles\":")
                .append(miles);
        sb.append(",\"parkingLocation\":\"")
                .append(parkingLocation).append('\"');
        sb.append(",\"state\":")
                .append(state);
        sb.append(",\"totalPrice\":")
                .append(totalPrice);
        sb.append(",\"memberPrice\":")
                .append(memberPrice);
        sb.append(",\"actualPrice\":")
                .append(actualPrice);
        sb.append(",\"firstPayMethod\":")
                .append(firstPayMethod);
        sb.append(",\"firstActualPrice\":")
                .append(firstActualPrice);
        sb.append(",\"firstCardId\":\"")
                .append(firstCardId).append('\"');
        sb.append(",\"secondPayMethod\":")
                .append(secondPayMethod);
        sb.append(",\"secondActualPrice\":")
                .append(secondActualPrice);
        sb.append(",\"secondCardId\":\"")
                .append(secondCardId).append('\"');
        sb.append(",\"payState\":")
                .append(payState);
        sb.append(",\"pickCarStaffId\":\"")
                .append(pickCarStaffId).append('\"');
        sb.append(",\"pickCarStaffName\":\"")
                .append(pickCarStaffName).append('\"');
        sb.append(",\"orderType\":")
                .append(orderType);
        sb.append(",\"cardOrCouponId\":\"")
                .append(cardOrCouponId).append('\"');
        sb.append(",\"faultDescription\":\"")
                .append(faultDescription).append('\"');
        sb.append(",\"cancelTime\":\"")
                .append(cancelTime).append('\"');
        sb.append(",\"userKeyLocation\":\"")
                .append(userKeyLocation).append('\"');
        sb.append(",\"userKeyLocationSn\":\"")
                .append(userKeyLocationSn).append('\"');
        sb.append(",\"staffKeyLocation\":\"")
                .append(staffKeyLocation).append('\"');
        sb.append(",\"staffKeyLocationSn\":\"")
                .append(staffKeyLocationSn).append('\"');
        sb.append('}');
        return sb.toString();
    }
}
