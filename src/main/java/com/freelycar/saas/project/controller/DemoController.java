package com.freelycar.saas.project.controller;

import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.exception.OpenArkDoorFailedException;
import com.freelycar.saas.exception.OpenArkDoorTimeOutException;
import com.freelycar.saas.project.service.DemoService;
import com.freelycar.saas.project.service.DemoUserService;
import com.freelycar.saas.project.service.EdaijiaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class DemoController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private DemoService demoService;

    @Autowired
    private DemoUserService demoCommonService;

    @Autowired
    private EdaijiaService edaijiaService;

    @GetMapping("/superadmin/index")
    public Map<String,Object> index() {
        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("status","success");
        resultMap.put("content",demoService.getString());
        return resultMap;
    }

    @GetMapping(value = "/mobile/{mobile:.+}")
    public Object getSingleLoanItem(@PathVariable("mobile") String mobile) {
        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("status","success");
        resultMap.put("content",demoCommonService.getUserByMobile(mobile));
        return resultMap;
    }

    @GetMapping("/verifyCode")
    public ResultJsonObject verifyCode(String sign,Integer password,Integer orderId){
        logger.info("arkOrderLog:代驾取车接口----------");
        String errorMessage;
        try {
            return edaijiaService.verifyCode(sign,password,orderId);
        } catch (ObjectNotFoundException | InterruptedException e) {
            errorMessage = e.getMessage();
            e.printStackTrace();
            logger.error(errorMessage, e);
        } catch (OpenArkDoorFailedException e) {
            errorMessage = "智能柜开门失败";
            e.printStackTrace();
            logger.error(errorMessage, e);
        } catch (ArgumentMissingException e) {
            errorMessage = "智能柜开门故障";
            e.printStackTrace();
            logger.error(errorMessage, e);
        } catch (OpenArkDoorTimeOutException e) {
            errorMessage = "智能柜开门超时";
            e.printStackTrace();
            logger.error(errorMessage, e);
        }
        return ResultJsonObject.getErrorResult(null, errorMessage + "，请稍后重试或联系门店。");
    }
}