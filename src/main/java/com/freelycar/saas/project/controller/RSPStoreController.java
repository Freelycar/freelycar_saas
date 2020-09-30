package com.freelycar.saas.project.controller;

import com.freelycar.saas.aop.LoggerManage;
import com.freelycar.saas.basic.wrapper.PaginationRJO;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.project.service.RSPStoreService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-09-25
 * Time: 14:42
 */
@Api(value = "服务商网点", description = "服务商网点接口", tags = "服务商网点接口")
@RestController
@RequestMapping("/sp/store")
public class RSPStoreController {
    private RSPStoreService rspStoreService;

    @Autowired
    public void setRspStoreService(RSPStoreService rspStoreService) {
        this.rspStoreService = rspStoreService;
    }

    @ApiOperation(value = "获取服务商网点列表（分页）", produces = "application/json")
    @GetMapping("/list")
    @LoggerManage(description = "调用方法：获取服务商网点列表（分页）")
    public ResultJsonObject list(
            @RequestParam String rspId,
            @RequestParam(required = false) String name,
            @RequestParam Integer currentPage,
            @RequestParam(required = false) Integer pageSize) {
        if (StringUtils.isEmpty(name)) {
            name = "";
        }
        return ResultJsonObject.getDefaultResult(PaginationRJO.of(rspStoreService.list(rspId, name, currentPage, pageSize)));
    }

    @ApiOperation(value = "获取服务商网点列表", produces = "application/json")
    @GetMapping("/listStore")
    @LoggerManage(description = "调用方法：获取服务商网点列表")
    public ResultJsonObject listStore(
            @RequestParam String rspId) {
        return ResultJsonObject.getDefaultResult(rspStoreService.listStore(rspId));
    }

    @ApiOperation(value = "关闭服务商网点智能柜服务", produces = "application/json")
    @PostMapping(value = "/close")
    @LoggerManage(description = "调用方法：关闭服务商网点智能柜服务")
    public ResultJsonObject closeArk(@RequestBody String[] ids, @RequestParam String rspId) {
        return rspStoreService.closeArk(ids, rspId);
    }

    @ApiOperation(value = "开通服务商网点智能柜服务", produces = "application/json")
    @PostMapping(value = "/open")
    @LoggerManage(description = "调用方法：开通服务商网点智能柜服务")
    public ResultJsonObject openArk(@RequestBody String[] ids, @RequestParam String rspId) {
        return rspStoreService.openArk(ids, rspId);
    }
}
