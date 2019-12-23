package com.freelycar.saas.wechat.controller;

import com.freelycar.saas.basic.wrapper.ResultCode;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.exception.NoEmptyArkException;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.project.service.ArkService;
import com.freelycar.saas.project.service.ConsumerOrderService;
import com.freelycar.saas.wechat.model.BaseOrderInfo;
import com.freelycar.saas.wechat.model.FinishOrderInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(WeChatOrderController.class);

    @Autowired
    private ConsumerOrderService consumerOrderService;
    @Autowired
    private ArkService arkService;

    @GetMapping("/listOrders")
    public ResultJsonObject listOrders(
            @RequestParam String arkSn,
            @RequestParam String clientId,
            @RequestParam String type
    ) {
        try {
            boolean flag = false;
            List<BaseOrderInfo> res = consumerOrderService.findAllOrdersByClientId(clientId, type);
            if (res.size() > 0) {
                for (BaseOrderInfo info :
                        res) {
                    if (info.getState() < 3) {
                        flag = true;
                        break;
                    }
                }
            }
            if (flag) {//用户名下有进行中订单
                return ResultJsonObject.getDefaultResult(res);
            } else {//检查进行中订单数是否小于柜门数
                if (arkService.checkArk(arkSn)) return ResultJsonObject.getDefaultResult(null);
                else return ResultJsonObject.getErrorResult(null, ResultCode.ARK_FULL.message());
            }
        } catch (IllegalArgumentException | NoEmptyArkException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }


    @GetMapping("/getOrderDetail")
    public ResultJsonObject getOrderDetail(@RequestParam String id) {
        return consumerOrderService.getOrderObjectDetail(id);
    }

    @GetMapping("/listReservationOrders")
    public ResultJsonObject listReservationOrders(
            @RequestParam String licensePlate,
            @RequestParam String storeId,
            @RequestParam String staffId
    ) {
        try {
            return ResultJsonObject.getDefaultResult(consumerOrderService.listReservationOrders(licensePlate, storeId, staffId));

        } catch (ArgumentMissingException | ObjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null);
        }
    }


    @GetMapping("/listServicingOrders")
    public ResultJsonObject listServicingOrders(
            @RequestParam String licensePlate,
            @RequestParam String storeId,
            @RequestParam String staffId
    ) {
        List<FinishOrderInfo> res = consumerOrderService.listServicingOrders(licensePlate, storeId, staffId);
        if (null != res) {
            return ResultJsonObject.getDefaultResult(res);
        }
        return ResultJsonObject.getErrorResult(null);
    }

}
