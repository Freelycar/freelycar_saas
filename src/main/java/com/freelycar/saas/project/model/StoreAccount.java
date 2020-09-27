package com.freelycar.saas.project.model;

import com.freelycar.saas.permission.entity.SysUser;
import com.freelycar.saas.project.entity.Store;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.math.BigInteger;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-09-17
 * Time: 17:29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreAccount {
    /**
     * 网点id
     */
    private String storeId;
    /**
     * 网点名称
     */
    private String name;
    /**
     * 网店地址
     */
    private String address;
    /**
     * 网点备注
     */
    private String remark;
    /**
     * 网点排序
     */
    private BigInteger sort;
    /**
     * 账号id
     */
    private Long sysUserId;
    /**
     * 账号名
     */
    private String username;
    /**
     * 账号密码
     */
    private String password;
    /**
     * 账号是否关闭
     */
    private boolean openStatus;

    public Store toStore() {
        Store store = new Store();
        if (!StringUtils.isEmpty(this.storeId)) {
            store.setId(this.storeId);
        }
        store.setName(this.name);
        store.setAddress(this.address);
        store.setRemark(this.remark);
        store.setSort(this.sort);
        return store;
    }

    public SysUser toUser() {
        SysUser user = new SysUser();
        if (!StringUtils.isEmpty(this.sysUserId)) {
            user.setId(this.sysUserId);
        }
        user.setUsername(this.username);
        user.setPassword(this.password);
        return user;
    }
}
