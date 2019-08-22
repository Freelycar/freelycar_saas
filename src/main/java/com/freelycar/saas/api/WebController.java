package com.freelycar.saas.api;

import com.freelycar.saas.aop.LoggerManage;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.project.entity.Partner;
import com.freelycar.saas.project.service.PartnerService;
import com.freelycar.saas.util.PartnerNumberGenerator;
import com.freelycar.saas.util.TimestampUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author tangwei - Toby
 * @date 2019-08-07
 * @email toby911115@gmail.com
 */
@Api(value = "小易爱车宣传网站接口", tags = "小易爱车宣传网站接口")
@RestController
@RequestMapping("/webapi")
public class WebController {
    private Logger logger = LoggerFactory.getLogger(WebController.class);

    @Autowired
    private PartnerService partnerService;

    @ApiOperation(value = "保存加盟商信息", produces = "application/json")
    @PostMapping("/saveInfo")
    @LoggerManage(description = "调用方法：保存加盟商信息")
    public ResultJsonObject savePartnerInfo(@RequestBody Partner partner) {
        partner.setCreateTime(TimestampUtil.getCurrentTimestamp());
        return ResultJsonObject.getDefaultResult(partnerService.save(partner));
    }

    @ApiOperation(value = "获取已登记人数", produces = "application/json")
    @GetMapping("/getCount")
    @LoggerManage(description = "调用方法：获取已登记人数")
    public ResultJsonObject getCount() {
        return ResultJsonObject.getDefaultResult(partnerService.countAll() + PartnerNumberGenerator.partnerCount);
    }
}
