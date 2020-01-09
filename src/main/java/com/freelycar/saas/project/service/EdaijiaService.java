package com.freelycar.saas.project.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.basic.wrapper.ResultCode;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.*;
import com.freelycar.saas.project.entity.*;
import com.freelycar.saas.project.repository.ConsumerOrderRepository;
import com.freelycar.saas.project.repository.DoorRepository;
import com.freelycar.saas.project.repository.EOrderRepository;
import com.freelycar.saas.wxutils.HttpRequest;
import com.freelycar.saas.wxutils.LeanCloudUtils;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.http.HttpEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
    @Autowired
    private EOrderRepository eOrderRepository;
    @Autowired
    private ServiceProviderService serviceProviderService;
    @Autowired
    private LeanCloudUtils leanCloudUtils;
    @Autowired
    private DoorRepository doorRepository;
    @Autowired
    private ConsumerOrderRepository consumerOrderRepository;
    @Autowired
    private ConsumerOrderService consumerOrderService;
    @Autowired
    private ConsumerProjectInfoService consumerProjectInfoService;
    @Autowired
    private DoorService doorService;


    /**
     * 创建e代驾订单并向e代驾发送下单请求（未完成）
     *
     * @param consumerOrder
     * @param door
     * @return
     */
    public Integer createOrder(ConsumerOrder consumerOrder, Door door, String serviceProviderId) throws ObjectNotFoundException, NormalException {
        String url = orderUrl + "create";
        EOrder eOrder = eOrderService.create(door.getArkSn(), consumerOrder, serviceProviderId);
        logger.info("向" + url + "发送下单请求");
        JSONObject param = JSONObject.parseObject(JSONObject.toJSONString(eOrder));
        HttpEntity entity = HttpRequest.getEntity(param);
        String result = HttpRequest.postCall2(url, HttpRequest.getEParam(param), entity, null);
        JSONObject jsonResult = JSONObject.parseObject(result);
        logger.info("下单结果：{}", jsonResult);
        if (jsonResult.getInteger("code") == 0) {//下单成功，查询司机详情（线程开启）
            Integer orderId = jsonResult.getInteger("data");
            eOrder.setOrderId(orderId);
            eOrderRepository.saveAndFlush(eOrder);
            logger.info("开始查询e代驾订单：{} 详情", orderId);
            return orderId;
        } else throw new NormalException("创建e代驾订单失败");
    }

    /**
     * 用户下单完成后，给接车司机发送接车短信
     *
     * @param orderId
     */
    public void sendTemplate(Integer orderId) {
        logger.info("线程开始");
        AtomicBoolean flag = new AtomicBoolean(true);
        while (flag.get()) {
            try {
                Thread.sleep(30000);//30s
                //1.获取详情
                JSONObject orderDetail = orderDetail(orderId);
                if (orderDetail.get("code") != null && orderDetail.getInteger("code") == 0) {
                    logger.info("成功获取订单：{}详情", orderDetail);
                    Integer status = orderDetail.getJSONObject("data").getJSONObject("orderInfo").getInteger("status");
                    if (status == 5) {//订单取消
                        logger.info("订单已取消，不再给司机发送接车短信");
                        flag.set(false);
                    } else if (status == 8) {//司机已就位，发送短信
                        logger.info("司机已就位，准备发送短信");
                        String driverPhone;
                        //1.获取司机电话
                        JSONArray array = orderDetail.getJSONObject("data").getJSONArray("drivingInfoList");
                        for (Object driver :
                                array) {
                            JSONObject driverInfo = (JSONObject) driver;
                            if (driverInfo.get("driverPhone") != null && !driverInfo.getString("driverPhone").equals("")) {
                                driverPhone = driverInfo.getString("driverPhone");
                                //2.给driverPhone发送链接
                                int password = leanCloudUtils.getPassword();
                                EOrder eOrder = eOrderService.findByOrderId(orderId);
                                if (eOrder != null) {
                                    eOrder.setVerifyCode(password);
                                    eOrderRepository.saveAndFlush(eOrder);
                                } else throw new ObjectNotFoundException("未找到对应的代驾订单");
                                String link = "/verifyCode?sign=";
                                Map<String, Object> param = new HashedMap<>();
                                param.put("password", password);
                                param.put("orderId", orderId);
                                link = link + "" + HttpRequest.getSign(param)
                                        + "&password=" + password
                                        + "&orderId=" + orderId;
                                leanCloudUtils.sendTemplate(driverPhone, password, link);
                                break;
                            }
                        }
                        flag.set(false);
                    }
                }
            } catch (InterruptedException | ObjectNotFoundException e) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * 司机取完钥匙后，给服务商发送收车验证码
     *
     * @param orderId
     * @param serviceProviderId
     */
    public void sendVerifyCode(Integer orderId, String serviceProviderId) {
        logger.info("线程开始");
        AtomicBoolean flag = new AtomicBoolean(true);
        while (flag.get()) {
            try {
                Thread.sleep(30000);//30s
                //1.获取详情
                JSONObject orderDetail = orderDetail(orderId);
                if (orderDetail.get("code") != null && orderDetail.getInteger("code") == 0) {
                    logger.info("成功获取订单：{}详情", orderDetail);
                    Integer status = orderDetail.getJSONObject("data").getJSONObject("orderInfo").getInteger("status");
                    if (status == 5) {//订单取消
                        logger.info("订单已取消，不再向服务商发送收车验证码");
                        flag.set(false);
                    } else if (status == 11 || status == 12) {//司机开车中或已到达目的地
                        //1.获取服务商手机号
                        ServiceProvider serviceProvider = serviceProviderService.findById(serviceProviderId);
                        if (null == serviceProvider) {
                            logger.error("未找到对应的服务商信息 " + serviceProviderId);
                            throw new ObjectNotFoundException("未找到对应的服务商信息");
                        }
                        String serviceProviderPhone = serviceProvider.getPhone();
                        //2.发送验证短信
                        logger.info("向服务商发送收车验证码");
                        String verifyCode = getVerifyCode(orderId);
                        leanCloudUtils.sendVerifyCode(serviceProviderPhone, verifyCode);
                        flag.set(false);
                    }
                }
            } catch (InterruptedException | ObjectNotFoundException e) {
                logger.error(e.getMessage());
            }
        }
    }

    public void cancelConsumerOrder(String consumerOrderId) throws ObjectNotFoundException, NormalException {
        List<EOrder> eOrders = eOrderService.getEOrderIdByConsumerOrderId(consumerOrderId);
        if (eOrders.size() == 0) throw new ObjectNotFoundException("未找到对应的代驾订单");
        else {
            for (EOrder eOrder : eOrders) {
                cancelOrder(eOrder.getOrderId());
            }
        }
    }

    /**
     * 取消e代驾订单
     * 1.查询订单详情，status<7情况下用户可取消
     * 2.取消
     *
     * @param orderId
     */
    public void cancelOrder(Integer orderId) throws ObjectNotFoundException, NormalException {
        JSONObject jsonResult = orderDetail(orderId);
        if (jsonResult.get("code") != null && jsonResult.getInteger("code") == 0) {
            logger.info("成功获取订单：{}详情", jsonResult);
            Integer status = jsonResult.getJSONObject("data").getJSONObject("orderInfo").getInteger("status");
            if (status < 7) {
                String url = orderUrl + "cancel";
                JSONObject param = new JSONObject();
                param.put("orderId", orderId);
                param.put("channel", channel);
                HttpEntity entity = HttpRequest.getEntity(param);
                String result = HttpRequest.postCall2(url, HttpRequest.getEParam(param), entity, null);

                JSONObject cancelResult = JSONObject.parseObject(result);
                if (cancelResult.get("code") != null && cancelResult.getInteger("code") == 0) {
                    logger.info("成功取消代驾订单，订单号：{}", orderId);
                } else throw new NormalException("代驾订单取消失败");
            }
        } else throw new ObjectNotFoundException("代驾订单查询失败");
    }

    /**
     * 获取订单详情
     */
    public JSONObject orderDetail(Integer orderId) {
        String url = orderUrl + "detail";
        logger.info("向" + url + "发送订单详情请求");
        Map<String, Object> param = new HashedMap<>();
        param.put("orderId", orderId);
        param.put("channel", channel);
        String result = HttpRequest.getCall(url, HttpRequest.getEParam(param), null);
        JSONObject jsonResult = JSONObject.parseObject(result);
        logger.info("获取订单详情:{}", jsonResult);

        /*logger.info("成功获取订单：{}详情", orderId);
        JSONObject data = jsonResult.getJSONObject("data");
        JSONArray array = data.getJSONArray("drivingInfoList");
        for (Object driver :
                array) {
            JSONObject driverInfo = (JSONObject) driver;
            if (driverInfo.get("driverPhone") != null && !driverInfo.getString("driverPhone").equals("")) {
                driverInfo.getString("driverPhone");
                break;
            }
        }*/
        return jsonResult;
    }

    /**
     * 获取目的人收车验证码
     *
     * @param orderId
     * @return
     * @throws ObjectNotFoundException
     */
    public String getVerifyCode(Integer orderId) throws ObjectNotFoundException {
        String url = orderUrl + "verifyCode";
        logger.info("向" + url + "发送获取目的人收车验证码请求");
        Map<String, Object> param = new HashedMap<>();
        param.put("orderId", orderId);
        param.put("channel", channel);
        param.put("type", 1);
        String result = HttpRequest.getCall(url, HttpRequest.getEParam(param), null);
        JSONObject jsonResult = JSONObject.parseObject(result);
        logger.info("获取目的人收车验证码:{}", jsonResult);
        if (jsonResult.get("code") != null && jsonResult.getInteger("code") == 0) {
            return jsonResult.getString("data");
        } else throw new ObjectNotFoundException("未查询到对应验证码");

    }

    /**
     * 代驾司机取车，提醒用户订单已受理
     * @param sign
     * @param password
     * @param orderId
     * @return
     */
    public ResultJsonObject verifyCode(String sign, Integer password, Integer orderId) throws ObjectNotFoundException, InterruptedException, OpenArkDoorFailedException, OpenArkDoorTimeOutException, ArgumentMissingException {
        logger.info("执行代驾司机接车操作：---start---" + orderId);
        //1.验证司机的订单验证码是否正确
        Map<String, Object> param = new HashedMap<>();
        param.put("password", password);
        param.put("orderId", orderId);
        if (sign.equals(HttpRequest.getSign(param))) {
            EOrder eOrder = eOrderService.findByOrderId(orderId);
            if (eOrder != null && eOrder.getVerifyCode() != null && eOrder.getVerifyCode().equals(password)) {
                logger.info("司机输入验证码正确，准备打开柜门");
                String consumerOrderId = eOrder.getConsumerOrderId();
        //2.更新consumerOrder
                ConsumerOrder consumerOrder = consumerOrderRepository.findById(consumerOrderId).orElse(null);
                if (null == consumerOrder) {
                    return ResultJsonObject.getCustomResult("Not found consumerOrder object by orderId : " + orderId, ResultCode.RESULT_DATA_NONE);
                }
                String driverPhone = ""; //司机手机号
                String  driverNo = "";   //司机工号
                JSONObject orderDetail = this.orderDetail(orderId);
                if (orderDetail.get("code") != null && orderDetail.getInteger("code") == 0) {
                    logger.info("成功获取订单：{}详情", orderId);
                    //1.获取司机电话
                    JSONArray array = orderDetail.getJSONObject("data").getJSONArray("drivingInfoList");
                    for (Object driver :
                            array) {
                        JSONObject driverInfo = (JSONObject) driver;
                        if (driverInfo.get("driverPhone") != null && !driverInfo.getString("driverPhone").equals("")) {
                            driverPhone = driverInfo.getString("driverPhone");
                            driverNo = driverInfo.getString("driverNo");
                            break;
                        }
                    }
                }
                consumerOrder.setPickTime(new Timestamp(System.currentTimeMillis()));
                consumerOrder.setState(Constants.OrderState.RECEIVE_CAR.getValue());
                consumerOrder.setPickCarStaffId(driverNo);
                consumerOrder.setPickCarStaffName("e代驾"+driverPhone);
                ConsumerOrder orderRes = consumerOrderService.updateOrder(consumerOrder);

                // 回填服务技师的id和name
                List<ConsumerProjectInfo> consumerProjectInfos = consumerProjectInfoService.getAllProjectInfoByOrderId(consumerOrderId);
                for (ConsumerProjectInfo consumerProjectInfo : consumerProjectInfos) {
                    consumerProjectInfo.setStaffId(driverNo);
                    consumerProjectInfo.setStaffName("e代驾"+driverPhone);
                    consumerProjectInfoService.saveOrUpdate(consumerProjectInfo);
                }

    //3.更新door表数据状态
                Door door = doorRepository.findTopByOrderId(consumerOrderId);

                logger.info("arkOrderLog:智能柜柜门door信息：" + door);

                this.changeDoorState(door, null, Constants.DoorState.EMPTY.getValue());
    //4. 调用硬件接口方法打开柜门
                doorService.openDoorByDoorObject(door);

    //5.推送微信公众号消息，通知用户已开始受理服务
                consumerOrderService.sendWeChatMsg(orderRes);

                logger.info("执行代驾司机接车操作：---end---" + orderId);
                return ResultJsonObject.getDefaultResult(orderId);
            }return ResultJsonObject.getErrorResult(null,"开柜验证码错误");
        }
        return ResultJsonObject.getErrorResult(null, "签名错误");
    }

    private void changeDoorState(Door door, String orderId, int doorState) throws ArgumentMissingException {
        if (null == door) {
            throw new ArgumentMissingException("没找到分配的智能柜door表信息，无法更新状态。预约服务状态终止。");
        }
        door.setOrderId(orderId);
        door.setState(doorState);
        doorRepository.save(door);
    }

}
