package project.service;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.BootApplication;
import com.freelycar.saas.project.entity.EOrder;
import com.freelycar.saas.project.service.EdaijiaService;
import com.freelycar.saas.project.thread.EThread;
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
        EOrder order = new EOrder();
        order.setCarNo("test");
        order.setChannel(1);
        /*JSONObject params = JSONObject.parseObject(JSONObject.toJSONString(order));
        System.out.println(order);
        System.out.println(params);*/
        /*int result = edaijiaService.createOrder(order);
        System.out.println("订单号："+result);*/
    }

}
