package com.freelycar.saas.util;

import com.freelycar.saas.BootApplication;
import com.freelycar.saas.wxutils.LeanCloudUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author puyuting
 * @date 2019/12/20
 * @email 2630451673@qq.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BootApplication.class)
@EnableAutoConfiguration
public class LeanCloudUtilsTest {
    @Autowired
    private LeanCloudUtils leanCloudUtils;

    @Test
    public void testGetVerification() {
        leanCloudUtils.getVerification("18206295380", false);
    }

    @Test
    public void testSendTemplate(){
//        leanCloudUtils.sendTemplate("18206295380");
        leanCloudUtils.sendVerifyCode("18206295380","123456");
    }
}
