package project.service.repotest;

import com.freelycar.saas.BootApplication;
import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.project.repository.DoorRepository;
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
public class DoorRepositoryTest {
    @Autowired
    private DoorRepository doorRepository;
    @Test
    public void findByArkSnAndStateLessThanAndDelStatusTest() {
        String arkSn = "862057048957259";
        int size = doorRepository.findByArkSnAndStateLessThanAndDelStatus(arkSn,Constants.DoorState.DISABLED.getValue(),Constants.DelStatus.NORMAL.isValue()).size();
        System.out.printf("size:"+size);
    }
}
