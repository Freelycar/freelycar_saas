package com.freelycar.saas.project.controller;

import com.freelycar.saas.aop.LoggerManage;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.project.entity.Staff;
import com.freelycar.saas.project.service.StaffService;
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
     * 获取服务商技师列表（分页）
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
            @RequestParam String rspId,
            @RequestParam(required = false) String storeName,
            @RequestParam(required = false) String staffName,
            @RequestParam Integer currentPage,
            @RequestParam(required = false) Integer pageSize) {
        if (StringUtils.isEmpty(staffName)) {
            staffName = "";
        }
        return ResultJsonObject.getDefaultResult(staffService.list(rspId, storeName, staffName, currentPage, pageSize));
    }

    /**
     * 新增/修改服务商技师
     *
     * @param staff
     * @return
     */
    @ApiOperation(value = "新增/修改服务商技师", produces = "application/json")
    @PostMapping("/modify")
    @LoggerManage(description = "调用方法：新增/修改服务商技师")
    public ResultJsonObject modify(@RequestBody Staff staff) {
        return staffService.modifyRspStaff(staff);
    }

    /**
     * 删除操作（软删除）
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "单个删除员工信息", produces = "application/json")
    @GetMapping(value = "/delete")
    @LoggerManage(description = "调用方法：单个删除员工信息")
    public ResultJsonObject delete(@RequestParam String id) {
        return staffService.delete(id);
    }

    /**
     * 智能柜技师开通
     *
     * @param id
     * @return
     **/
    @ApiOperation(value = "智能柜技师开通", produces = "application/json")
    @GetMapping(value = "/openArk")
    @LoggerManage(description = "调用方法：智能柜技师开通")
    public ResultJsonObject openArk(
            @RequestParam String id
    ) {
        return staffService.openArk(id);
    }


    /**
     * 智能柜技师关闭
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "智能柜技师关闭", produces = "application/json")
    @GetMapping(value = "/closeArk")
    @LoggerManage(description = "调用方法：智能柜技师关闭")
    public ResultJsonObject closeArk(@RequestParam String id) {
        return staffService.closeArk(id);
    }

}
