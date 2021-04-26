package com.freelycar.saas.wechat.controller;

import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.project.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: pyt
 * @Date: 2021/4/21 16:27
 * @Description:
 */
@RestController
@RequestMapping("/wechat/reminder")
public class WeChatReminderController {
    @Autowired
    private ReminderService reminderService;

    @GetMapping("/list")
    public ResultJsonObject list(String id) {
        return ResultJsonObject.getDefaultResult(reminderService.listById(id));
    }

    @GetMapping("/count")
    public ResultJsonObject count(String id) {
        return ResultJsonObject.getDefaultResult(reminderService.count(id));
    }

    @GetMapping("/test")
    public ResultJsonObject test() {
        reminderService.createReminder();
        return ResultJsonObject.getDefaultResult(null);
    }
}
