package com.freelycar.saas.project.controller;

import com.freelycar.saas.aop.LoggerManage;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.DataIsExistException;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.project.entity.RealServiceProvider;
import com.freelycar.saas.project.service.RealServiceProviderService;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sp")
public class RealServiceProviderController {
    private Logger logger = LoggerFactory.getLogger(RealServiceProviderController.class);
    private RealServiceProviderService realServiceProviderService;
    private String errorMsg;
    @Autowired
    public void setRealServiceProviderService(RealServiceProviderService realServiceProviderService) {
        this.realServiceProviderService = realServiceProviderService;
    }

    @PostMapping(value = "/modify")
    @LoggerManage(description = "调用方法：服务商新增/修改")
    public ResultJsonObject saveOrUpdate(@RequestBody RealServiceProvider serviceProvider) {
        if (null == serviceProvider) {
            errorMsg = "接收到的参数：ServiceProvider为NULL";
            logger.error(errorMsg);
            return ResultJsonObject.getErrorResult(null, errorMsg);
        }
        try {
            return ResultJsonObject.getDefaultResult(realServiceProviderService.modify(serviceProvider));
        } catch (DataIsExistException | ObjectNotFoundException e) {
            logger.error(e.getMessage(), e);
//            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }

    @ApiOperation(value = "删除服务商信息", produces = "application/json")
    @GetMapping(value = "/delete")
    @LoggerManage(description = "调用方法：删除服务商信息")
    public ResultJsonObject delete(@RequestParam String id) {
        return null;
    }
}
