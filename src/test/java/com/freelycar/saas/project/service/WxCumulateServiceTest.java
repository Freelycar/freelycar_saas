package com.freelycar.saas.project.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.BootApplication;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.project.entity.WxCumulate;
import com.freelycar.saas.util.TimestampUtil;
import com.freelycar.saas.util.UpdateTool;
import com.freelycar.saas.wxutils.WechatConfig;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

/**
 * @Author: pyt
 * @Date: 2020/12/22 16:23
 * @Description:
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BootApplication.class)
public class WxCumulateServiceTest extends TestCase {
    @Autowired
    WxCumulateService wxCumulateService;

    @Test
    public void testSaveWxCumulateTask() {
        /*String yesterdayDate = "2020-12-20";
        try {
            JSONArray wxCumulateObj;
            JSONObject jsonObject = WechatConfig.getUserCumulate(yesterdayDate, yesterdayDate);
            wxCumulateObj = jsonObject.getJSONArray("list");
            if (wxCumulateObj.size() == 0) {
                wxCumulateObj = WechatConfig.getUserCumulate(yesterdayDate, yesterdayDate).getJSONArray("list");
            }
            String refDate = wxCumulateObj.getJSONObject(0).getString("ref_date");
            Long cumulateUser = wxCumulateObj.getJSONObject(0).getLong("cumulate_user");

            if (StringUtils.hasText(refDate) && null != cumulateUser) {
                WxCumulate wxCumulate = new WxCumulate();
                wxCumulate.setRefDate(refDate);
                wxCumulate.setCumulateUser(cumulateUser);
                WxCumulate res = wxCumulateService.saveOrUpdate(wxCumulate);
                System.out.println("定时任务saveWxCumulateTask执行成功，存入数据库的数据为：");
                System.out.println(res.toString());
            } else {
                System.out.println("定时任务saveWxCumulateTask中，微信返回数据中为找到有效的refDate和cumulateUser");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            System.out.println("定时任务saveWxCumulateTask执行异常");
        }*/
    }

}