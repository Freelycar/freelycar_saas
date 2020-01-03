package project.service;

import com.freelycar.saas.BootApplication;
import com.freelycar.saas.project.service.ServiceProviderService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author puyuting
 * @date 2020/1/2
 * @email 2630451673@qq.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BootApplication.class)
@EnableAutoConfiguration
public class ServiceProviderServiceTest {
    @Autowired
    private ServiceProviderService serviceProviderService;

    @Test
    public void testGetLatAndLonAndDetail(){
        String address = "江苏省南京市玄武区苏宁青创园";
        System.out.println(serviceProviderService.getLatAndLonAndDetail(address));
    }
}
