package com.freelycar.saas.screen.service;

import com.freelycar.saas.project.entity.Ark;
import com.freelycar.saas.project.entity.ConsumerOrder;
import com.freelycar.saas.project.entity.WxUserInfo;
import com.freelycar.saas.screen.utils.TimestampUtil;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author pyt
 * @date 2020/4/1 15:42
 * @email 2630451673@qq.com
 * @desc
 */
@Service
public class MonthService {
    public static List<Map<String, Integer>> getMonthlyAdditions(List list, DateTime now) {
        List<Map<String, Integer>> result = new ArrayList<>();
        List<DateTime> timeList = TimestampUtil.getTimeList(now);
        int[] sum_ark = new int[12];
        for (Object o :
                list) {
            Timestamp createTime = null;
            if (o instanceof Ark) {
                createTime = ((Ark) o).getCreateTime();
            } else if (o instanceof WxUserInfo) {
                createTime = ((WxUserInfo) o).getCreateTime();
            } else if (o instanceof ConsumerOrder) {
                createTime = ((ConsumerOrder) o).getCreateTime();
            }
            for (int i = 0; i < timeList.size(); i++) {
                if (createTime.getTime() > timeList.get(i).getMillis()) continue;
                else if (createTime.getTime() == timeList.get(i).getMillis()) sum_ark[i]++;
                else sum_ark[i - 1]++;
                break;
            }
        }
        for (int i = 0; i < 12; i++) {
            Map<String, Integer> r = new HashMap<>();
            r.put(timeList.get(i).toString(TimestampUtil.dateTimeFormatter1), sum_ark[i]);
            result.add(r);
        }
        return result;
    }
}
