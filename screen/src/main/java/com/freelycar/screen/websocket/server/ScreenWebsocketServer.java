package com.freelycar.screen.websocket.server;

import com.freelycar.screen.enums.ResultEnum;
import com.freelycar.screen.exception.WebSocketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author pyt
 * @date 2020/3/31 10:52
 * @email 2630451673@qq.com
 * @desc
 */
@ServerEndpoint("/test")
@Component
public class ScreenWebsocketServer {
    private final static Logger logger = LoggerFactory.getLogger(ScreenWebsocketServer.class);
    /**
     * concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
     * 在外部可以获取此连接的所有websocket对象，并能对其触发消息发送功能，我们的定时发送核心功能的实现在与此变量
     */
    private static CopyOnWriteArraySet<ScreenWebsocketServer> webSocketSet = new CopyOnWriteArraySet<ScreenWebsocketServer>();

    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;

    @OnOpen
    public void onOpen(Session session) {
        logger.info("有新的客户端连接了: {}", session.getId());
        //将新用户存入在线的组
        this.session = session;
        webSocketSet.add(this);
        try {
            sendMessage("连接成功建立");
        } catch (IOException e) {
            logger.error("IO异常");
        }
    }

    /**
     * 客户端关闭
     *
     * @param session session
     */
    @OnClose
    public void onClose(Session session) {
        logger.info("有用户断开了, id为:{}", session.getId());
        //将掉线的用户移除在线的组里
        webSocketSet.remove(this);
    }

    /**
     * 发生错误
     *
     * @param throwable e
     */
    @OnError
    public void onError(Throwable throwable) {
        logger.error("websocket发生错误");
        throw new WebSocketException(ResultEnum.WEBSOCKET_ERROR);
    }

    /**
     * 收到客户端发来消息
     *
     * @param message 消息对象
     */
    @OnMessage
    public void onMessage(String message) {
        logger.info("服务端收到客户端发来的消息: {}", message);
    }

    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }



    public static CopyOnWriteArraySet<ScreenWebsocketServer> getWebSocketSet() {
        return webSocketSet;
    }

    public static void setWebSocketSet(CopyOnWriteArraySet<ScreenWebsocketServer> webSocketSet) {
        ScreenWebsocketServer.webSocketSet = webSocketSet;
    }
}
