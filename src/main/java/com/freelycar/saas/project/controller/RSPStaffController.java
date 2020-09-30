package com.freelycar.saas.project.controller;

import com.freelycar.saas.aop.LoggerManage;
import com.freelycar.saas.basic.wrapper.PaginationRJO;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.project.repository.StaffRepository;
import com.freelycar.saas.project.service.StaffService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-09-25
 * Time: 14:32
 */
@Api(value = "服务商技师", description = "服务商技师接口", tags = "服务商技师接口")
@RestController
@RequestMapping("/sp/staff")
public class RSPStaffController {
    private StaffService staffService;

    @Autowired
    public void setStaffService(StaffService staffService) {
        this.staffService = staffService;
    }

    /**
     * 获取服务商列表（分页）
     *
     * @param storeName
     * @param currentPage
     * @param pageSize
     * @return
     */
    @ApiOperation(value = "获取服务商技师列表（分页）", produces = "application/json")
    @GetMapping("/list")
    @LoggerManage(description = "调用方法：获取服务商技师列表（分页）")
    public ResultJsonObject list(
            @RequestParam(required = false) String storeName,
            @RequestParam(required = false) String staffName,
            @RequestParam Integer currentPage,
            @RequestParam(required = false) Integer pageSize) {
        if (StringUtils.isEmpty(storeName)) {
            storeName = "";
            //TODO 服务商技师
        }
        return ResultJsonObject.getDefaultResult(staffService.list(storeName, staffName, currentPage, pageSize));
    }
}
