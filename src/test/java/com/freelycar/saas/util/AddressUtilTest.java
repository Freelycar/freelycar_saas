package com.freelycar.saas.util;

import com.freelycar.saas.BootApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

/**
 * @author puyuting
 * @date 2019/12/24
 * @email 2630451673@qq.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BootApplication.class)
@EnableAutoConfiguration
public class AddressUtilTest {
    @Autowired
    private AddressUtil util;

    @Test
    public void testGetLngAndLat(){
        String address = "南京市玄武区AUX钟山府15栋";
        Map<String,Double> result = util.getLngAndLat(address);
        System.out.println(result.get("lng")+","+result.get("lat"));
    }

    @Test
    public void testGetAddress(){
        /*double lng = 116.3084202915042;
        double lat = 40.05703033345938;*/
        double lng = 118.91415870973218;
        double lat = 32.095114965162196;
        /*double lng = 118.82898263171734;
        double lat = 31.955871608973847;*/
        util.getAddress(lng,lat);
    }


}
