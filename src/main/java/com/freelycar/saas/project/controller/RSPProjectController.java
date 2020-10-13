package com.freelycar.saas.project.controller;

import com.freelycar.saas.aop.LoggerManage;
import com.freelycar.saas.basic.wrapper.PaginationRJO;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.exception.BatchDeleteException;
import com.freelycar.saas.exception.DataIsExistException;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.project.entity.RSPProject;
import com.freelycar.saas.project.service.RSPProjectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-09-10
 * Time: 16:58
 */
@Api(value = "服务商项目", description = "服务商项目接口", tags = "服务商项目接口")
@RestController
@RequestMapping("/sp/project")
public class RSPProjectController {
    private RSPProjectService rspProjectService;

    @Autowired
    public void setRspProjectService(RSPProjectService rspProjectService) {
        this.rspProjectService = rspProjectService;
    }

    @ApiOperation(value = "服务商项目新增/修改", produces = "application/json")
    @PostMapping(value = "/modify")
    @LoggerManage(description = "调用方法：服务商项目新增/修改")
    public ResultJsonObject add(@RequestBody RSPProject project) {
        try {
            return ResultJsonObject.getDefaultResult(rspProjectService.add(project));
        } catch (DataIsExistException | ArgumentMissingException | ObjectNotFoundException e) {
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }

    @ApiOperation(value = "删除服务商项目信息", produces = "application/json")
    @PostMapping(value = "/delete")
    @LoggerManage(description = "调用方法：删除服务商信息")
    public ResultJsonObject delete(@RequestBody String[] ids) {
        try {
            return rspProjectService.delete(ids);
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
    @ApiOperation(value = "获取服务商项目列表（分页）", produces = "application/json")
    @GetMapping("/list")
    @LoggerManage(description = "调用方法：获取服务商列表（分页）")
    public ResultJsonObject list(
            @RequestParam(required = false) String name,
            @RequestParam String rspId,
            @RequestParam Integer currentPage,
            @RequestParam(required = false) Integer pageSize) {
        if (StringUtils.isEmpty(name)) {
            name = "";
        }
        return ResultJsonObject.getDefaultResult(PaginationRJO.of(rspProjectService.list(name,currentPage, pageSize,rspId)));
    }
}
