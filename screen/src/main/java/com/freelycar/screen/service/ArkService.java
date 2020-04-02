package com.freelycar.screen.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.freelycar.screen.entity.Ark;
import com.freelycar.screen.entity.repo.ArkRepository;
import com.freelycar.screen.utils.TimestampUtil;
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
@Service
public class ArkService {
    @Autowired
    private ArkRepository arkRepository;

    /**
     * 1.最近12月智能柜每月新增数量
     * 2.最近12月覆盖用户数
     * @return list
     */
    public JSONObject getMonthlyAdditions() {
        JSONObject result = new JSONObject();
        DateTime now = new DateTime();
        Timestamp start = new Timestamp(TimestampUtil.getStartTime(now).getMillis());
        List<Ark> arkList = arkRepository.findByDelStatusAndCreateTimeAfter(false,start);
        List<Map<String, Integer>> r1 = MonthService.getMonthlyAdditions(arkList, now);
        result.put("ark", r1);
        List<Map<String, Long>> r2 = new ArrayList<>();
        long sum = getInitNumberOfCoveredPeople();
        for (Map<String, Integer> o : r1) {
            for (String key :
                    o.keySet()) {
                int s = o.get(key);
                sum += genRandomSum(s);
                Map<String, Long> map = new HashMap<>();
                map.put(key, sum);
                r2.add(map);
            }
        }
        result.put("user", r2);
        return result;
    }

    private Long getInitNumberOfCoveredPeople() {
        DateTime now = new DateTime();
        Timestamp start = new Timestamp(TimestampUtil.getStartTime(now).getMillis());
        List<Ark> arkList = arkRepository.findByDelStatusAndCreateTimeBefore(false,start);
        return genRandomSum(arkList.size());
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
}
