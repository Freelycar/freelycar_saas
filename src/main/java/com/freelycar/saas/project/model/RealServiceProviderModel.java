package com.freelycar.saas.project.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-10-14
 * Time: 16:36
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RealServiceProviderModel {
    private String id;
    private String name;
    private String address;
    private BigDecimal sort;
}
