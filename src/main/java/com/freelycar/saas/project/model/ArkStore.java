package com.freelycar.saas.project.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: pyt
 * @Date: 2021/3/19 9:44
 * @Description:
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArkStore {
    //设备编码
    private String sn;
    //设备位置
    private String location;
    //网点名称
    private String name;

    @Override
    public String toString() {
        return "ArkStore{" +
                "sn='" + sn + '\'' +
                ", location='" + location + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
