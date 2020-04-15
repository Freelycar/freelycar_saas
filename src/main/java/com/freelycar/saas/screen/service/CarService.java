package com.freelycar.saas.screen.service;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.project.entity.Car;
import com.freelycar.saas.project.entity.CarBrand;
import com.freelycar.saas.project.entity.CarType;
import com.freelycar.saas.project.entity.ConsumerOrder;
import com.freelycar.saas.project.repository.*;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;

/**
 * @author pyt
 * @date 2020/4/8 14:46
 * @email 2630451673@qq.com
 * @desc
 */
@Service("carService1")
public class CarService {
    private final static Logger logger = LoggerFactory.getLogger(CarService.class);
    @Autowired
    private CarRepository carRepository;
    @Autowired
    private CarBrandRepository carBrandRepository;
    @Autowired
    private CarTypeRepository carTypeRepository;
    @Autowired
    private ConsumerOrderRepository consumerOrderRepository;
    @Autowired
    private WxUserInfoRepository wxUserInfoRepository;


    public JSONObject getTop10CarBrand() {
        //******车系与车牌关系********
        List<CarType> carTypeList = carTypeRepository.findAll();
        List<CarBrand> carBrandList = carBrandRepository.findAll();
        Map<Integer, String> carBrandMap = new HashMap<>();
        //车系-品牌
        Map<String, String> carBrand = new HashMap<>();
        for (CarBrand brand :
                carBrandList) {
            carBrandMap.put(brand.getId(), brand.getBrand());
        }
        for (CarType type :
                carTypeList) {
            carBrand.put(type.getType(), carBrandMap.get(type.getCarBrandId()));
        }
        //******品牌-用户clientIds********
        List<Map<String, Set<String>>> brandUsers = new ArrayList<>();
        Map<String, Object> resultMap1 = getCarTypeSum();
        List<Map<String, Set<String>>> typeUsers = (List<Map<String, Set<String>>>) resultMap1.get("list1");
        Set<String> brandSet = new HashSet<>();
        for (Map<String, Set<String>> map : typeUsers) {
            for (String key : map.keySet()) {
                String realBrand = carBrand.get(key);//品牌
                if (realBrand == null) {
                    realBrand = key;
                }
                if (!brandSet.contains(realBrand)) {
                    brandSet.add(realBrand);
                    Map<String, Set<String>> brandMap = new HashMap<>();
                    brandMap.put(realBrand, map.get(key));
                    brandUsers.add(brandMap);
                } else {
                    boolean flag = false;
                    for (Map<String, Set<String>> brandMap : brandUsers) {
                        for (String bmkey : brandMap.keySet()) {
                            if (bmkey.equals(realBrand)) {
                                Set<String> clientIds = brandMap.get(bmkey);
                                clientIds.addAll(map.get(key));
                                break;
                            }
                        }
                        if (flag) break;
                    }
                }
                break;
            }
        }
//        logger.info("******品牌-用户clientIds********");
//        for (Map<String, Set<String>> map :
//                brandUsers) {
//            logger.info("{}", map);
//        }

        //******结果：用户中品牌排名********
        //用户车系数据统计
        List<Map<String, Integer>> resultPre1 = (List<Map<String, Integer>>) resultMap1.get("list2");
        //用户车牌排名结果
        List<Map<String, Integer>> resultMiddle1 = getResult1(resultPre1, carBrand);
        //计算%
        int sum1 = (int) resultMap1.get("sum");
        List<Map<String, Double>> result1 = new ArrayList<>();
        for (Map<String, Integer> map :
                resultMiddle1) {
            Map<String, Double> mapResult = new HashMap<>();
            for (String key :
                    map.keySet()) {
                double percent = (double) (Math.round(((double) map.get(key) / sum1) * 100)) / 100;
                mapResult.put(key, percent);
                result1.add(mapResult);
            }
        }
        Map<String, Object> resultMap2 = getCarTypeByUserConsumption();
        //******结果：用户消费次数品牌排名********
        List<Map<String, Integer>> resultPre2 = (List<Map<String, Integer>>) resultMap2.get("result1");
        //用户车牌排名结果
        List<Map<String, Integer>> result2 = getResult1(resultPre2, carBrand);
        if (result2.size() > 6) result2 = result2.subList(0, 6);
        //******结果：用户平均消费金额品牌排名********
        List<Map<String, Object>> resultPre3 = (List<Map<String, Object>>) resultMap2.get("result2");
        List<Map<String, Double>> result3 = getResult2(resultPre3, carBrand);

        //******结果：本月用户增长率********
        List<Map<String, Object>> currentUserIncrement = new ArrayList<>();
        DateTime dateTime = DateTime.now();
        DateTime firstDay = dateTime
                .minusMonths(1)//为了方便测试使用上月1号到当前数据
                .withDayOfMonth(1)    //当月第一天
                .withHourOfDay(0)     //当天0点
                .withMinuteOfHour(0)  //当小时0分
                .withSecondOfMinute(0)//当分钟0秒
                .withMillisOfSecond(0); //当秒0毫秒
        Timestamp start = new Timestamp(firstDay.getMillis());
        Set<String> clientIdSet = wxUserInfoRepository.findClientIdByDelStatusAndCreateTimeAfter(false, start);
        for (Map<String, Set<String>> map :
                brandUsers) {
            for (String key :
                    map.keySet()) {
                Map<String, Object> map1 = new HashMap<>();
                double percent = 0;
                Set<String> clientSum = map.get(key);
                int count = 0;
                for (String clientId :
                        clientSum) {
                    if (clientIdSet.contains(clientId)) count++;
                }
                if (count > 0) percent = (double) (Math.round(((double) count / clientSum.size()) * 100)) / 100;
                map1.put("brand", key);
                map1.put("percent", percent);
                map1.put("count", count);
                map1.put("sum", clientSum.size());
                currentUserIncrement.add(map1);
            }
        }
        for (int i = 0; i < currentUserIncrement.size() - 1; i++) {
            Map<String, Object> map1 = currentUserIncrement.get(i);
            double percent1 = (double) map1.get("percent");
            for (int j = i + 1; j < currentUserIncrement.size(); j++) {
                Map<String, Object> map2 = currentUserIncrement.get(j);
                double percent2 = (double) map2.get("percent");
                if (percent1 < percent2) {
                    percent1 = percent2;
                    Collections.swap(currentUserIncrement, i, j);
                }
            }
        }
        if (currentUserIncrement.size() > 6) currentUserIncrement = currentUserIncrement.subList(0, 6);
        //复购用户数
        List<Map<String, Double>> repeatUserList = new ArrayList<>();
        Set<String> repeatUser = (Set<String>) resultMap2.get("result3");
        for (Map<String, Set<String>> map :
                brandUsers) {
            for (String key : map.keySet()) {
                Map<String, Double> map1 = new HashMap<>();
                int count = 0;
                double percent = 0;
                Set<String> clientSum = map.get(key);
                for (String clientId : clientSum) {
                    if (repeatUser.contains(clientId)) count++;
                }
                if (count > 0) percent = (double) (Math.round(((double) count / clientSum.size()) * 100)) / 100;
                map1.put(key, percent);
                repeatUserList.add(map1);
            }
        }
        repeatUserList = sort(repeatUserList);
        JSONObject result = new JSONObject();
        result.put("result1", result1);
        result.put("result2", result2);
        result.put("result3", result3);
        result.put("currentUserIncrement", currentUserIncrement);
        result.put("repeatUserList", repeatUserList);
        return result;
    }

