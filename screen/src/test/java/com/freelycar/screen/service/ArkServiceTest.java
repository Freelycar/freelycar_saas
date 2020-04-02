package com.freelycar.screen.service;

import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author pyt
 * @date 2020/4/1 14:58
 * @email 2630451673@qq.com
 * @desc
 */
@SpringBootTest
@RunWith(SpringRunner.class)
class ArkServiceTest {
    @Autowired
    private ArkService arkService;

    @Test
    void getMonthlyAdditions() {
        JSONObject result = arkService.getMonthlyAdditions();
        System.out.println(result);
    }

    @Test
    void testGetMonthlyAdditions() {
    }
}