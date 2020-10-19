package com.freelycar.saas.wechat.controller;

import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.*;
import com.freelycar.saas.project.model.OrderObject;
import com.freelycar.saas.project.service.*;
import com.freelycar.saas.wechat.model.BaseOrderInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author tangwei - Toby
 * @date 2019-01-25
 * @email toby911115@gmail.com
 */

@RestController
@RequestMapping("/wechat/ark")
public class WeChatArkController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ConsumerOrderService consumerOrderService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ArkService arkService;

    @Autowired
    private DoorService doorService;

    @Autowired
    private RSPProjectService rspProjectService;

    @GetMapping("/getActiveOrder")
    public ResultJsonObject getActiveOrder(@RequestParam String clientId) {
        try {
            List<BaseOrderInfo> res = consumerOrderService.findAllOrdersByClientId(clientId, "ark");
            return ResultJsonObject.getDefaultResult(res);
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }

    @PostMapping("/orderService")
    public ResultJsonObject orderService(@RequestBody OrderObject orderObject) {
        logger.info("arkOrderLog:生成智能柜订单接口----------");
        try {
            return consumerOrderService.arkHandleOrder(orderObject);
        } catch (ArgumentMissingException | ObjectNotFoundException | NoEmptyArkException | OpenArkDoorTimeOutException | InterruptedException | OpenArkDoorFailedException | UpdateDataErrorException | NormalException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, "智能柜开单失败：" + e.getMessage() + "，请稍后重试或联系门店");
        }
    }

    @GetMapping("/cancelOrderService")
    public ResultJsonObject cancelOrderService(@RequestParam String id) {
        logger.info("arkOrderLog:用户取消订单接口----------");
        String errorMessage;
        try {
            return consumerOrderService.cancelOrder(id);
        } catch (ArgumentMissingException | OpenArkDoorFailedException | OpenArkDoorTimeOutException | InterruptedException | ObjectNotFoundException | NormalException e) {
            errorMessage = e.getMessage();
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
        return ResultJsonObject.getErrorResult(null, errorMessage);
    }

    @GetMapping("/getProjects")
    public ResultJsonObject getProjects(@RequestParam String storeId, @RequestParam(required = false) Boolean newUser) {
        if (null == newUser) {
            newUser = false;
        }

        try {
            return ResultJsonObject.getDefaultResult(projectService.getProjects(storeId, newUser));
        } catch (ArgumentMissingException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null);
        }
    }

    @GetMapping("/getRspProjects")
    public ResultJsonObject getRspProjects(@RequestParam String storeId, @RequestParam(required = false) Boolean newUser) {
        if (null == newUser) {
            newUser = false;
        }
        try {
            return ResultJsonObject.getDefaultResult(rspProjectService.getRspProjects(storeId, newUser));
        } catch (ArgumentMissingException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null);
        }
    }

    @GetMapping("/orderFinish")
    public ResultJsonObject orderFinish(@RequestParam String id) {
        logger.info("arkOrderLog:用户取车，订单完结接口----------");
        try {
            return consumerOrderService.orderFinish(id);
        } catch (Exception e) {
            logger.error("用户取车出现异常", e);
            e.printStackTrace();
        }
        return ResultJsonObject.getErrorResult(null, "用户取车出现异常，请稍后重试或联系门店。");
    }

    @GetMapping("/pickCar")
    public ResultJsonObject pickCar(@RequestParam String orderId, @RequestParam String staffId) {
        logger.info("arkOrderLog:技师接车开始服务接口----------");
        try {
            return consumerOrderService.pickCar(orderId, staffId);
        } catch (Exception e) {
            logger.error("技师取车出现异常", e);
            e.printStackTrace();
        }
        return ResultJsonObject.getErrorResult(null, "技师取车出现异常，请稍后重试或联系门店。");
    }

    @PostMapping("/finishCar")
    public ResultJsonObject finishCar(@RequestBody OrderObject orderObject) {
        logger.info("arkOrderLog:技师完工还车接口----------");
        String errorMessage;
        try {
            return consumerOrderService.finishCar(orderObject);
        } catch (NoEmptyArkException e1) {
            errorMessage = "没有可用的空智能柜";
            logger.error(errorMessage, e1);
            e1.printStackTrace();
        } catch (Exception e) {
            errorMessage = "技师完工还车-智能柜服务出现异常";
            logger.error(errorMessage, e);
            e.printStackTrace();
        }
        return ResultJsonObject.getErrorResult(null, errorMessage + "，请稍后重试或联系门店。");
    }

    @GetMapping("/getCurrentArkLocation")
    public ResultJsonObject getCurrentArkLocation(@RequestParam String arkSn) {
        return arkService.getCurrentArkLocation(arkSn);
    }

    @GetMapping("/getArkInfo")
    public ResultJsonObject getArkInfo(@RequestParam String arkSn) {
        return arkService.getArkInfo(arkSn);
    }

    @GetMapping("/getEmptyDoor")
    public ResultJsonObject getEmptyDoor(@RequestParam String arkSn) {
        logger.info("arkOrderLog:用户下单分配柜号接口----------");
        try {
            return ResultJsonObject.getDefaultResult(doorService.getUsefulDoor(arkSn));
        } catch (NoEmptyArkException | ArgumentMissingException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }

    @GetMapping("/getStaffKeyLocation")
    public ResultJsonObject getStaffKeyLocation(@RequestParam String orderId) {
        try {
            return ResultJsonObject.getDefaultResult(consumerOrderService.getStaffKeyLocation(orderId));
        } catch (ArgumentMissingException | ObjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }
}
