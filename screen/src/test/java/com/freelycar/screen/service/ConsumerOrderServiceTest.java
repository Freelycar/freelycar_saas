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
 * @date 2020/4/1 16:32
 * @email 2630451673@qq.com
 * @desc
 */
@SpringBootTest
@RunWith(SpringRunner.class)
class ConsumerOrderServiceTest {
    @Autowired
    private ConsumerOrderService consumerOrderService;

    @Test
    void getMonthlyAddtions() {
        JSONObject result = consumerOrderService.getMonthlyAdditions();
        System.out.println(result.get("storeRanking"));
    }
}