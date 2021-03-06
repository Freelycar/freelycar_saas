package com.freelycar.saas.wechat.model;

import com.freelycar.saas.project.entity.Staff;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author tangwei - Toby
 * @date 2019-02-11
 * @email toby911115@gmail.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeChatStaff {
    private String jwt;

    private Staff staff;
}
