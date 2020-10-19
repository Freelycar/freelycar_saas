package com.freelycar.saas.project.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-10-14
 * Time: 11:11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RspProjectModel {
    private String id;
    private String name;
    //服务商名称
    private String rspName;
    private Double price;
    private String comment;
    private Timestamp createTime;
    //上架-true，下架-false
    private boolean bookOnline;
}
