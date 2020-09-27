package com.freelycar.saas.project.controller;

import com.freelycar.saas.aop.LoggerManage;
import com.freelycar.saas.basic.wrapper.PaginationRJO;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.BatchDeleteException;
import com.freelycar.saas.exception.DataIsExistException;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.project.entity.RealServiceProvider;
import com.freelycar.saas.project.service.RealServiceProviderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Api(value = "服务商管理", description = "新的服务商管理接口", tags = "服务商管理接口")
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

    @ApiOperation(value = "新增/修改服务商", produces = "application/json")
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
    @PostMapping(value = "/delete")
    @LoggerManage(description = "调用方法：删除服务商信息")
    public ResultJsonObject delete(@RequestBody String[] ids) {
        try {
            return realServiceProviderService.delete(ids);
        } catch (BatchDeleteException e) {
//            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }

    /**
     * 获取服务商列表（分页）
     *
     * @param name
     * @param currentPage
     * @param pageSize
     * @return
     */
    @ApiOperation(value = "获取服务商列表（分页）", produces = "application/json")
    @GetMapping("/list")
    @LoggerManage(description = "调用方法：获取服务商列表（分页）")
    public ResultJsonObject list(
            @RequestParam(required = false) String name,
            @RequestParam Integer currentPage,
            @RequestParam(required = false) Integer pageSize) {
        if (StringUtils.isEmpty(name)) {
            name = "";
        }
        return ResultJsonObject.getDefaultResult(PaginationRJO.of(realServiceProviderService.list(name, currentPage, pageSize)));
    }
}
