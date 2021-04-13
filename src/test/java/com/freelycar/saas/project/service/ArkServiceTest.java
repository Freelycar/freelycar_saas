package com.freelycar.saas.project.service;

import com.freelycar.saas.BootApplication;
import com.freelycar.saas.project.model.ArkStore;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

/**
 * @Author: pyt
 * @Date: 2021/3/19 9:54
 * @Description:
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BootApplication.class)
public class ArkServiceTest extends TestCase {
    @Autowired
    private ArkService arkService;

    @Test
    public void testGetOfflineDevices() {
        /*List<ArkStore> arkStores = arkService.getOfflineDevices();
        for (ArkStore arkStore :
                arkStores) {
            System.out.println(arkStore.toString());
        }*/
    }
}