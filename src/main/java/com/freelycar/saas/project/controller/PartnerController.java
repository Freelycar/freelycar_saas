package com.freelycar.saas.project.controller;

import com.freelycar.saas.aop.LoggerManage;
import com.freelycar.saas.basic.wrapper.PaginationRJO;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.project.service.PartnerService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author tangwei - Toby
 * @date 2019/8/23
 * @email toby911115@gmail.com
 */
@RestController
@RequestMapping("/partner")
public class PartnerController {
    @Autowired
    private PartnerService partnerService;

    @ApiOperation(value = "获取合作意向列表（分页）", produces = "application/json")
    @GetMapping(value = "/list")
    @LoggerManage(description = "调用方法：获取合作意向列表（分页)）")
    public ResultJsonObject list(
            @RequestParam Integer currentPage,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ResultJsonObject.getDefaultResult(PaginationRJO.of(partnerService.list(currentPage, pageSize)));
    }
}
