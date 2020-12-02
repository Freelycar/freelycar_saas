package com.freelycar.saas.wechat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.Date;

/**
 * 技师服务中的订单：
 * 包括：接单订单和接车订单
 *
 * @author tangwei - Toby
 * @date 2019-02-18
 * @email toby911115@gmail.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinishOrderInfo {
    private String id;

    private Boolean isBuffer;

    private String rspName;

    private Integer state;

    private String clientName;

    private String phone;

    private String licensePlate;

    private String carBrand;

    private String carType;

    private String carColor;

    private String carImageUrl;

    private String projectNames;

    private Date pickTime;

    private Date orderTakingTime;

    private String userKeyLocationSn;

    private String userKeyLocation;

    private String parkingLocation;

    private String clientOrderImgUrl;
}
