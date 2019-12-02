package com.freelycar.saas.project.controller;

import com.freelycar.saas.aop.LoggerManage;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.exception.DataIsExistException;
import com.freelycar.saas.project.entity.Employee;
import com.freelycar.saas.project.service.EmployeeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;

/**
 * 雇员的概念：
 * 雇员employee与员工staff是一对多关系
 * 一个雇员对于代理商来说是一个人，但对于多个门店来说，一个雇员可能兼职在这些门店中，故每个门店中会有一个该雇员的员工数据
 */
@Api(value = "雇员", tags = "雇员")
@RestController
@RequestMapping("/employee")
public class EmployeeController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EmployeeService employeeService;

    @ApiOperation(value = "新增/修改雇员信息", produces = "application/json")
    @PostMapping(value = "/modify")
    @LoggerManage(description = "调用方法：新增/修改雇员信息")
    public ResultJsonObject saveOrUpdate(@RequestBody Employee employee) {
        try {
            return ResultJsonObject.getDefaultResult(employeeService.modify(employee));
        } catch (EntityNotFoundException | ArgumentMissingException | DataIsExistException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }

    @ApiOperation(value = "获取雇员详情", produces = "application/json")
    @GetMapping(value = "/detail")
    @LoggerManage(description = "调用方法：获取雇员详情")
    public ResultJsonObject detail(@RequestParam String id) {
        try {
            return ResultJsonObject.getDefaultResult(employeeService.loadEmployeeDetail(id));
        } catch (EntityNotFoundException | ArgumentMissingException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }


}
