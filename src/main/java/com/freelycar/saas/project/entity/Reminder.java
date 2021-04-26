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
import java.util.Date;

/**
 * @Author: pyt
 * @Date: 2021/4/21 10:20
 * @Description: 提醒消息
 */
@Entity
@Table
@DynamicInsert
@DynamicUpdate
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Reminder implements Serializable {
    private static final long serialVersionUID = 4893163397954259834L;
    @Id
    @GenericGenerator(name = "uuid", strategy = "uuid")
    @GeneratedValue(generator = "uuid")
    @NotNull
    @Length(max = 50)
    private String id;
    /**
     * 消息类型
     */
    private String type;
    /**
     * 是否已读，默认未读
     */
    @Column(nullable = false, columnDefinition = "bit default 0", name = "isRead")
    private Boolean read;

    /**
     * 消息主体
     */
    private String message;
    @Length(max = 50)
    private String toClient;
    @Length(max = 50)
    private String toEmployee;

    /**
     * 创建时间
     */
    @Column(nullable = false, columnDefinition = "datetime default NOW()")
    private Timestamp createTime;
}
