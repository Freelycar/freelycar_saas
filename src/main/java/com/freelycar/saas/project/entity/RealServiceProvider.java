package com.freelycar.saas.project.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table
@DynamicInsert
@DynamicUpdate
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RealServiceProvider implements Serializable {
    private static final long serialVersionUID = -5424806474663395636L;

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
     * 服务商名称
     */
    @Column
    private String name;


    /**
     * 服务商地址
     */
    @Column
    private String address;

    /**
     * 纬度
     */
    @Column
    private Double latitude;
    /**
     * 经度
     */
    @Column
    private Double longitude;

    /**
     * 电话
     */
    @Column
    private String phone;

    /**
     * 备注（内容）
     */
    @Column
    private String comment;

    @Column(name = "service_status",nullable = false,columnDefinition = "bit default 0")
    private Boolean serviceStatus;


}
