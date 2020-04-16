package com.freelycar.saas.screen.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.wxutils.HttpRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author pyt
 * @date 2020/4/15 11:54
 * @email 2630451673@qq.com
 * @desc
 */
@Service
public class WeatherService {
    private static String url = "https://api.caiyunapp.com/v2.5/xl5eYnP5hxiEtuTy/118.78,32.07/weather.json";

    public JSONObject getWeather() {
        JSONObject dailyData = null;
        String result = HttpRequest.getCall(url, null, null);
        JSONObject object = JSONObject.parseObject(result);
        if (object.get("status").equals("ok")) {

            JSONObject jsonResult = (JSONObject) object.get("result");
//            System.out.println(jsonResult);
            dailyData = (JSONObject) jsonResult.get("daily");
//            System.out.println(dailyData);
        }
        return dailyData;
    }
}
