package com.freelycar.saas.project.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.aop.LoggerManage;
import com.freelycar.saas.basic.wrapper.PaginationRJO;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.exception.NormalException;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.project.entity.ConsumerOrder;
import com.freelycar.saas.project.model.*;
import com.freelycar.saas.project.service.ConsumerOrderService;
import com.freelycar.saas.project.service.CouponService;
import com.freelycar.saas.util.ExcelTool;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author tangwei - Toby
 * @date 2018-12-28
 * @email toby911115@gmail.com
 */
@Api(value = "门店单据管理", description = "门店单据管理接口", tags = "门店单据管理接口")
@RestController
@RequestMapping("/order")
public class ConsumerOrderController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ConsumerOrderService consumerOrderService;

    @Autowired
    private CouponService couponService;

    /**
     * 开单
     *
     * @param orderObject
     * @return
     */
    @ApiOperation(value = "开单", produces = "application/json")
    @PostMapping("/handleOrder")
    @LoggerManage(description = "调用方法：开单")
    public ResultJsonObject handleOrder(@RequestBody OrderObject orderObject) {
        return consumerOrderService.handleOrder(orderObject);
    }

    /**
     * 获取单据详情
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取单据详情", produces = "application/json")
    @GetMapping("/detail")
    @LoggerManage(description = "调用方法：获取单据详情")
    public ResultJsonObject detail(@RequestParam String id) {
        return consumerOrderService.getOrderObjectDetail(id);
    }

    /**
     * 结算
     *
     * @param payOrder
     * @return
     */
    @ApiOperation(value = "单据结算（支付）", produces = "application/json")
    @PostMapping("/payment")
    @LoggerManage(description = "调用方法：单据结算（支付）")
    public ResultJsonObject payment(@RequestBody PayOrder payOrder) {
        try {
            return consumerOrderService.payment(payOrder);
        } catch (ObjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }

    /**
     * 挂单
     *
     * @param payOrder
     * @return
     */
    @ApiOperation(value = "单据挂单", produces = "application/json")
    @PostMapping("/pendingOrder")
    @LoggerManage(description = "调用方法：单据挂单")
    public ResultJsonObject pendingOrder(@RequestBody PayOrder payOrder) {
        try {
            return consumerOrderService.pendingOrder(payOrder);
        } catch (ObjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }

    @ApiOperation(value = "单据列表（分页）", produces = "application/json")
    @PostMapping("/list")
    @LoggerManage(description = "调用方法：单据列表（分页）")
    public ResultJsonObject list(
            @RequestParam String storeId,
            @RequestParam Integer currentPage,
            @RequestParam(required = false) Integer pageSize,
            @RequestBody OrderListParam params
    ) {
        return consumerOrderService.listSql(storeId, currentPage, pageSize, params, false);
    }

    /**
     * 完工
     *
     * @param consumerOrder
     * @return
     */
    @ApiOperation(value = "设置单据完工状态", produces = "application/json")
    @PostMapping("/serviceFinish")
    @LoggerManage(description = "调用方法：设置单据完工状态")
    public ResultJsonObject serviceFinish(@RequestBody ConsumerOrder consumerOrder) {
        try {
            return consumerOrderService.serviceFinish(consumerOrder);
        } catch (ObjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }

    /**
     * 交车
     *
     * @param consumerOrder
     * @return
     */
    @ApiOperation(value = "设置单据交车状态", produces = "application/json")
    @PostMapping("/handOver")
    @LoggerManage(description = "调用方法：设置单据交车状态")
    public ResultJsonObject handOver(@RequestBody ConsumerOrder consumerOrder) {
        try {
            return consumerOrderService.handOver(consumerOrder);
        } catch (ObjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }


    @ApiOperation(value = "根据车牌查出相关信息", produces = "application/json")
    @GetMapping("/loadClientInfoByLicensePlate")
    @LoggerManage(description = "调用方法：根据车牌查出相关信息")
    public ResultJsonObject loadClientInfoByLicensePlate(@RequestParam String licensePlate, @RequestParam String storeId) {
        return consumerOrderService.loadClientInfoByLicensePlate(licensePlate, storeId);
    }

    @ApiOperation(value = "查询某订单下，所有项目对应的抵用券")
    @GetMapping("/getCouponsForOrder")
    @LoggerManage(description = "调用方法：查询某订单下，所有项目对应的抵用券")
    public ResultJsonObject getCouponsForOrder(@RequestParam String orderId) {
        try {
            return ResultJsonObject.getDefaultResult(couponService.findAllUsefulCouponForOneOrder(orderId));
        } catch (ObjectNotFoundException | ArgumentMissingException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }

    @ApiOperation(value = "查询某用户的消费记录")
    @GetMapping("/orderRecord")
    @LoggerManage(description = "调用方法：查询某用户的消费记录")
    public ResultJsonObject orderRecord(
            @RequestParam String clientId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam Integer currentPage,
            @RequestParam(required = false) Integer pageSize
    ) {
        Map<String, Object> params = new HashMap<>();
        if (StringUtils.hasText(clientId)) {
            params.put("clientId", clientId);
        }
        if (StringUtils.hasText(startTime)) {
            params.put("startTime", startTime);
        }
        if (StringUtils.hasText(endTime)) {
            params.put("endTime", endTime);
        }
        try {
            return ResultJsonObject.getDefaultResult(consumerOrderService.orderRecord(params, currentPage, pageSize));
        } catch (ArgumentMissingException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }


    @ApiOperation(value = "导出单据列表", produces = "application/json")
    @PostMapping("/exportOrder")
    @LoggerManage(description = "调用方法：导出单据列表")
    public void exportOrder(
            @RequestParam String storeId,
            @RequestParam Integer currentPage,
            @RequestParam(required = false) Integer pageSize,
            @RequestBody OrderListParam params,
            HttpServletResponse response
    ) {
        ResultJsonObject resultJsonObject = consumerOrderService.listSql(storeId, currentPage, pageSize, params, true);
        //模拟从数据库获取需要导出的数据
        @SuppressWarnings({"unused", "unchecked"})
        List<CustomerOrderListObject> list = (List<CustomerOrderListObject>) resultJsonObject.getData();


        //导出操作
        try {
            ExcelTool.exportExcel(list, "单据列表", "单据列表", CustomerOrderListObject.class, "小易车单据列表.xls", response);
        } catch (NormalException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @ApiOperation(value = "查询流水明细（分页）", produces = "application/json")
    @LoggerManage(description = "调用方法：查询流水明细（分页）")
    @GetMapping("/listOrderParticulars")
    public ResultJsonObject listOrderParticulars(
            @RequestParam String storeId,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam Integer currentPage,
            @RequestParam(required = false) Integer pageSize
    ) {
        if (null == pageSize) {
            pageSize = 10;
        }

        PaginationRJO pageResult = consumerOrderService.listPageOrderParticulars(storeId, startTime, endTime, currentPage, pageSize);

        double totalAccount = consumerOrderService.sumOrderParticularsTotalAccount(storeId, startTime, endTime).doubleValue();


        JSONObject jsonResult = new JSONObject();
        jsonResult.put("pageResult", pageResult);
        jsonResult.put("totalAccount", totalAccount);

        return ResultJsonObject.getDefaultResult(jsonResult);
    }

    @ApiOperation(value = "导出流水明细Excel", produces = "application/json")
    @LoggerManage(description = "调用方法：导出流水明细Excel")
    @GetMapping("/exportOrderParticularsExcel")
    public void exportOrderParticularsExcel(
            @RequestParam String storeId,
            @RequestParam String startTime,
            @RequestParam String endTime,
            HttpServletResponse response
    ) {
        List<OrderParticulars> list = consumerOrderService.exportOrderParticulars(storeId, startTime, endTime);

        //导出操作
        try {
            ExcelTool.exportExcel(list, "流水明细", "流水明细", OrderParticulars.class, "小易车流水明细.xls", response);
        } catch (NormalException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }


    @ApiOperation(value = "获取某门店的收入统计", produces = "application/json")
    @LoggerManage(description = "调用方法：获取某门店的收入统计")
    @GetMapping("/getStoreIncome")
    public ResultJsonObject getStoreIncome(
            @RequestParam String storeId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        try {
            if (StringUtils.hasText(startTime)) {
                startTime += " 00:00:00";
            } else {
                startTime = "2019-01-01 00:00:00";
            }

            if (StringUtils.hasText(endTime)) {
                endTime += " 23:59:59";
            } else {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                endTime = dateFormat.format(new Date());
            }


            JSONObject resultJSON = new JSONObject();
            JSONObject memberIncomeJSON = consumerOrderService.getStoreIncome(storeId, startTime, endTime);
            JSONObject payMethodIncomeJSON = consumerOrderService.getAllPayMethodIncomeForOneStore(storeId, startTime, endTime);
            List pieChartJSON = consumerOrderService.getProjectPieChart(storeId, startTime, endTime);

            resultJSON.put("memberIncome", memberIncomeJSON);
            resultJSON.put("payMethodIncome", payMethodIncomeJSON);
            resultJSON.put("pieChart", pieChartJSON);

            return ResultJsonObject.getDefaultResult(resultJSON);
        } catch (ArgumentMissingException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }

    @ApiOperation(value = "获取营业汇总-时间", produces = "application/json")
    @LoggerManage(description = "调用方法：获取营业汇总-时间")
    @GetMapping("/getIncomeByYear")
    public ResultJsonObject getIncomeByYear(@RequestParam String year) {
        List year1 = consumerOrderService.getMongthlyIncomeByYear(year);
        //计算当年总额
        double sum = 0;
        for (int i = 0; i < year1.size(); i++) {
            Object[] objs = (Object[]) year1.get(i);
            sum += (double) objs[1];
        }
        List year2 = consumerOrderService.getMongthlyIncomeByYear((Integer.valueOf(year) - 1) + "");
        //计算环比
        JSONArray m2m = new JSONArray();
        for (int i = 0; i < year1.size(); i++) {
            Object[] objs1 = (Object[]) year1.get(i);
            String month1 = (String) objs1[0];
            double value1 = (double) objs1[1];
            double value2 = 0;
            if (i < year1.size() - 1) {
                Object[] objs2 = (Object[]) year1.get(i + 1);
                String month2 = (String) objs2[0];
                if (Integer.valueOf(month2.split("-")[1]) == (Integer.valueOf(month1.split("-")[1]) - 1)) {
                    value2 = (double) objs2[1];
                }
            } else {
                if (year2.size()>0){
                    Object[] objs2 = (Object[]) year2.get(0);
                    String month2 = (String) objs2[0];
                    if (Integer.valueOf(month2.split("-")[1]) == 12) {
                        value2 = (double) objs2[1];
                    }
                }
            }
            double value = 0;
            if (value2 > 0) {
                value = (double) Math.round((value1 - value2) / value2 * 100) / 100;
            }
            Object[] res = {month1, value};
            m2m.add(res);
        }
        //计算同比
        JSONArray y2y = new JSONArray();
        for (int i = 0; i < year1.size(); i++) {
            Object[] objs1 = (Object[]) year1.get(i);
            String month1 = (String) objs1[0];
            double value1 = (double) objs1[1];
            double value2 = 0;
            String month = month1.split("-")[1];
            for (int j = 0; j < year2.size(); j++) {
                Object[] objs2 = (Object[]) year2.get(j);
                String month2 = (String) objs2[0];
                if (month2.split("-")[1].equals(month)) {
                    value2 = (double) objs2[1];
                    break;
                }
            }
            double value = 0;
            if (value2 > 0) {
                value = (double) Math.round((value1 - value2) / value2 * 100) / 100;
            }
            Object[] res = {month1, value};
            y2y.add(res);
        }
        JSONObject res = new JSONObject();
        res.put("sum", sum);
        res.put("year", year1);
        res.put("M2M", m2m);
        res.put("Y2Y", y2y);
        return ResultJsonObject.getDefaultResult(res);
    }
}