    private List<Map<String, Double>> sort(List<Map<String, Double>> result) {
        for (int i = 0; i < result.size() - 1; i++) {
            Map<String, Double> map1 = result.get(i);
            double d1 = 0;
            for (String key : map1.keySet()) {
                d1 = map1.get(key);
                break;
            }
            for (int j = i + 1; j < result.size(); j++) {
                Map<String, Double> map2 = result.get(j);
                double d2 = 0;
                for (String key : map2.keySet()) {
                    d2 = map2.get(key);
                    break;
                }
                if (d1 < d2) {
                    d1 = d2;
                    Collections.swap(result, i, j);
                }
            }
        }
        if (result.size() > 6) result = result.subList(0, 6);
        return result;
    }

    private List<Map<String, Double>> getResult2(List<Map<String, Object>> resultPre, Map<String, String> carBrand) {
        List<Map<String, Double>> result = new ArrayList<>();
        List<Map<String, Object>> resultMiddle = new ArrayList<>();
        Set<String> brandSet1 = new HashSet<>();
        for (Map<String, Object> map :
                resultPre) {
            String carType = (String) map.get("carType");
            Set<String> clientIds = (Set<String>) map.get("clientIds");
            double sum = (double) map.get("sum");
            String realBrand = carBrand.get(carType);//品牌
            if (realBrand == null) {
                realBrand = carType;
            }
            if (!brandSet1.contains(realBrand)) {
                brandSet1.add(realBrand);
                Map<String, Object> map1 = new HashMap<>();
                map1.put("brand", realBrand);
                map1.put("clientIds", clientIds);
                map1.put("sum", sum);
                resultMiddle.add(map1);
            } else {
                for (Map<String, Object> map1 :
                        resultMiddle) {
                    if (map1.get("brand").equals(realBrand)) {
                        double sum1 = (double) map1.get("sum");
                        Set<String> clientIds1 = (Set<String>) map1.get("clientIds");
                        sum1 += sum;
                        clientIds1.addAll(clientIds);
                        map1.put("clientIds", clientIds1);
                        map1.put("sum", sum1);
                        break;
                    }
                }
            }
        }
        for (Map<String, Object> map :
                resultMiddle) {
            Map<String, Double> mapR = new HashMap<>();
            String key = (String) map.get("brand");
            int len = ((Set<String>) map.get("clientIds")).size();
            double sum = (double) map.get("sum");
            double averageConsumption = (double) (Math.round((sum / len) * 100)) / 100;
            mapR.put(key, averageConsumption);
            result.add(mapR);
        }
        result = sort(result);
        return result;
    }

