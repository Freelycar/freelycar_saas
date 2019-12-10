package com.freelycar.saas.project.controller;


import com.freelycar.saas.aop.LoggerManage;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.project.service.WxUserInfoService;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author tangwei
 * @date 2018/9/25
 */
@RestController
@RequestMapping("/wxUserInfo")
public class WxUserInfoController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private WxUserInfoService wxUserInfoService;

    /**
     * 根据日期区间查找关注数、注册数、订单数
     *
     * @param storeId
     * @param refDate
     * @return
     */
    @ApiOperation(value = "获取系统三项统计")
    @GetMapping("/getCumulateThree")
    @LoggerManage(description = "调用方法：获取系统三项统计")
    public ResultJsonObject getCumulateThree(
            @RequestParam(required = false) String storeId,
            @RequestParam String refDate
    ) {
        try {
            return ResultJsonObject.getDefaultResult(wxUserInfoService.getCumulateThree(storeId, refDate));
        } catch (ArgumentMissingException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }
}
