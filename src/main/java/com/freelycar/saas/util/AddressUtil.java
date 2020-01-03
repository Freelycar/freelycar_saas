package com.freelycar.saas.util;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.wxutils.HttpRequest;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author puyuting
 * @date 2019/12/24
 * @email 2630451673@qq.com
 */
@Component
public class AddressUtil {
    @Value("${baidu.map.ak}")
    private String ak;
    @Value("${baidu.map.geocoding.url}")
    private String geocodingUrl;
    @Value("${baidu.map.geocoding.reverse.url}")
    private String reverseGeocodingUrl;

    private Logger log = LogManager.getLogger(AddressUtil.class);

    public Map<String, Double> getLngAndLat(String address) {
        Map<String,Double> resultMap = new HashedMap<>();
        Map<String, Object> params = new HashedMap<>();
        params.put("ak", ak);
        params.put("output", "json");
        params.put("address",address);
        String result = HttpRequest.getCall(geocodingUrl, params, null);
        JSONObject json;
        try {
            json = JSONObject.parseObject(result);
            Integer status = json.getInteger("status");
            if (status==0){
                JSONObject location = json.getJSONObject("result").getJSONObject("location");
                double lng = location.getDouble("lng");
                double lat = location.getDouble("lat");
                resultMap.put("lng",lng);
                resultMap.put("lat",lat);
            }
        } catch (JSONException e) {
//            e.printStackTrace();
            log.error("解析错误");
        }
        return resultMap;
    }

    public String getAddress(Double lng,Double lat){
        Map<String, Object> params = new HashedMap<>();
        params.put("ak", ak);
        params.put("output", "json");
        params.put("location",lat+","+lng);
        params.put("coordtype","bd09ll");
        String result = HttpRequest.getCall(reverseGeocodingUrl, params, null);
        JSONObject json;
        try {
            json = JSONObject.parseObject(result);
            System.out.println(json);
            Integer status = json.getInteger("status");
            if (status==0){
                return json.getJSONObject("result").getString("formatted_address");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            log.error("解析错误");
        }
        return null;
    }

}
