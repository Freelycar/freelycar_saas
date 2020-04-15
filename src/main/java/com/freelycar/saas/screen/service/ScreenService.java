package com.freelycar.saas.screen.service;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.screen.timer.TimeTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author pyt
 * @date 2020/4/15 11:05
 * @email 2630451673@qq.com
 * @desc
 */
@Service
public class ScreenService {
    private final static Logger logger = LoggerFactory.getLogger(ScreenService.class);
    @Autowired
    @Qualifier("arkService1")
    private ArkService arkService;
    @Autowired
    @Qualifier("consumerOrderService1")
    private ConsumerOrderService consumerOrderService;
    @Autowired
    @Qualifier("consumerProjectService1")
    private ConsumerProjectService consumerProjectService;
    @Autowired
    @Qualifier("wxUserInfoService1")
    private WxUserInfoService wxUserInfoService;
    @Autowired
    @Qualifier("carService1")
    private CarService carService;

    public JSONObject result(){
        JSONObject result = new JSONObject();
        JSONObject result1 = arkService.getMonthlyAdditions();
        /*
         * 1.最近12月智能柜每月新增数量:ark
         * 2.最近12月覆盖用户数:user
         */
        result.put("ark", result1.get("ark"));
        result.put("user", result1.get("user"));
        JSONObject result2 = consumerOrderService.getMonthlyAdditions();
        /*
         * 1.最近12月每月智能柜使用用户数:activeUser
         * （同一用户当月多次使用仅作为一条数据）
         * 2.门店排名:storeRanking
         * 3.最近12月用户平均使用频次:averageUsage
         * 4.用户平均每月消费数额:averageConsumption
         */
        result.put("activeUser", result2.get("activeUser"));
        result.put("storeRanking", result2.get("storeRanking"));
        result.put("averageUsage", result2.get("averageUsage"));
        result.put("averageConsumption", result2.get("averageConsumption"));
        JSONObject result3 = consumerProjectService.genProjectRanking();
        /*
         * 服务项目排名:projectTypeSumList
         */
        result.put("projectTypeSumList", result3.get("projectTypeSumList"));
        JSONObject result4 = wxUserInfoService.getMonthlyAdditions();
        /*
        最近12月每月新增注册用户数,总注册数与用户转化率:newUser
         */
        result.put("newUser", result4.get("newUser"));
        /*
         * 1.用户中汽车品牌top10:topCarBrandByUser
         * 2.用户消费次数汽车品牌top10:topCarBrandByConsumptionTime
         * 3.平均消费金额汽车品牌top10:topCarBrandByAverageConsumption
         * 4.用户增长率汽车品牌top10:topCarBrandByUserIncrement
         * 5.用户复购率汽车品牌top10:topCarBrandByRepeatUse
         */
        JSONObject result5 = carService.getTop10CarBrand();
        result.put("topCarBrandByUser", result5.get("result1"));
        result.put("topCarBrandByConsumptionTime", result5.get("result2"));
        result.put("topCarBrandByAverageConsumption", result5.get("result3"));
        result.put("topCarBrandByUserIncrement", result5.get("currentUserIncrement"));
        result.put("topCarBrandByRepeatUse", result5.get("repeatUserList"));

        return result;
    }
}
