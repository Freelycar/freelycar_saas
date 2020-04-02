package com.freelycar.screen.service;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.screen.entity.ConsumerOrder;
import com.freelycar.screen.entity.Store;
import com.freelycar.screen.entity.repo.ConsumerOrderRepository;
import com.freelycar.screen.entity.repo.StoreRepository;
import com.freelycar.screen.utils.TimestampUtil;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
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
    @Autowired
    private ConsumerOrderRepository consumerOrderRepository;
    @Autowired
    private StoreRepository storeRepository;

    /**
     * 1.最近12月每月智能柜使用用户数:activeUser
     * （同一用户当月多次使用仅作为一条数据）
     * 2.门店排名
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
        return result;
    }

    /**
     * 处理用户订单列表
     * 每月每个用户只保留一条数据
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
            for (int j = 1; j < stores.size(); j++) {
                int count2 = (int) stores.get(j).get("count");
                if (count2 > count1) {
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
        return result;
    }

    public static void main(String[] args) {
        List<Integer> nums = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            nums.add(i);
        }
        System.out.println(nums.toString());
        List<Integer> result = nums.subList(0, 10);
        System.out.println(result.toString());
    }
}
