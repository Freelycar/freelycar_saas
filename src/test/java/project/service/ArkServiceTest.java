package project.service;

import com.freelycar.saas.BootApplication;
import com.freelycar.saas.exception.NoEmptyArkException;
import com.freelycar.saas.project.service.ArkService;
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
public class ArkServiceTest {
    @Autowired
    private ArkService arkService;

    @Test
    public void checkArkTest() {
        try {
            boolean result = arkService.checkArk("862057048957259");
            System.out.printf("该柜子是否有可用柜门："+result);
        } catch (NoEmptyArkException e) {
            e.printStackTrace();
        }
    }
}
