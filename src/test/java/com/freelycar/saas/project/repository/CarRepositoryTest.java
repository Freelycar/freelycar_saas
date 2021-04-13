package com.freelycar.saas.project.repository;

import com.freelycar.saas.BootApplication;
import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.project.entity.Car;
import com.freelycar.saas.project.entity.Client;
import com.freelycar.saas.project.entity.WxUserInfo;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author: pyt
 * @Date: 2021/3/25 18:19
 * @Description:
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BootApplication.class)
public class CarRepositoryTest extends TestCase {
    @Autowired
    private CarRepository carRepository;
    @Autowired
    private WxUserInfoRepository wxUserInfoRepository;
    @Autowired
    private ClientRepository clientRepository;

    @Test
    public void testFindByWxUserIdAndDelStatus() {
        List<Car> carList = carRepository.findByWxUserIdAndDelStatus("ea8ecbc56ecf93e7016ee94776fd00a0",Constants.DelStatus.NORMAL.isValue());
        System.out.println(" 结果如下");
        for (Car car :
                carList) {
            System.out.println(car.toString());
        }
    }

    @Test
    public void deleteDuplicateCar() {
        List<WxUserInfo> wxUserInfoList = wxUserInfoRepository.findAll();
        for (WxUserInfo wxUserInfo :
                wxUserInfoList) {
            String wxUserId = wxUserInfo.getId();
            String phone = wxUserInfo.getPhone();
            if (StringUtils.isEmpty(phone)) continue;
            List<Client> clientList = clientRepository.findByPhoneAndDelStatusOrderByCreateTimeAsc(phone, Constants.DelStatus.NORMAL.isValue());
            Set<String> clientIds = new HashSet<>();
            for (Client c :
                    clientList) {
                clientIds.add(c.getId());
            }
            Set<String> licensePlates = new HashSet<>();
            List<Car> carList = carRepository.findByClientIdInAndDelStatus(clientIds, Constants.DelStatus.NORMAL.isValue());
            List<Car> pre_delete_carList = new ArrayList<>();
            List<Car> pre_update_carList = new ArrayList<>();
            for (Car car : carList) {
                String licensePlate = car.getLicensePlate();
                if (licensePlates.contains(licensePlate)) {
                    pre_delete_carList.add(car);
                } else {
                    licensePlates.add(licensePlate);
                    car.setWxUserId(wxUserId);
                    pre_update_carList.add(car);
                }
            }
            carRepository.saveAll(pre_update_carList);
            carRepository.deleteAll(pre_delete_carList);
        }
    }
}