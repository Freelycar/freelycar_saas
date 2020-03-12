package com.freelycar.saas.project.controller;

import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.*;
import com.freelycar.saas.project.service.ConsumerOrderService;
import com.freelycar.saas.project.service.EdaijiaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author puyuting
 * @date 2020/1/13
 * @email 2630451673@qq.com
 */
@RestController
public class EdaijiaController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private EdaijiaService edaijiaService;

    @Autowired
    private ConsumerOrderService consumerOrderService;

    @GetMapping("/verifyCode")
    public ResultJsonObject verifyCode(
            @RequestParam String sign,
            @RequestParam Integer password,
            @RequestParam Integer orderId) {
        logger.info("arkOrderLog:代驾取/还车接口----------");
        String errorMessage;
        try {
            return edaijiaService.verifyCode(sign, password, orderId);
        } catch (ObjectNotFoundException | InterruptedException e) {
            errorMessage = e.getMessage();
            e.printStackTrace();
            logger.error(errorMessage, e);
        } catch (OpenArkDoorFailedException e) {
            errorMessage = "智能柜开门失败";
            e.printStackTrace();
            logger.error(errorMessage, e);
        } catch (ArgumentMissingException e) {
            errorMessage = "智能柜开门故障";
            e.printStackTrace();
            logger.error(errorMessage, e);
        } catch (OpenArkDoorTimeOutException e) {
            errorMessage = "智能柜开门超时";
            e.printStackTrace();
            logger.error(errorMessage, e);
        } catch (NoEmptyArkException e) {
            errorMessage = "没有可分配的智能柜";
            e.printStackTrace();
            logger.error(errorMessage, e);
        }
        return ResultJsonObject.getErrorResult(null, errorMessage + "，请稍后重试或联系门店。");
    }

    @GetMapping("/verifyCodeAndOpenDoor")
    public ResultJsonObject verifyCodeAndOpenDoor(
            @RequestParam String sign,
            @RequestParam Integer password,
            @RequestParam Integer orderId,
            @RequestParam String doorId) {
        logger.info("arkOrderLog:代驾取/还车接口----------");
        String errorMessage;
        try {
            return edaijiaService.verifyCodeAndOpenDoor(sign, password, orderId,doorId);
        } catch (ObjectNotFoundException | InterruptedException e) {
            errorMessage = e.getMessage();
            e.printStackTrace();
            logger.error(errorMessage, e);
        } catch (OpenArkDoorFailedException e) {
            errorMessage = "智能柜开门失败";
            e.printStackTrace();
            logger.error(errorMessage, e);
        } catch (ArgumentMissingException e) {
            errorMessage = "智能柜开门故障";
            e.printStackTrace();
            logger.error(errorMessage, e);
        } catch (OpenArkDoorTimeOutException e) {
            errorMessage = "智能柜开门超时";
            e.printStackTrace();
            logger.error(errorMessage, e);
        } catch (NoEmptyArkException e) {
            errorMessage = "没有可分配的智能柜";
            e.printStackTrace();
            logger.error(errorMessage, e);
        }
        return ResultJsonObject.getErrorResult(null, errorMessage + "，请稍后重试或联系门店。");
    }

    @GetMapping("/edaijiList")
    public ResultJsonObject edaijiList(
            @RequestParam Integer currentPage,
            @RequestParam Integer pageSize) {
        logger.info("consumerOrderLog:代驾订单列表接口----------");
        return consumerOrderService.EdaijiaList(currentPage, pageSize);
    }

    @GetMapping("/confirmService")
    public ResultJsonObject confirmService(@RequestParam String orderId) {
        logger.info("consumerOrderLog:服务商完成服务接口----------");
        String errorMessage;
        try {
            return edaijiaService.confirmService(orderId);
        } catch (ObjectNotFoundException e) {
            errorMessage = "未查询到对应订单";
            e.printStackTrace();
            logger.error(errorMessage, e);
        } catch (NormalException e){
            errorMessage = "创建e代驾订单失败";
            e.printStackTrace();
            logger.error(errorMessage, e);
        }
        return ResultJsonObject.getErrorResult(null, errorMessage + "，请稍后重试或联系门店。");
    }
}
