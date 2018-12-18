package com.freelycar.saas.project.controller;

import com.freelycar.saas.aop.LoggerManage;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.project.entity.CardService;
import com.freelycar.saas.project.service.CardServiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cardService")
public class CardServiceController {
    private static Logger logger = LoggerFactory.getLogger(CardServiceController.class);
    @Autowired
    CardServiceService cardServiceService;
    private String errorMsg;

    /**
     * 新增/修改项目
     *
     * @param cardService
     * @return
     */
    @PostMapping(value = "/modify")
    @LoggerManage(description = "调用方法：卡类新增/修改")
    public ResultJsonObject saveOrUpdate(CardService cardService) {
        if (null == cardService) {
            errorMsg = "接收到的参数：cardService为NULL";
            logger.error(errorMsg);
            return ResultJsonObject.getErrorResult(null, errorMsg);
        }
        return cardServiceService.modify(cardService);
    }
    /**
     * 获取卡类对象
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/detail")
    @LoggerManage(description = "调用方法：获取卡类详情")
    public ResultJsonObject detail(@RequestParam String id) {
        if (null == id) {
            errorMsg = "接收到的参数：id为NULL";
            logger.error(errorMsg);
            return ResultJsonObject.getErrorResult(null, errorMsg);
        }
        return cardServiceService.getDetail(id);
    }

    /**
     * 获取卡类列表
     * @param storeId
     * @param currentPage
     * @return
     */
    @GetMapping(value = "/list")
    @LoggerManage(description = "调用方法：获取卡类列表")
    public ResultJsonObject list(@RequestParam String storeId, @RequestParam Integer currentPage ) {
        return ResultJsonObject.getDefaultResult(cardServiceService.list(storeId, currentPage));
    }

}