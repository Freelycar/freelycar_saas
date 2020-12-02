package com.freelycar.saas.util;

import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.exception.NormalException;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.iotcloudcn.ArkOperation;
import com.freelycar.saas.iotcloudcn.util.ArkThread;
import com.freelycar.saas.iotcloudcn.util.BoxCommandResponse;
import com.freelycar.saas.project.entity.Ark;
import com.freelycar.saas.project.entity.ConsumerOrder;
import com.freelycar.saas.project.entity.Door;
import com.freelycar.saas.project.entity.Staff;
import com.freelycar.saas.project.repository.ArkRepository;
import com.freelycar.saas.project.repository.ConsumerOrderRepository;
import com.freelycar.saas.project.repository.DoorRepository;
import com.freelycar.saas.project.service.EdaijiaService;
import com.freelycar.saas.project.service.StaffService;
import com.freelycar.saas.project.service.WxUserInfoService;
import com.freelycar.saas.wxutils.WechatTemplateMessage;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-11-25
 * Time: 15:32
 */
public class CloseDoorThread implements Runnable {

    private static Logger log = LogManager.getLogger(DeviceStateThread.class);
    private DoorRepository doorRepository;
    private ConsumerOrderRepository consumerOrderRepository;
    private EdaijiaService edaijiaService;
    private StaffService staffService;
    private WxUserInfoService wxUserInfoService;
    private ArkRepository arkRepository;

    private Door door;
    private Boolean orderType;
    private String serviceProviderId;
    private String rspId;
    private BoxCommandResponse boxCommandResponse = null;

    public CloseDoorThread() {
        this.consumerOrderRepository = SpringBeanUtil.getBean(ConsumerOrderRepository.class);
        this.doorRepository = SpringBeanUtil.getBean(DoorRepository.class);
    }

    public CloseDoorThread(Door door, Boolean orderType, String serviceProviderId, String rspId) {
        this.door = door;
        this.orderType = orderType;
        this.serviceProviderId = serviceProviderId;
        this.rspId = rspId;

        this.consumerOrderRepository = SpringBeanUtil.getBean(ConsumerOrderRepository.class);
        this.doorRepository = SpringBeanUtil.getBean(DoorRepository.class);
        this.edaijiaService = SpringBeanUtil.getBean(EdaijiaService.class);
        this.staffService = SpringBeanUtil.getBean(StaffService.class);
        this.wxUserInfoService = SpringBeanUtil.getBean(WxUserInfoService.class);
        this.arkRepository = SpringBeanUtil.getBean(ArkRepository.class);
    }

