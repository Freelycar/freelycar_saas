package com.freelycar.screen.timer;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.screen.service.*;
import com.freelycar.screen.websocket.server.ScreenWebsocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author pyt
 * @date 2020/3/31 15:44
 * @email 2630451673@qq.com
 * @desc
 */
@Component
@EnableScheduling
public class TimeTask {
    private final static Logger logger = LoggerFactory.getLogger(TimeTask.class);
    @Autowired
    private ArkService arkService;
    @Autowired
    private ConsumerOrderService consumerOrderService;
    @Autowired
    private ConsumerProjectService consumerProjectService;
    @Autowired
    private WxUserInfoService wxUserInfoService;
    @Autowired
    private CarService carService;


    @Scheduled(cron = "0 0/1 * * * ?")   //每分钟执行一次
    public void test() {
        logger.info("**************开始定时推送**************");
        CopyOnWriteArraySet<ScreenWebsocketServer> webSocketSet =
                ScreenWebsocketServer.getWebSocketSet();
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
         * 1.用户中汽车品牌top10
         */
        List<Map<String, Integer>> result5 = carService.getCarBrand();
        result.put("topCarBrand",result5);
        webSocketSet.forEach(c -> {
            try {
                c.sendMessage(result.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        logger.info("**************定时推送结束**************");
    }
}
