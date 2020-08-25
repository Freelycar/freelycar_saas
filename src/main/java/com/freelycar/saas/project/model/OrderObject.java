package com.freelycar.saas.project.model;

import com.freelycar.saas.project.entity.*;

import java.util.List;

/**
 * 快速开单的JavaBean
 *
 * @author tangwei - Toby
 * @date 2018-12-28
 * @email toby911115@gmail.com
 */
public class OrderObject {
    private ConsumerOrder consumerOrder;

    private List<ConsumerProjectInfo> consumerProjectInfos;

    private List<AutoParts> autoParts;

    private String doorId;

    private Card card;

    private Coupon coupon;

    private ClientOrderImg clientOrderImg;

    private StaffOrderImg staffOrderImg;

    private StaffInfo staffInfo;

    private Store store;

    private Ark ark;

    public OrderObject() {
    }

    public ConsumerOrder getConsumerOrder() {
        return consumerOrder;
    }

    public void setConsumerOrder(ConsumerOrder consumerOrder) {
        this.consumerOrder = consumerOrder;
    }

    public List<ConsumerProjectInfo> getConsumerProjectInfos() {
        return consumerProjectInfos;
    }

    public void setConsumerProjectInfos(List<ConsumerProjectInfo> consumerProjectInfos) {
        this.consumerProjectInfos = consumerProjectInfos;
    }

    public List<AutoParts> getAutoParts() {
        return autoParts;
    }

    public void setAutoParts(List<AutoParts> autoParts) {
        this.autoParts = autoParts;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public Coupon getCoupon() {
        return coupon;
    }

    public void setCoupon(Coupon coupon) {
        this.coupon = coupon;
    }

    public ClientOrderImg getClientOrderImg() {
        return clientOrderImg;
    }

    public void setClientOrderImg(ClientOrderImg clientOrderImg) {
        this.clientOrderImg = clientOrderImg;
    }

    public StaffOrderImg getStaffOrderImg() {
        return staffOrderImg;
    }

    public void setStaffOrderImg(StaffOrderImg staffOrderImg) {
        this.staffOrderImg = staffOrderImg;
    }

    public StaffInfo getStaffInfo() {
        return staffInfo;
    }

    public void setStaffInfo(StaffInfo staffInfo) {
        this.staffInfo = staffInfo;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public String getDoorId() {
        return doorId;
    }

    public void setDoorId(String doorId) {
        this.doorId = doorId;
    }

    public Ark getArk() {
        return ark;
    }

    public void setArk(Ark ark) {
        this.ark = ark;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"consumerOrder\":")
                .append(consumerOrder);
        sb.append(",\"consumerProjectInfos\":")
                .append(consumerProjectInfos);
        sb.append(",\"autoParts\":")
                .append(autoParts);
        sb.append(",\"doorId\":\"")
                .append(doorId).append('\"');
        sb.append(",\"card\":")
                .append(card);
        sb.append(",\"coupon\":")
                .append(coupon);
        sb.append(",\"clientOrderImg\":")
                .append(clientOrderImg);
        sb.append(",\"staffOrderImg\":")
                .append(staffOrderImg);
        sb.append(",\"staffInfo\":")
                .append(staffInfo);
        sb.append(",\"store\":")
                .append(store);
        sb.append('}');
        return sb.toString();
    }
}
