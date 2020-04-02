package com.freelycar.screen.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author pyt
 * @date 2020/3/31 14:18
 * @email 2630451673@qq.com
 * @desc
 */
@RestController
public class TestController {
    @GetMapping("/test")
    public String test(){
        return "test";
    }
}
