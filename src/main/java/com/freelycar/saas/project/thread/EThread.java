package com.freelycar.saas.project.thread;


import com.freelycar.saas.project.service.EdaijiaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * @author puyuting
 * @date 2019/12/25
 * @email 2630451673@qq.com
 */
public class EThread implements Runnable {
    @Value("${edaijia.order}")
    private String orderUrl;

    private Integer orderId;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public Integer getOrderId() {
        return orderId;
    }

    public EThread(Integer orderId) {
        this.orderId = orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    @Override
    public void run() {
        logger.info("线程开始");
        boolean flag = true;
        while (flag) {
            try {
//                System.out.println(System.currentTimeMillis());
                Thread.sleep(1000);
                String url = orderUrl + "detail";
                logger.info("向"+url+"发送订单详情请求");
                logger.info("获取订单进度");
            } catch (InterruptedException e) {
//                System.out.println(e.getMessage());
            }
        }
    }
}
