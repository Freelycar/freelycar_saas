package com.freelycar.saas.project.entity;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 优惠券实物表（客户购买的信息）
 *
 * @author tangwei - Toby
 * @date 2018/10/23
 * @email toby911115@gmail.com
 */
@Entity
@Table
@DynamicInsert
@DynamicUpdate
public class Coupon implements Serializable {
    private static final long serialVersionUID = 15L;

    @Id
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @GeneratedValue(generator = "uuid")
    @NotNull
    private String id;

    @Column(nullable = false, columnDefinition = "bit default 0")
    private Boolean delStatus = false;

    @Column(nullable = false, columnDefinition = "datetime default NOW()")
    private Timestamp createTime = new Timestamp(System.currentTimeMillis());

    @Column
    private String storeId;

    @Column
    private String couponServiceId;

    @Column
    private String clientId;

    @Column
    private String name;

    @Column
    private Timestamp deadline;

    public Coupon() {
    }

    @Override
    public String toString() {
        return "Coupon{" +
                "id='" + id + '\'' +
                ", delStatus=" + delStatus +
                ", createTime=" + createTime +
                ", storeId='" + storeId + '\'' +
                ", couponServiceId='" + couponServiceId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", name='" + name + '\'' +
                ", deadline=" + deadline +
                '}';
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getDelStatus() {
        return delStatus;
    }

    public void setDelStatus(Boolean delStatus) {
        this.delStatus = delStatus;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Timestamp createTime) {
        this.createTime = createTime;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getCouponServiceId() {
        return couponServiceId;
    }

    public void setCouponServiceId(String couponServiceId) {
        this.couponServiceId = couponServiceId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
        this.deadline = deadline;
    }
}
