package project.service.repotest;

import com.freelycar.saas.BootApplication;
import com.freelycar.saas.project.entity.ServiceProvider;
import com.freelycar.saas.project.repository.ServiceProviderRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

/**
 * @author puyuting
 * @date 2019/12/23
 * @email 2630451673@qq.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BootApplication.class)
@EnableAutoConfiguration
public class ServiceProviderRepositoryTest {
    @Autowired
    private ServiceProviderRepository repository;

    @Test
    public void testFindByName(){
        String name = "test";
        System.out.printf("size:"+repository.findByNameAndDelStatus(name,false).size());;
    }

    @Test
    public void testDelete(){
        String id = "402880236f30d2cb016f315a865a0002";
        repository.delById(id);
    }

    @Test
    public void testList(){
        String name = "1";
        List<ServiceProvider> list = repository.findByDelStatusAndNameContainingOrderByIdAsc(false, name);
        for (ServiceProvider pro :
                list) {
            System.out.println(pro.toString());
        }
    }
}
