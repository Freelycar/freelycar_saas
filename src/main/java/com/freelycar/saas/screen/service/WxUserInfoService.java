package com.freelycar.saas.screen.service;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.project.entity.WxUserInfo;
import com.freelycar.saas.project.repository.ConsumerOrderRepository;
import com.freelycar.saas.project.repository.WxUserInfoRepository;
import com.freelycar.saas.screen.utils.TimestampUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;

/**
 * @author pyt
 * @date 2020/4/1 15:38
 * @email 2630451673@qq.com
 * @desc
 */
@Service("wxUserInfoService1")
public class WxUserInfoService {
    private final static Logger logger = LoggerFactory.getLogger(WxUserInfoService.class);
    @Autowired
    private WxUserInfoRepository wxUserInfoRepository;
    @Autowired
    private ConsumerOrderRepository consumerOrderRepository;

    /**
     * 1.最近12月每月新增注册用户数,总注册数
     * 2.新增用户转化率
     * 3.总用户转化率
     *
     * @return List
     */
    public JSONObject getMonthlyAdditions() {
        JSONObject result = new JSONObject();
        DateTime now = DateTime.now();
        Timestamp start = new Timestamp(TimestampUtil.getStartTime(now).getMillis());
        List<WxUserInfo> wxUserInfoList1 = wxUserInfoRepository.findByDelStatusAndCreateTimeAfter(false, start);
        List<WxUserInfo> wxUserInfoList2 = wxUserInfoRepository.findByDelStatusAndCreateTimeBefore(false, start);
        //每月新增用户数据
        List<Map<String, Integer>> resultPre = MonthService.getMonthlyAdditions(wxUserInfoList1, now);
        //每月：新增用户数量，总数量，日期
        List<Map<String, Object>> result1 = new ArrayList<>();
        Set<String> allClientIds = consumerOrderRepository.findClientIdByDelStatus(false);
        Long startCount = (long) wxUserInfoList2.size();
        Long startEffectiveCount = 0l;
        for (WxUserInfo user :
                wxUserInfoList2) {
            if (user.getDefaultClientId() != null) {
                String clientId = user.getDefaultClientId();
                if (allClientIds.contains(clientId)) startEffectiveCount++;
            }
        }
        for (Map<String, Integer> map :
                resultPre) {
            for (String key :
                    map.keySet()) {
                Map<String, Object> map1 = new HashMap<>();
                //当月日期
                map1.put("date", key);
                startCount += map.get(key);
                //当月新增用户
                map1.put("addition", map.get(key));
                //截止当月底新增总用户
                map1.put("sum", startCount);
                result1.add(map1);
            }
        }
        result.put("newUser", result1);
        //每月新增有效数据
        Map<String, Integer> result2Pre = getActiveClientMonthly(wxUserInfoList1, allClientIds);
        Set<String> result2Month = result2Pre.keySet();
//        logger.info("{}", result2Month);
        for (Map<String, Object> map :
                result1) {
            String month = (String) map.get("date");
            Integer addition = (int) map.get("addition");
            long sum = (long) map.get("sum");
            float invertionRate = 0;
            if (result2Month.contains(month)) {
                int activeCount = result2Pre.get(month);
                //每月新增用户中的有效用户数量
                map.put("activeCount", activeCount);
                startEffectiveCount += activeCount;
                invertionRate = (float) (Math.round(((float) activeCount / addition) * 100)) / 100;
            } else {
                map.put("activeCount", 0);
            }
            float totalConversion = (float) (Math.round(((float) startEffectiveCount / sum) * 100)) / 100;
            //当月新增用户的转化率
            map.put("invertionRate", invertionRate);
            //截止到当月的有效用户数量
            map.put("effectiveAddition", startEffectiveCount);
            //截止到当月的总用户转化率
            map.put("totalConversion", totalConversion);
        }
        for (Map<String, Object> map :
                result1) {
            map.put("date", ((String) map.get("date")).substring(5));
        }

        return result;
    }

    public Map<String, Integer> getActiveClientMonthly(List<WxUserInfo> wxUserInfoList, Set<String> allClientIds) {
        Set<String> months = new HashSet<>();
        Map<String, Set<String>> resultPre = new HashMap<>();
        Map<String, Integer> result = new HashMap<>();
        //获取每月新增用户的clientId
        for (WxUserInfo user : wxUserInfoList) {
            if (user.getDefaultClientId() != null) {
                String month = new DateTime(user.getCreateTime()).toString(TimestampUtil.dateTimeFormatter1);
                if (!months.contains(month)) {
                    Set<String> stringSet = new HashSet<>();
                    stringSet.add(user.getDefaultClientId());
                    resultPre.put(month, stringSet);
                    months.add(month);
                } else {
                    Set<String> stringSet = resultPre.get(month);
                    stringSet.add(user.getDefaultClientId());
                    resultPre.put(month, stringSet);
                }
            }
        }

        //判断每月新增用户的client的有效性
        for (String key :
                resultPre.keySet()) {
            Set<String> clientIds = resultPre.get(key);
            int count = 0;
            for (String s :
                    clientIds) {
                if (allClientIds.contains(s)) count++;
            }
            result.put(key, count);
        }
        return result;
    }
}
