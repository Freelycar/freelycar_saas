package project.service.repotest;

import com.freelycar.saas.BootApplication;
import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.exception.NoEmptyArkException;
import com.freelycar.saas.project.entity.Door;
import com.freelycar.saas.project.repository.ConsumerOrderRepository;
import com.freelycar.saas.project.service.DoorService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
}
