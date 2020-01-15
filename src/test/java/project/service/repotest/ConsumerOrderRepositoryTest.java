package project.service.repotest;

import com.freelycar.saas.BootApplication;
import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.exception.NoEmptyArkException;
import com.freelycar.saas.project.entity.ConsumerOrder;
import com.freelycar.saas.project.entity.Door;
import com.freelycar.saas.project.repository.ConsumerOrderRepository;
import com.freelycar.saas.project.service.DoorService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * @author puyuting
 * @date 2019/12/19
 * @email 2630451673@qq.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BootApplication.class)
@EnableAutoConfiguration
public class ConsumerOrderRepositoryTest {
    @Autowired
    private ConsumerOrderRepository consumerOrderRepository;

    @Test
    public void countAllByUserKeyLocationSnContainsAndDelStatusAndStateLessThan() {
        String arkSn = "862057048957259";
        boolean delStatus = Constants.DelStatus.NORMAL.isValue();
        int state =  Constants.OrderState.HAND_OVER.getValue();
        int result = consumerOrderRepository.countAllByUserKeyLocationSnContainsAndDelStatusAndStateLessThan(arkSn,delStatus,state);
        System.out.printf("result:"+result);
    }

    @Test
    public void findByIdInTest(){
        List<String> ids = new ArrayList<>();
        ids.add("A0011905080002");
        ids.add("A0011908100012");
        ids.add("A0011907040001");
        ids.add("A0011908020002");
        ids.add("A0011908130008");
        ids.add("A0011908130007");
        ids.add("A0011908100012");
        ids.add("A0011908140001");
        ids.add("A0011908140005");
        ids.add("A0011908140006");
        ids.add("A0011908140007");



        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "createTime"));
        Page<ConsumerOrder> page = consumerOrderRepository.findByIdIn(ids, new PageRequest(0, 10, new Sort(orders)));
        System.out.println("------------------------------------------------------------");
        for (ConsumerOrder order:page.getContent()
             ) {
            System.out.println(order.toString());
        }
        System.out.println("------------------------------------------------------------");
    }
}
