package com.freelycar.saas.project.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * @Author: pyt
 * @Date: 2021/1/15 9:59
 * @Description:
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedBackImg implements Serializable {
    private static final long serialVersionUID = -7304472973783631574L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imgUrl;
}
