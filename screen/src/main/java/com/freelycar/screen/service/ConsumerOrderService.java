package com.freelycar.screen.service;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.screen.entity.ConsumerOrder;
import com.freelycar.screen.entity.Store;
import com.freelycar.screen.entity.repo.ConsumerOrderRepository;
import com.freelycar.screen.entity.repo.StoreRepository;
import com.freelycar.screen.utils.TimestampUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

/**
 * @author pyt
 * @date 2020/4/1 16:24
 * @email 2630451673@qq.com
 * @desc
 */
@Service
public class ConsumerOrderService {
    private final static Logger logger = LoggerFactory.getLogger(ConsumerOrderService.class);
    @Autowired
    private ConsumerOrderRepository consumerOrderRepository;
    @Autowired
    private StoreRepository storeRepository;

    /**
     * 1.最近12月每月智能柜使用用户数:activeUser
     * （同一用户当月多次使用仅作为一条数据）
     * 2.门店排名
     * 3.最近12月用户平均使用频次
     * 4.用户平均每月消费数额
     *
     * @return JSONObject
     */
    public JSONObject getMonthlyAdditions() {
        JSONObject result = new JSONObject();
        DateTime now = new DateTime();
        Timestamp start = new Timestamp(TimestampUtil.getStartTime(now).getMillis());
        List<ConsumerOrder> consumerOrderList = consumerOrderRepository.findByDelStatusAndCreateTimeAfter(false, start);
        List<Map<String, Integer>> r1 = MonthService.getMonthlyAdditions(dealWithList(consumerOrderList), now);
        result.put("activeUser", r1);
        List<Map<String, Object>> r2 = genStoreRanking(consumerOrderList);
        result.put("storeRanking", r2);
        List<Map<String, Object>> r3 = getAverageUsageFrequencyMonthly(consumerOrderList, now);
        result.put("averageUsage", r3);
        double averageConsumption = getAvarageConsumption(consumerOrderList);
        result.put("averageConsumption", averageConsumption);
        return result;
    }

    /**
     * 用户平均每月消费数额
     *
     * @return JSONObject
     */
    public double getAvarageConsumption(List<ConsumerOrder> consumerOrderList) {
        BigDecimal sum = new BigDecimal("0");
        Set<String> clientIds = new HashSet<>();
        for (ConsumerOrder order :
                consumerOrderList) {
            clientIds.add(order.getClientId());
            sum = sum.add(BigDecimal.valueOf(order.getActualPrice()));
        }
        logger.info("{}人共消费：{}", clientIds.size(), sum.toString());
        BigDecimal average = sum.divide(BigDecimal.valueOf(clientIds.size() * 12), 2, BigDecimal.ROUND_HALF_UP);
        double averageConsumption = average.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        return averageConsumption;
    }

    /**
     * 处理用户订单列表
     * 每月每个用户只保留一条数据
     *
     * @param consumerOrderList
     * @return
     */
    private List<ConsumerOrder> dealWithList(List<ConsumerOrder> consumerOrderList) {
        List<ConsumerOrder> result = new ArrayList<>();
        Map<Integer, Set<String>> record = new HashMap<>();
        for (ConsumerOrder order :
                consumerOrderList) {
            DateTime createTime = new DateTime(order.getCreateTime().getTime());
            Integer month = createTime.getMonthOfYear();
            String clientId = order.getClientId();
            if (record.keySet().contains(month)) {
                Set<String> clientIds = record.get(month);
                if (clientIds.contains(clientId)) {
                    continue;
                } else {
                    clientIds.add(clientId);
                    record.put(month, clientIds);
                    result.add(order);
                }
            } else {
                Set<String> clientIds = new HashSet<>();
                clientIds.add(clientId);
                record.put(month, clientIds);
                result.add(order);
            }
        }
        return result;
    }

