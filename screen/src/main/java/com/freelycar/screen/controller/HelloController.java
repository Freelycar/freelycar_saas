package com.freelycar.screen.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author pyt
 * @date 2020/3/31 13:11
 * @email 2630451673@qq.com
 * @desc
 */
@RestController
public class HelloController {
    @GetMapping("/hello")
    public String Hello(){
        return "hello";
    }
}
