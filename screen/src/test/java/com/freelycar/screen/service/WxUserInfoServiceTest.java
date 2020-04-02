package com.freelycar.screen.service;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;

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
        List<Map<String, Integer>> result = wxUserInfoService.getMonthlyAdditions();
        for (Map<String, Integer> r :
                result) {
            for (String name :
                    r.keySet()) {
                System.out.println(name + ":" + r.get(name));
            }
        }
    }
}