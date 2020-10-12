package com.freelycar.saas.project.model;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-10-10
 * Time: 16:21
 */
@Data
public class CumulateOrder {
    private String createDate;
    private Long orderCount;
    private Long registerUserCount;
}
