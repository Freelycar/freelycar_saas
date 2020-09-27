package com.freelycar.saas.project.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-09-25
 * Time: 15:02
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RspStoreModel {
    private String id;
    private String name;
    private String address;
    private Boolean isArk;
}
