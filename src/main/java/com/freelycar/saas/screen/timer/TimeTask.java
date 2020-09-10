package com.freelycar.saas.screen.timer;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.screen.service.*;
import com.freelycar.saas.screen.websocket.server.ScreenWebsocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author pyt
 * @date 2020/3/31 15:44
 * @email 2630451673@qq.com
 * @desc
 */
@Component
@EnableScheduling
public class TimeTask {
    private final static Logger logger = LoggerFactory.getLogger(TimeTask.class);
    @Autowired
    private ScreenService screenService;

//    @Scheduled(cron = "0 0/1 * * * ?")   //每分钟执行一次
    @Scheduled(cron = "0 0 0 * * ?")//每天执行一次
    public void test() {
        logger.info("**************开始定时推送**************");
        CopyOnWriteArraySet<ScreenWebsocketServer> webSocketSet =
                ScreenWebsocketServer.getWebSocketSet();
        JSONObject result = screenService.result();
        webSocketSet.forEach(c -> {
            try {
                c.sendMessage(result.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        logger.info("**************定时推送结束**************");
    }
}
