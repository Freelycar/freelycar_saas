package com.freelycar.saas.project.controller;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.aop.LoggerManage;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.project.entity.Agent;
import com.freelycar.saas.project.service.AgentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Api(value = "代理商", tags = "代理商")
@RestController
@RequestMapping("/agent")
public class AgentController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AgentService agentService;

    @ApiOperation(value = "新增/修改代理商", produces = "application/json")
    @PostMapping(value = "/modify")
    @LoggerManage(description = "调用方法：新增/修改代理商")
    public ResultJsonObject saveOrUpdate(@RequestBody Agent agent) {
        try {
            return ResultJsonObject.getDefaultResult(agentService.modify(agent));
        } catch (ObjectNotFoundException | ArgumentMissingException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }

    @ApiOperation(value = "获取代理商详情", produces = "application/json")
    @GetMapping(value = "/detail")
    @LoggerManage(description = "调用方法：获取代理商详情")
    public ResultJsonObject detail(@RequestParam String id) {
        try {
            return ResultJsonObject.getDefaultResult(agentService.findById(id));
        } catch (ObjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }

    @ApiOperation(value = "获取供应商列表（分页）", produces = "application/json")
    @GetMapping(value = "/list")
    @LoggerManage(description = "调用方法：获取供应商列表")
    public ResultJsonObject list(
            @RequestParam Integer currentPage,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String agentName,
            @RequestParam(required = false) String linkMan
    ) {
        if (StringUtils.isEmpty(StringUtils.trimWhitespace(agentName))) {
            agentName = "";
        }
        if (StringUtils.isEmpty(StringUtils.trimWhitespace(linkMan))) {
            linkMan = "";
        }
        return ResultJsonObject.getDefaultResult(agentService.list(currentPage, pageSize, agentName, linkMan));
    }

    /**
     * 删除操作（软删除）
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "删除供应商信息", produces = "application/json")
    @GetMapping(value = "/delete")
    @LoggerManage(description = "调用方法：删除供应商信息")
    public ResultJsonObject delete(@RequestParam String id) {
        return agentService.delete(id);
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    @ApiOperation(value = "批量删除供应商信息", produces = "application/json")
    @PostMapping("/batchDelete")
    @LoggerManage(description = "调用方法：批量删除供应商信息")
    public ResultJsonObject batchDelete(@RequestBody JSONObject ids) {
        if (null == ids) {
            return ResultJsonObject.getErrorResult(null, "ids参数为NULL");
        }
        return agentService.delByIds(ids.getString("ids"));
    }
}