    /**
     * 获取门店排名（根据业务量（订单数）显示业绩（收入））
     * 最近12月
     *
     * @param consumerOrderList
     * @return list
     */
    private List<Map<String, Object>> genStoreRanking(List<ConsumerOrder> consumerOrderList) {
        //1.计算每家门店的业务量和业绩
        Set<String> storeIds = new HashSet<>();
        List<Map<String, Object>> stores = new ArrayList<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (ConsumerOrder order :
                consumerOrderList) {
            String storeId = order.getStoreId();
            if (storeId != null && !storeId.equals("")) {
                if (!storeIds.contains(storeId)) {
                    storeIds.add(storeId);
                    Map<String, Object> store = new HashMap<>();
                    store.put("storeId", storeId);
                    store.put("count", 1);
                    store.put("sum", BigDecimal.valueOf(order.getTotalPrice()));
                    stores.add(store);
                } else {
                    boolean flag = false;
                    for (Map<String, Object> store :
                            stores) {
                        //找到门店数据
                        for (String key :
                                store.keySet()) {
                            if (!key.equals("storeId")) {
                                continue;
                            } else {
                                if (!store.get(key).equals(storeId)) {
                                    continue;
                                } else {
                                    flag = true;
                                }
                            }
                        }
                        if (flag) {//找到对应门店数据
                            int count = (int) store.get("count");
                            count++;
                            store.put("count", count);
                            BigDecimal sum = (BigDecimal) store.get("sum");
                            sum = sum.add(BigDecimal.valueOf(order.getTotalPrice()));
                            store.put("sum", sum);
                            break;
                        }
                    }
                }
            }
        }
        //2.根据业务量对门店进行排序
        for (int i = 0; i < stores.size() - 1; i++) {
            int count1 = (int) stores.get(i).get("count");
            for (int j = i + 1; j < stores.size(); j++) {
                int count2 = (int) stores.get(j).get("count");
                if (count2 > count1) {
                    count1 = count2;
                    Collections.swap(stores, i, j);
                }
            }
        }
        //3.获取门店名称
        List<String> storeIdList = new ArrayList<>();
        int len = 10;
        if (stores.size() < 10) {
            len = stores.size();
            result = stores;
        } else {
            result = stores.subList(0, 10);
        }
        for (int i = 0; i < len; i++) {
            storeIdList.add((String) stores.get(i).get("storeId"));
        }
        List<Store> storeResults = storeRepository.findByDelStatusAndIdIn(false, storeIdList);
        for (Store s :
                storeResults) {
            String id = s.getId();
            String name = s.getName();
            boolean flag = false;
            for (Map<String, Object> map :
                    result) {
                //找到门店数据
                for (String key :
                        map.keySet()) {
                    if (!key.equals("storeId")) {
                        continue;
                    } else {
                        if (!map.get(key).equals(id)) {
                            continue;
                        } else {
                            flag = true;
                        }
                    }
                }
                if (flag) {
                    map.put("name", name);
                }
            }
        }
        for (Map<String, Object> map :
                result) {
            map.remove("storeId");
        }
        return result;
    }

    /**
     * 按月处理，获取每月客户的平均消费次数
     *
     * @param consumerOrderList
     * @param now
     * @return
     */
    private List<Map<String, Object>> getAverageUsageFrequencyMonthly(List<ConsumerOrder> consumerOrderList, DateTime now) {
        List<Map<String, Object>> result = new ArrayList<>();
        //将数据按月分类
        Map<String, List<ConsumerOrder>> mapResult = new HashMap<>();
        for (ConsumerOrder order :
                consumerOrderList) {
            DateTime createTime = new DateTime(order.getCreateTime().getTime());
            String month = createTime.toString(TimestampUtil.dateTimeFormatter1);
            if (mapResult.keySet().contains(month)) {
                List<ConsumerOrder> list = mapResult.get(month);
                list.add(order);
            } else {
                List<ConsumerOrder> ll = new ArrayList<>();
                ll.add(order);
                mapResult.put(month, ll);
            }
        }
        //数据按月统计
        List<DateTime> timeList = TimestampUtil.getTimeList(now);
        List<String> stimeList = new ArrayList<>();
        for (int i = 0; i < timeList.size() - 1; i++) {
            stimeList.add(timeList.get(i).toString(TimestampUtil.dateTimeFormatter1));
        }
        for (String month :
                stimeList) {
            Map<String, Object> rr = new HashMap<>();
            float r1 = 0;
            if (mapResult.get(month) != null && mapResult.get(month).size() > 0) {
                List<ConsumerOrder> orderPerMonth = mapResult.get(month);
                r1 = getAverageUsageFrequency(orderPerMonth);
            }
            rr.put(month, r1);
            result.add(rr);
        }
        return result;
    }

    /**
     * 根据消费订单获取平均每人消费次数
     *
     * @param consumerOrderList
     * @return
     */
    private float getAverageUsageFrequency(List<ConsumerOrder> consumerOrderList) {
        Set<String> clientIds = new HashSet<>();
        for (ConsumerOrder order :
                consumerOrderList) {
            if (!clientIds.contains(order.getClientId())) {
                clientIds.add(order.getClientId());
            }
        }
        return (float) (Math.round(((float) consumerOrderList.size() / clientIds.size()) * 100)) / 100;
    }
}
