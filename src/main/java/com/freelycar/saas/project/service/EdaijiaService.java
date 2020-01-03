package com.freelycar.saas.project.service;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.project.entity.ConsumerOrder;
import com.freelycar.saas.project.entity.Door;
import com.freelycar.saas.project.entity.EOrder;
import com.freelycar.saas.project.thread.EThread;
import com.freelycar.saas.wxutils.HttpRequest;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * @author puyuting
 * @date 2019/12/24
 * @email 2630451673@qq.com
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class EdaijiaService {
    @Value("${edaijia.order}")
    private String orderUrl;
    @Value("${edaijia.channel}")
    private Integer channel;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EOrderService eOrderService;


    /**
     * 创建e代驾订单并向e代驾发送下单请求（未完成）
     *
     * @param consumerOrder
     * @param door
     * @return
     */
    public void createOrder(ConsumerOrder consumerOrder, Door door, String serviceProviderId) throws ObjectNotFoundException {
        String url = orderUrl + "create";
        EOrder eOrder = eOrderService.create(door.getArkSn(), consumerOrder.getCarId(), consumerOrder.getClientId(), serviceProviderId);
        logger.info("向" + url + "发送下单请求");
        JSONObject param = JSONObject.parseObject(JSONObject.toJSONString(eOrder));
        HttpEntity entity = HttpRequest.getEntity(param);
        String result = HttpRequest.postCall(url, entity, null);
        JSONObject jsonResult = JSONObject.parseObject(result);
        logger.info("下单结果：{}", jsonResult);
        /*if (jsonResult.getInteger("code")==0){//下单成功，查询司机详情（线程开启）
            Integer orderId = jsonResult.getInteger("data");
            logger.info("开始查询e代驾订单：{} 详情",orderId);
            EThread eThread = new EThread(orderId);
            Thread th = new Thread(eThread);
            th.start();
        }*/
    }

    /**
     * 取消e代驾订单（未完成）
     *
     * @param orderId
     */
    public void cancelOrder(Integer orderId) {
        String url = orderUrl + "cancel";
        JSONObject param = new JSONObject();
        param.put("orderId", orderId);
        HttpEntity entity = HttpRequest.getEntity(param);
        HttpRequest.postCall(url, entity, null);
    }

    /**
     * 获取订单详情(未完成)
     */
    public void orderDetail(Integer orderId) {
        String url = orderUrl + "detail";
        Map<String, Object> param = new HashedMap<>();
        param.put("orderId", orderId);
        param.put("channel", channel);
        HttpRequest.getCall(url, HttpRequest.getEParam(param), null);
        logger.info("向" + url + "发送订单详情请求");
        logger.info("获取订单进度");
    }
}