    private List<Map<String, Integer>> getResult1(List<Map<String, Integer>> resultPre1, Map<String, String> carBrand) {
        List<Map<String, Integer>> result1 = new ArrayList<>();
        Set<String> brandSet1 = new HashSet<>();
        for (Map<String, Integer> map : resultPre1) {
            for (String key ://车系
                    map.keySet()) {
                String realBrand = carBrand.get(key);//品牌
                if (realBrand == null) {
                    realBrand = key;
                }
                if (realBrand != null) {
                    if (!brandSet1.contains(realBrand)) {
                        brandSet1.add(realBrand);
                        Map<String, Integer> brandMap = new HashMap<>();
                        brandMap.put(realBrand, map.get(key));
                        result1.add(brandMap);
                    } else {
                        for (Map<String, Integer> brandMap :
                                result1) {
                            for (String key1 : //品牌
                                    brandMap.keySet()) {
                                if (key1.equals(realBrand)) {
                                    int count = brandMap.get(realBrand);
                                    count += map.get(key);
                                    brandMap.put(realBrand, count);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        //对品牌结果排序
        for (int i = 0; i < result1.size() - 1; i++) {
            int count1 = 0;
            for (String key : result1.get(i).keySet()) {
                count1 = result1.get(i).get(key);
            }
            for (int j = i + 1; j < result1.size(); j++) {
                int count2 = 0;
                for (String key : result1.get(j).keySet()) {
                    count2 = result1.get(j).get(key);
                }
                if (count1 < count2) {
                    count1 = count2;
                    Collections.swap(result1, i, j);
                }
            }
        }
        if (result1.size() > 10) result1 = result1.subList(0, 10);
        return result1;
    }


    /**
     * 获得用户车系数据统计
     *
     * @return
     */
    private Map<String, Object> getCarTypeSum() {
        Map<String, Object> result = new HashMap<>();
        Set<String> brandSet = new HashSet<>();
        List<Map<String, Set<String>>> list1 = new ArrayList<>();
        List<Car> carList = carRepository.findByDelStatus(false);
        for (Car car :
                carList) {
            String brand = car.getCarBrand();
            if (brand != null && !brand.equals("")) {
                if (!brandSet.contains(brand)) {
                    brandSet.add(brand);
                    Map<String, Set<String>> map = new HashMap<>();
                    Set<String> clientIds = new HashSet<>();
                    clientIds.add(car.getClientId());
                    map.put(brand, clientIds);
                    list1.add(map);
                } else {
                    for (Map<String, Set<String>> map :
                            list1) {
                        for (String key :
                                map.keySet()) {
                            if (key.equals(brand)) {
                                Set<String> clientIds = map.get(brand);
                                clientIds.add(car.getClientId());
                                map.put(brand, clientIds);
                                break;
                            }
                        }
                    }
                }
            }
        }
        //车系-clientIds
        result.put("list1", list1);
        List<Map<String, Integer>> list2 = new ArrayList<>();
        for (Map<String, Set<String>> map :
                list1) {
            for (String key : map.keySet()) {
                Map<String, Integer> map1 = new HashMap<>();
                map1.put(key, map.get(key).size());
                list2.add(map1);
                break;
            }
        }
        //车系-用户数量
        result.put("list2", list2);
        result.put("sum", carList.size());
        return result;
    }

    /**
     * result1：用户消费次数-车品牌
     * result2：用户平均消费金额-车品牌
     * result3: 复购用户
     *
     * @return
     */
    private Map<String, Object> getCarTypeByUserConsumption() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Integer>> result1 = new ArrayList<>();
        List<Map<String, Object>> result2 = new ArrayList<>();
        List<ConsumerOrder> consumerOrderList = consumerOrderRepository.findByDelStatus(false);
        Set<String> carTypeSet = new HashSet<>();
        Set<String> clientId1 = new HashSet<>();
        Set<String> clientId2 = new HashSet<>();
        for (ConsumerOrder order :
                consumerOrderList) {
            String clientId = order.getClientId();
            if (clientId != null) {
                if (clientId1.contains(clientId)) clientId2.add(clientId);
                clientId1.add(clientId);
            }
            String carType = order.getCarBrand();
            if (carType != null) {
                if (!carTypeSet.contains(carType)) {
                    carTypeSet.add(carType);
                    //用户消费次数统计
                    Map<String, Integer> map1 = new HashMap<>();
                    map1.put(carType, 1);
                    result1.add(map1);
                    //用户平均消费金额统计
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("carType", carType);
                    Set<String> clientIds = new HashSet<>();
                    clientIds.add(order.getClientId());
                    map2.put("clientIds", clientIds);
                    map2.put("sum", order.getActualPrice());
                    result2.add(map2);
                } else {
                    //用户消费次数统计
                    for (Map<String, Integer> map1 :
                            result1) {
                        for (String key :
                                map1.keySet()) {
                            if (key.equals(carType)) {
                                int count = map1.get(key);
                                count++;
                                map1.put(carType, count);
                                break;
                            }
                        }
                    }
                    //用户平均消费金额统计
                    for (Map<String, Object> map2 :
                            result2) {
                        if (map2.get("carType").equals(carType)) {
                            Set<String> clientIds = (Set<String>) map2.get("clientIds");
                            clientIds.add(order.getClientId());
                            map2.put("clientIds", clientIds);
                            double sum = (double) map2.get("sum");
                            sum += order.getActualPrice();
                            map2.put("sum", sum);
                            break;
                        }
                    }
                }
            }
        }
        result.put("result1", result1);
        result.put("result2", result2);
        result.put("result3", clientId2);
        return result;
    }
}
