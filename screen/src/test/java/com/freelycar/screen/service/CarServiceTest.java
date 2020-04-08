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
 * @date 2020/4/8 14:57
 * @email 2630451673@qq.com
 * @desc
 */
@SpringBootTest
@RunWith(SpringRunner.class)
class CarServiceTest {
    @Autowired
    private CarService carService;

    @Test
    void getCarBrand() {
        List<Map<String, Integer>> list = carService.getCarBrand();
        JSONObject result = new JSONObject();
        result.put("list", list);
        System.out.println(result);
    }
}