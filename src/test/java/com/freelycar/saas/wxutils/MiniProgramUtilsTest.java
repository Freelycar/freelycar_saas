package com.freelycar.saas.wxutils;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.BootApplication;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @Author: pyt
 * @Date: 2021/3/2 14:13
 * @Description:
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BootApplication.class)
public class MiniProgramUtilsTest extends TestCase {
    @Autowired
    private MiniProgramUtils utils;

    @Test
    public void testCode2Session() {
        JSONObject result = utils.code2Session("043o4cll2Ug8C64y7hnl2SoAJh4o4clI");
        System.out.println(result.toString());
    }

    @Test
    public void testGetAccessTokenForInteface() {
//        JSONObject res = utils.getAccessTokenForInteface();
//        System.out.println(res.toString());
    }
}