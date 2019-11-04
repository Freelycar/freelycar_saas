package com.freelycar.saas.permission.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.validator.constraints.Length;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;

@Entity
@Data
public class SysResource {
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
     * 权限名称
     */
    private String perName;

    /**
     * 权限描述
     */
    private String description;

    /**
     * 权限链接
     */
    private String url;

    /**
     * 所属系统
     */
    private String sysId;

    /**
     * 父级权限节点
     */
    private String parentId;

    /**
     * 排序
     */
    private Long sort;

    /**
     * 0=系统菜单
     * 1=列表菜单
     * 2=二级菜单
     */
    private String component;

    /**
     * Vue路由
     */
    private String path;
}
