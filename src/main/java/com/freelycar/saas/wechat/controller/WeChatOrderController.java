package com.freelycar.saas.wechat.controller;

import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.project.entity.ConsumerOrder;
import com.freelycar.saas.project.service.ConsumerOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author tangwei - Toby
 * @date 2019-01-07
 * @email toby911115@gmail.com
 */
@RestController
@RequestMapping("/wechat/order")
public class WeChatOrderController {

    @Autowired
    private ConsumerOrderService consumerOrderService;

    @GetMapping("/listOrders")
    public ResultJsonObject listOrders(
            @RequestParam String clientId,
            @RequestParam String type
    ) {
        List<ConsumerOrder> res = consumerOrderService.findAllOrdersByTypeAndClientId(clientId, type);
        if (null != res) {
            return ResultJsonObject.getDefaultResult(res);
        }
        return ResultJsonObject.getErrorResult(null);
    }
}