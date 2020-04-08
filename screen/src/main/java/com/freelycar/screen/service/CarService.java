package com.freelycar.screen.service;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.screen.entity.Car;
import com.freelycar.screen.entity.repo.CarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author pyt
 * @date 2020/4/8 14:46
 * @email 2630451673@qq.com
 * @desc
 */
@Service
public class CarService {
    @Autowired
    private CarRepository carRepository;

    public List<Map<String, Integer>> getCarBrand() {
        Set<String> brandSet = new HashSet<>();
        List<Map<String, Integer>> result = new ArrayList<>();
        List<Car> carList = carRepository.findByDelStatus(false);
        for (Car car :
                carList) {
            String brand = car.getCarBrand();
            if (brand != null && !brand.equals("")) {
                if (!brandSet.contains(brand)) {
                    brandSet.add(brand);
                    Map<String, Integer> map = new HashMap<>();
                    map.put(brand, 1);
                    result.add(map);
                } else {
                    for (Map<String, Integer> map :
                            result) {
                        for (String key :
                                map.keySet()) {
                            if (key.equals(brand)) {
                                int count = map.get(brand);
                                count++;
                                map.put(brand, count);
                            }
                        }
                    }
                }
            }
        }
        for (int i = 0; i < result.size() - 1; i++) {
            int count1 = 0;
            for (String key :
                    result.get(i).keySet()) {
                count1 = result.get(i).get(key);
            }
            for (int j = i + 1; j < result.size(); j++) {
                int count2 = 0;
                for (String key :
                        result.get(j).keySet()) {
                    count2 = result.get(j).get(key);
                }
                if (count1 < count2) {
                    count1 = count2;
                    Collections.swap(result, i, j);
                }
            }
        }
        if (result.size() > 10) result = result.subList(0, 10);
        return result;
    }
}
