package project.service.repotest;

import com.freelycar.saas.BootApplication;
import com.freelycar.saas.project.entity.EOrder;
import com.freelycar.saas.project.repository.EOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author puyuting
 * @date 2020/1/9
 * @email 2630451673@qq.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BootApplication.class)
@EnableAutoConfiguration
@Slf4j
public class EOrderRepositoryTest {
    @Autowired
    private EOrderRepository eOrderRepository;

    @Test
    public void findByConsumerOrderIdTest() {
        log.info("根据ConsumerOrderId获取e代驾订单");
        for (EOrder eOrder : eOrderRepository.findByConsumerOrderId("S0011908100016")) {
            log.info("e代驾订单详情：{}", eOrder.toString());
        }

    }

    @Test
    public void findByOrderIdTest() {
        log.info("e代驾订单详情：{}", eOrderRepository.findByOrderId(43880).get());
    }
}
