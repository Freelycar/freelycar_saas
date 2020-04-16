package com.freelycar.saas.screen.controller;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.screen.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author pyt
 * @date 2020/3/31 14:18
 * @email 2630451673@qq.com
 * @desc
 */
@RestController
@RequestMapping("/screen")
public class TestController {
    @Autowired
    private WeatherService weatherService;

    @GetMapping("/test")
    public String test() {
        return "test";
    }

    @GetMapping("/weather")
    public JSONObject weather() {
        return weatherService.getWeather();
    }
}
