package project.service;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.BootApplication;
import com.freelycar.saas.exception.NormalException;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.project.entity.ConsumerOrder;
import com.freelycar.saas.project.entity.Door;
import com.freelycar.saas.project.service.EdaijiaService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author puyuting
 * @date 2019/12/24
 * @email 2630451673@qq.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BootApplication.class)
@EnableAutoConfiguration
public class EdaijiaServiceTest {
    @Autowired
    private EdaijiaService edaijiaService;

    @Test
    public void testEOrder() {
        ConsumerOrder consumerOrder = new ConsumerOrder();
        consumerOrder.setCarId("ea8ecbc5696add0701696b6124390005");
        consumerOrder.setClientId("ea8ecbc5694d1d1d01695193e6a70012");
        Door door = new Door();
        door.setArkSn("862057048892597");
        String serviceProviderId = "402880236f30d2cb016f315a865a0002";
        try {
            edaijiaService.createOrder(consumerOrder, door, serviceProviderId);
        } catch (ObjectNotFoundException | NormalException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testEOrderDetail() {
        Integer orderId = 44088;
        JSONObject result = edaijiaService.orderDetail(orderId);
        Integer code = result.getInteger("code");
        Integer status = result.getJSONObject("data").getJSONObject("orderInfo").getInteger("status");
        System.out.printf("司机手机号："+ code);
    }

    @Test
    public void testDelEOrder() throws NormalException, ObjectNotFoundException {
        Integer orderId = 44091;
        edaijiaService.cancelOrder(orderId);
    }

}
