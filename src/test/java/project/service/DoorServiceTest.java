package project.service;

import com.freelycar.saas.BootApplication;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.exception.NoEmptyArkException;
import com.freelycar.saas.project.entity.Door;
import com.freelycar.saas.project.service.DoorService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author tangwei - Toby
 * @date 2019-08-21
 * @email toby911115@gmail.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BootApplication.class)
@EnableAutoConfiguration
public class DoorServiceTest {
    @Autowired
    private DoorService doorService;

    @Test
    public void getUsefulDoor() {
        try {
            Door door = doorService.getUsefulDoor("862057048892555");
            System.out.println(door);
        } catch (NoEmptyArkException | ArgumentMissingException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void countAllByUserKeyLocationSnContainsAndDelStatusAndState(){

    }
}