    @Transactional
    private void updateData(boolean isClosed, Door doorOld) throws NormalException, ObjectNotFoundException {
        String deviceId = doorOld.getArkSn();
        Integer boxId = doorOld.getDoorSn();
        Door door = doorRepository.findTopByArkSnAndDoorSn(deviceId, boxId);
        Ark ark = arkRepository.findTopBySnAndDelStatus(deviceId, Constants.DelStatus.NORMAL.isValue());
        if (null != door && !door.getOrderId().isEmpty()) {
            String orderId = door.getOrderId();
            ConsumerOrder consumerOrder = consumerOrderRepository.findById(orderId).orElse(null);
            if (null != consumerOrder) {
                Integer orderState = consumerOrder.getState();
                if (orderState == Constants.OrderState.RESERVATION.getValue()) {
                    //如果订单当前为预约状态
                    if (isClosed) {
                        //柜门正常关闭，更新柜门状态
                        door.setState(Constants.DoorState.USER_RESERVATION.getValue());
                        doorRepository.saveAndFlush(door);

                        if (orderType) {//1.代驾订单：向e代驾下单，发送短信给代驾师傅
                            log.info("订单：" + orderId + "为代驾订单");
                            Integer EorderId = edaijiaService.createOrder(consumerOrder, door, serviceProviderId);
                            // 推送微信公众号消息，通知用户订单生成成功
                            sendWeChatMsg(consumerOrder);
                            // 向接单司机发送短信链接
                            edaijiaService.sendTemplate(EorderId);
                        } else {//2.普通订单：推送微信消息给技师 需要给这个柜子相关的技师都推送
                            //更新:给项目相关的技师推送微信消息模板
                            staffService.sendWeChatMessageToStaff(consumerOrder, door, null, rspId);
                            // 推送微信公众号消息，通知用户订单生成成功
                            sendWeChatMsg(consumerOrder);
                        }
                    } else {
                        //柜门关闭超时,释放柜门,取消订单并通知用户和技师
                        //释放柜门
                        door.setOrderId(null);
                        door.setState(Constants.DoorState.EMPTY.getValue());
                        doorRepository.saveAndFlush(door);
                        //取消订单
                        consumerOrder.setState(Constants.OrderState.CANCEL.getValue());
                        consumerOrderRepository.saveAndFlush(consumerOrder);
                    }
                }
                if (orderState == Constants.OrderState.RECEIVE_CAR.getValue()) {
                    String staffId = consumerOrder.getPickCarStaffId();
                    String openId = getOpenIdByStaffId(staffId);
                    //如果订单当前为还车状态
                    if (isClosed) {
                        //柜门正常关闭，更新柜门状态
                        door.setState(Constants.DoorState.STAFF_FINISH.getValue());
                        doorRepository.saveAndFlush(door);
                        //更新订单状态
                        consumerOrder.setFinishTime(new Timestamp(System.currentTimeMillis()));
                        consumerOrder.setState(Constants.OrderState.SERVICE_FINISH.getValue());
                        consumerOrderRepository.saveAndFlush(consumerOrder);
                        //通知用户
                        sendWeChatMsg(consumerOrder);
                        //通知技师
                        WechatTemplateMessage.orderChangedForStaff(consumerOrder, openId, door, ark);
                    } else {
                        //通知技师，门未关闭
                        WechatTemplateMessage.orderChangedFailureForStaff(consumerOrder, openId, door, ark);
                    }
                }

            }
        }
    }

    /**
     * 给用户推送微信公众号消息
     *
     * @param consumerOrder
     */
    public void sendWeChatMsg(ConsumerOrder consumerOrder) {
        //推送微信公众号消息，通知用户已开始受理服务
        String phone = consumerOrder.getPhone();
        String openId = wxUserInfoService.getOpenId(phone);
        if (StringUtils.isEmpty(openId)) {
            log.error("未获得到对应的openId，微信消息推送失败");
        } else {
            WechatTemplateMessage.orderChanged(consumerOrder, openId);
        }
    }

    private String getOpenIdByStaffId(String staffId) {
        Staff staff = staffService.findById(staffId);
        if (null == staff) {
            return null;
        } else {
            String phone = staff.getPhone();
            String openId = wxUserInfoService.getOpenId(phone);
            return openId;
        }
    }

    @SneakyThrows
    @Override
    public void run() {
        log.info("开始检查柜门关闭情况——————————————————————");
        long start = System.currentTimeMillis();
        boolean startFlag = true;
        boolean isClosed = false;
        String deviceId = door.getArkSn();
        Integer boxId = door.getDoorSn();
        while (startFlag) {
            synchronized (CloseDoorThread.class) {
                try {
                    Thread.sleep(ArkThread.TIME_INTERVAL);
                    //是否已经超时，超时则直接退出进程
                    if ((System.currentTimeMillis() - start) > ArkThread.TIMEOUT) {
                        log.error(deviceId + " 柜门未关，已超时。线程终止。");
                        startFlag = false;
                        break;
                    }
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                    e.printStackTrace();
                }
                if (StringUtils.isEmpty(deviceId)) {
                    log.error("deviceId为空值，线程终止。deviceId：" + deviceId);
                    break;
                }
                if (0 == boxId) {
                    log.error("boxId为空值，线程终止。boxId：" + boxId);
                    break;
                }
                boxCommandResponse = ArkOperation.queryBox(deviceId, boxId);

                int code = boxCommandResponse.code;
                boolean isOpen = boxCommandResponse.is_open;
                if (ArkOperation.SUCCESS_CODE == code) {
                    if (!isOpen) {
                        log.info(deviceId + " 柜门关闭。正常结束进程");
                        isClosed = true;
                        break;
                    }
                }
            }
            // 让出CPU，给其他线程执行
            Thread.yield();
        }
        updateData(isClosed, door);

    }
}
