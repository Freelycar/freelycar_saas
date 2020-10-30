package project.service;

import com.freelycar.saas.BootApplication;
import com.freelycar.saas.exception.NoEmptyArkException;
import com.freelycar.saas.project.entity.Ark;
import com.freelycar.saas.project.repository.ArkRepository;
import com.freelycar.saas.project.service.ArkService;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    @Autowired
    private ArkRepository arkRepository;

    @Test
    public void checkArkTest() {
        try {
            boolean result = arkService.checkArk("862057048957259");
            System.out.printf("该柜子是否有可用柜门：" + result);
        } catch (NoEmptyArkException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void QueryAll() {
        CloseableHttpClient closeableHttpClient = HttpClients.createDefault();
        CloseableHttpResponse closeableHttpResponse;
        String url = "https://www.freelycar.cn/web/list/device";
        try {
            HttpGet httpGet = new HttpGet(url);
            closeableHttpResponse = closeableHttpClient.execute(httpGet);
            HttpEntity entity = closeableHttpResponse.getEntity();
            String result = EntityUtils.toString(entity, "UTF-8");
            String[] devicesns = result.split("\n");
            Set<String> devicesnSet = new HashSet<>(Arrays.asList(devicesns));
            List<Ark> arkList = arkRepository.findByDelStatus(false);
            for (Ark ark :
                    arkList) {
                String devicesn = ark.getSn();
                if (!devicesnSet.contains(devicesn)) {
                    System.out.println(devicesn + ":不在线");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
