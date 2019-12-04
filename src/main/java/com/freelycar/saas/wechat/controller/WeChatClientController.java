package com.freelycar.saas.wechat.controller;

import com.freelycar.saas.aop.LoggerManage;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.project.controller.CarController;
import com.freelycar.saas.project.entity.Car;
import com.freelycar.saas.project.service.CarService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * @author tangwei - Toby
 * @date 2019-01-23
 * @email toby911115@gmail.com
 */
@RestController
@RequestMapping("/wechat/client")
public class WeChatClientController {
    private Logger logger = LoggerFactory.getLogger(CarController.class);

    @Autowired
    private CarService carService;
    private String errorMsg;

    /**
     * 删除操作
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/deleteCar")
    @LoggerManage(description = "调用方法：微信端-删除车辆")
    public ResultJsonObject deleteCar(@RequestParam String id) {
        return carService.delete(id);
    }

    /**
     * 新增/修改车辆
     *
     * @param car
     * @return
     */

    @PostMapping(value = "/addCar")
    @LoggerManage(description = "调用方法：微信端-新增/修改车辆信息")
    public ResultJsonObject addCar(@RequestBody Car car) {
        if (null == car) {
            errorMsg = "接收到的JSON：car对象为NULL";
            logger.error(errorMsg);
            return ResultJsonObject.getErrorResult(null, errorMsg);
        }

        //去除车牌号中的空白字符
        String sourceLicensePlate = car.getLicensePlate();
        if (StringUtils.hasLength(sourceLicensePlate)) {
            String targetLicensePlate = sourceLicensePlate.replaceAll("\\s*", "");
            car.setLicensePlate(targetLicensePlate);
        }
        //执行保存操作
        return carService.modify(car);
    }


    @GetMapping(value = "/listPersonalCars")
    @LoggerManage(description = "调用方法：微信端-加载用户所有车辆")
    public ResultJsonObject listPersonalCars(@RequestParam String clientId) {
        return carService.listPersonalCars(clientId);
    }



}
