package com.freelycar.saas.wxutils;

import com.freelycar.saas.BootApplication;
import org.jasypt.encryption.StringEncryptor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;

/**
 * @author puyuting
 * @date 2020/1/3
 * @email 2630451673@qq.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BootApplication.class)
@EnableAutoConfiguration
public class HttpRequestTest {

    @Autowired
    MiniProgramConfig miniProgramConfig;

    @Test
    public void testGetEParam() {
        System.out.println(miniProgramConfig.getMiniAppId());
        System.out.println(miniProgramConfig.getMiniAppSecret());
    }
}
