package com.freelycar.saas.project.controller;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.aop.LoggerManage;
import com.freelycar.saas.basic.wrapper.PaginationRJO;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.*;
import com.freelycar.saas.permission.service.SysUserService;
import com.freelycar.saas.project.model.StoreAccount;
import com.freelycar.saas.project.model.StoreInfo;
import com.freelycar.saas.project.service.StoreService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.Map;

/**
 * @author tangwei - Toby
 * @date 2019-01-07
 * @email toby911115@gmail.com
 */
@Api(value = "门店信息", description = "门店信息服务接口", tags = "门店信息服务接口")
@RestController
@RequestMapping("/store")
public class StoreController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private StoreService storeService;

    @Autowired
    private SysUserService sysUserService;


    /**
     * 新增/修改门店
     *
     * @param storeAccount 门店-账号
     * @return
     */
    /*@ApiOperation(value = "新增/修改门店", produces = "application/json")
    @PostMapping("/modify")
    @LoggerManage(description = "调用方法：新增/修改门店")
    public ResultJsonObject modify(@RequestBody Store store) {
        Store storeRes;
        try {
            storeRes = storeService.saveOrUpdate(store);
        } catch (ArgumentMissingException | ObjectNotFoundException | NumberOutOfRangeException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
        return ResultJsonObject.getDefaultResult(storeRes);
    }*/
    @ApiOperation(value = "新增/修改门店", produces = "application/json")
    @PostMapping("/modify")
    @LoggerManage(description = "调用方法：新增/修改门店")
    public ResultJsonObject modify(@RequestBody StoreAccount storeAccount) {
        StoreAccount storeRes;
        try {
            storeRes = storeService.saveOrUpdate(storeAccount);
        } catch (ArgumentMissingException | ObjectNotFoundException | NumberOutOfRangeException | UnknownException | DataIsExistException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
        return ResultJsonObject.getDefaultResult(storeRes);
    }


    /**
     * 删除门店
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "单个删除", produces = "application/json")
    @GetMapping("/delete")
    @LoggerManage(description = "调用方法：单个删除门店信息")
    public ResultJsonObject delete(String id) {
        return storeService.deleteStoreAndAccount(id);
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @ApiOperation(value = "批量删除", produces = "application/json")
    @PostMapping("/batchDelete")
    @LoggerManage(description = "调用方法：批量删除门店信息")
    public ResultJsonObject batchDelete(@RequestBody JSONObject ids) {
        if (null == ids) {
            return ResultJsonObject.getErrorResult(null, "ids参数为NULL");
        }
        return storeService.deleteStoreAndAccountByIds(ids.getString("ids"));
    }

    /**
     * 获取网点列表（分页）
     *
     * @param name
     * @param currentPage
     * @param pageSize
     * @return
     */
    @ApiOperation(value = "获取门店列表（分页）", produces = "application/json")
    @GetMapping("/list")
    @LoggerManage(description = "调用方法：获取门店列表（分页）")
    public ResultJsonObject list(
            @RequestParam(required = false) String name,
            @RequestParam Integer currentPage,
            @RequestParam(required = false) Integer pageSize) {
        if (StringUtils.isEmpty(name)) {
            name = "";
        }
        return ResultJsonObject.getDefaultResult(PaginationRJO.of(storeService.list(name, currentPage, pageSize)));
    }

    /**
     * 获取网点列表
     *
     * @param name
     * @return
     */
    @ApiOperation(value = "获取门店列表", produces = "application/json")
    @GetMapping("/listAll")
    @LoggerManage(description = "调用方法：获取门店列表")
    public ResultJsonObject list(
            @RequestParam(required = false) String name) {
        if (StringUtils.isEmpty(name)) {
            name = "";
        }
        return ResultJsonObject.getDefaultResult(storeService.list(name));
    }

    /**
     * 获取网点账号列表（分页）
     *
     * @param name
     * @param currentPage
     * @param pageSize
     * @return
     */
    @ApiOperation(value = "获取网点账号列表（分页）", produces = "application/json")
    @GetMapping("/listStoreAccount")
    @LoggerManage(description = "调用方法：获取门店列表（分页）")
    public ResultJsonObject listStoreAccount(
            @RequestParam(required = false) String name,
            @RequestParam Integer currentPage,
            @RequestParam(required = false) Integer pageSize) {
        if (StringUtils.isEmpty(name)) {
            name = "";
        }
        return ResultJsonObject.getDefaultResult(PaginationRJO.of(storeService.listStoreAccount(name, currentPage, pageSize)));
    }

    @ApiOperation(value = "修改门店信息（门店端）", produces = "application/json")
    @PostMapping("/confirmInfo")
    @LoggerManage(description = "调用方法：修改门店信息（门店端）")
    public ResultJsonObject confirmInfo(@RequestBody StoreInfo storeInfo) {
        try {
            return storeService.confirmInfo(storeInfo);
        } catch (UpdateDataErrorException | ArgumentMissingException | ObjectNotFoundException | NumberOutOfRangeException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }

    @ApiOperation(value = "查询门店信息（门店端）", produces = "application/json")
    @GetMapping("/detail")
    @LoggerManage(description = "调用方法：查询门店信息（门店端）")
    public ResultJsonObject detail(@RequestParam String id) {
        try {
            return ResultJsonObject.getDefaultResult(storeService.detail(id));
        } catch (ArgumentMissingException | ObjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }

    @ApiOperation(value = "修改门店列表位置", produces = "application/json")
    @PostMapping("/switchLocation")
    @LoggerManage(description = "调用方法：修改门店列表位置")
    public ResultJsonObject switchLocation(@RequestBody Map<String, BigInteger> map) {
        boolean result = storeService.switchLocation(map);
        if (result) {
            return ResultJsonObject.getDefaultResult(null);
        } else {
            return ResultJsonObject.getErrorResult(null, "门店数据有误");
        }
    }
}
