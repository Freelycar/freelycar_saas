package com.freelycar.saas.screen.service;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.project.entity.Ark;
import com.freelycar.saas.project.repository.ArkRepository;
import com.freelycar.saas.screen.utils.TimestampUtil;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;

/**
 * @author pyt
 * @date 2020/4/1 11:03
 * @email 2630451673@qq.com
 * @desc
 */
@Service("arkService1")
public class ArkService {
    @Autowired
    private ArkRepository arkRepository;

    /**
     * 1.最近12月智能柜每月新增数量
     * 2.最近12月覆盖用户数
     *
     * @return list
     */
    public JSONObject getMonthlyAdditions() {
        JSONObject result = new JSONObject();
        DateTime now = new DateTime();
        Timestamp start = new Timestamp(TimestampUtil.getStartTime(now).getMillis());
        List<Ark> arkList = arkRepository.findByDelStatusAndCreateTimeAfter(false, start);
        //每月新增智能柜数
        List<Map<String, Integer>> r1 = MonthService.getMonthlyAdditions(arkList, now);
        List<Map<String, Long>> r2 = new ArrayList<>();
        int initSum = getInitNumberOfArk();
        long sum = genRandomSum(initSum);
        for (Map<String, Integer> o : r1) {
            for (String key :
                    o.keySet()) {
                //累计覆盖用户数
                int s = o.get(key);
                sum += genRandomSum(s);
                Map<String, Long> map = new HashMap<>();
                map.put(key, sum);
                r2.add(map);
                //累计智能柜数
                initSum += s;
                o.put(key, initSum);
            }
        }
        result.put("ark", updateDate1(r1));
        result.put("user", updateDate2(r2));
        return result;
    }

    private int getInitNumberOfArk() {
        DateTime now = new DateTime();
        Timestamp start = new Timestamp(TimestampUtil.getStartTime(now).getMillis());
        List<Ark> arkList = arkRepository.findByDelStatusAndCreateTimeBefore(false, start);
        return arkList.size();
    }

    /**
     * 随机数（500-800）数组
     * 长度为len的和
     *
     * @param len
     * @return
     */
    private long genRandomSum(int len) {
        long result = 0l;
        int count = 0;
        Random r = new Random();
        while (count < len) {
            result += r.nextInt(301) + 500;
            count++;
        }
        return result;
    }

    private List<Map<String, Integer>> updateDate1(List<Map<String, Integer>> list) {
        List<Map<String, Integer>> result = new ArrayList<>();
        for (Map<String, Integer> map :
                list) {
            for (String key :
                    map.keySet()) {
                Map<String, Integer> map1 = new HashMap<>();
                map1.put(key.substring(5), map.get(key));
                result.add(map1);
                break;
            }
        }
        return result;
    }

    private List<Map<String, Long>> updateDate2(List<Map<String, Long>> list) {
        List<Map<String, Long>> result = new ArrayList<>();
        for (Map<String, Long> map :
                list) {
            for (String key :
                    map.keySet()) {
                Map<String, Long> map1 = new HashMap<>();
                map1.put(key.substring(5), map.get(key));
                result.add(map1);
                break;
            }
        }
        return result;
    }
}
