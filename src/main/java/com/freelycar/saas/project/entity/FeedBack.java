package com.freelycar.saas.project.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * @Author: pyt
 * @Date: 2021/1/15 9:52
 * @Description:
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedBack implements Serializable {
    private static final long serialVersionUID = -5254690921984217937L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 联系人
     */
    private String username;

    /**
     * 联系方式
     */
    private String contactNumber;
    /**
     * 问题描述
     */
    @Column(columnDefinition = "varchar(100)")
    private String description;

    /**
     * 反馈类型
     */
    private String feedBackTypes;

    @Column(nullable = false, columnDefinition = "datetime default NOW()")
    private Timestamp createTime;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "fb_id")
    private Set<FeedBackImg> imgs = new HashSet<>();




}
