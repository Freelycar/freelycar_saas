package com.freelycar.screen.entity;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 订单
 *
 * @author tangwei - Toby
 * @date 2018/10/22
 * @email toby911115@gmail.com
 */
@Entity
@Table
@DynamicInsert
@DynamicUpdate
@Data
public class ConsumerProjectInfo implements Serializable {
    private static final long serialVersionUID = 16L;

    @Id
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @GeneratedValue(generator = "uuid")
    @NotNull
    @Length(max = 50)
    private String id;

    @Column(nullable = false, columnDefinition = "bit default 0")
    private Boolean delStatus;

    @Column(nullable = false, columnDefinition = "datetime default NOW()")
    private Timestamp createTime;

    /**
     * 服务项目所在订单ID
     */
    @Column
    private String consumerOrderId;

    /**
     * 服务项目ID
     */
    @Column
    private String projectId;

    /**
     * 服务项目名称
     */
    @Column
    private String projectName;

    /**
     * 支付方式
     */
    @Column
    private String payMethod;

    /**
     * 会员卡ID
     */
    @Column
    private String cardId;

    /**
     * 会员卡名称
     */
    @Column
    private String cardName;

    /**
     * 会员卡号
     */
    @Column
    private String cardNumber;

    /**
     * 会员卡卡此次消费扣除次数
     */
    @Column
    private Integer payCardTimes;

    /**
     * 优惠券ID
     */
    @Column
    private String couponId;

    /**
     * 优惠券名称
     */
    @Column
    private String couponName;

    /**
     * 金额
     */
    @Column(columnDefinition = "double default 0")
    private Double price;

    /**
     * 会员价格
     */
    @Column(nullable = false, columnDefinition = "float default 0.0")
    private Double memberPrice;

    /**
     * 工时单价
     */
    @Column
    private Double pricePerUnit;

    /**
     * 参考工时
     */
    @Column
    private Float referWorkTime;

    /**
     * 服务人员ID
     */
    @Column
    private String staffId;

    /**
     * 服务人员姓名
     */
    @Column
    private String staffName;

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":\"")
                .append(id).append('\"');
        sb.append(",\"delStatus\":")
                .append(delStatus);
        sb.append(",\"createTime\":\"")
                .append(createTime).append('\"');
        sb.append(",\"consumerOrderId\":\"")
                .append(consumerOrderId).append('\"');
        sb.append(",\"projectId\":\"")
                .append(projectId).append('\"');
        sb.append(",\"projectName\":\"")
                .append(projectName).append('\"');
        sb.append(",\"payMethod\":\"")
                .append(payMethod).append('\"');
        sb.append(",\"cardId\":\"")
                .append(cardId).append('\"');
        sb.append(",\"cardName\":\"")
                .append(cardName).append('\"');
        sb.append(",\"cardNumber\":\"")
                .append(cardNumber).append('\"');
        sb.append(",\"payCardTimes\":")
                .append(payCardTimes);
        sb.append(",\"couponId\":\"")
                .append(couponId).append('\"');
        sb.append(",\"couponName\":\"")
                .append(couponName).append('\"');
        sb.append(",\"price\":")
                .append(price);
        sb.append(",\"memberPrice\":")
                .append(memberPrice);
        sb.append(",\"pricePerUnit\":")
                .append(pricePerUnit);
        sb.append(",\"referWorkTime\":")
                .append(referWorkTime);
        sb.append(",\"staffId\":\"")
                .append(staffId).append('\"');
        sb.append(",\"staffName\":\"")
                .append(staffName).append('\"');
        sb.append('}');
        return sb.toString();
    }
}

