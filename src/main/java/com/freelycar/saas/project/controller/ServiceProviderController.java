package com.freelycar.saas.project.controller;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.aop.LoggerManage;
import com.freelycar.saas.basic.wrapper.PaginationRJO;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.exception.DataIsExistException;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.project.entity.ProjectType;
import com.freelycar.saas.project.entity.ServiceProvider;
import com.freelycar.saas.project.service.ProjectTypeService;
import com.freelycar.saas.project.service.ServiceProviderService;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * @author puyuting
 * @date 2019/12/23
 * @email 2630451673@qq.com
 */
@RestController
@RequestMapping("/serviceProvider")
public class ServiceProviderController {
    private Logger logger = LoggerFactory.getLogger(ProjectTypeController.class);
    private String errorMsg;
    @Autowired
    private ServiceProviderService serviceProviderService;

    @PostMapping(value = "/modify")
    @LoggerManage(description = "调用方法：服务商新增/修改")
    public ResultJsonObject saveOrUpdate(@RequestBody ServiceProvider serviceProvider) {
        if (null == serviceProvider) {
            errorMsg = "接收到的参数：ServiceProvider为NULL";
            logger.error(errorMsg);
            return ResultJsonObject.getErrorResult(null, errorMsg);
        }
        try {
            return ResultJsonObject.getDefaultResult(serviceProviderService.modify(serviceProvider));
        } catch (DataIsExistException | ObjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }

    /**
     * 删除操作（软删除）
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "删除服务商信息", produces = "application/json")
    @GetMapping(value = "/delete")
    @LoggerManage(description = "调用方法：删除服务商信息")
    public ResultJsonObject delete(@RequestParam String id) {
        return serviceProviderService.delete(id);
    }

    /**
     * 批量删除(软删除)
     *
     * @param ids
     * @return
     */
    @ApiOperation(value = "批量删除服务商信息", produces = "application/json")
    @PostMapping("/batchDelete")
    @LoggerManage(description = "调用方法：批量删除服务商信息")
    public ResultJsonObject batchDelete(@RequestBody JSONObject ids) {
        if (null == ids) {
            return ResultJsonObject.getErrorResult(null, "ids参数为NULL");
        }
        return serviceProviderService.delByIds(ids.getString("ids"));
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
        return ResultJsonObject.getDefaultResult(PaginationRJO.of(serviceProviderService.list(name, currentPage, pageSize)));
    }

    @ApiOperation(value = "查询服务商详情）", produces = "application/json")
    @GetMapping("/detail")
    @LoggerManage(description = "调用方法：查询服务商详情")
    public ResultJsonObject detail(@RequestParam String id) {
        try {
            return ResultJsonObject.getDefaultResult(serviceProviderService.detail(id));
        } catch (ArgumentMissingException | ObjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }
}
