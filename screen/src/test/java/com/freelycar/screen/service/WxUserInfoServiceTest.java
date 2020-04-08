package com.freelycar.screen.service;

import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author pyt
 * @date 2020/4/1 16:11
 * @email 2630451673@qq.com
 * @desc
 */
@SpringBootTest
@RunWith(SpringRunner.class)
class WxUserInfoServiceTest {
    @Autowired
    private WxUserInfoService wxUserInfoService;

    @Test
    void getMonthlyAdditions() {
        JSONObject  result = wxUserInfoService.getMonthlyAdditions();
        System.out.println(result);
    }
}