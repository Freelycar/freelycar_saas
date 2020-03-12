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
import com.freelycar.saas.project.repository.ProjectRepository;
import com.freelycar.saas.util.cache.ConcurrentHashMapCacheUtils;
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
import java.util.concurrent.atomic.AtomicLong;

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
    private static final AtomicLong count = new AtomicLong(0);

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
    @Autowired
    private ProjectService projectService;
    @Autowired
    private ProjectRepository projectRepository;


    /**
     * 创建e代驾订单并向e代驾发送下单请求，成功返回e代驾订单id
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
        Map<String, Object> eParam = HttpRequest.getEParam(param);
        HttpEntity entity = HttpRequest.getEntity(eParam);
        Map<String, Object> head = new HashedMap<>();
        head.put("Content-Type", "application/x-www-form-urlencoded");
        String result = HttpRequest.postCall(url, entity, null);
        JSONObject jsonResult = JSONObject.parseObject(result);
        logger.info("下单结果：{}", jsonResult);
        if (jsonResult.getInteger("code") == 0) {//下单成功，查询司机详情（线程开启）
            Integer orderId = jsonResult.getInteger("data");
            eOrder.setConsumerOrderId(consumerOrder.getId());
            eOrder.setOrderId(orderId);
            eOrder.setDor(0);   //delivery
            eOrderRepository.saveAndFlush(eOrder);
//            logger.info("开始查询e代驾订单：{} 详情", orderId);
            return orderId;
        } else throw new NormalException("创建e代驾订单失败");
    }

    /**
     * 1.用户下单完成后，给取车送到服务商处的司机发送接车短信
     * 2.服务商完成服务，电话告知小易，向e代驾下单后，给还车到智能柜处的司机发送还车短信
     *
     * @param orderId
     */
    public void sendTemplate(Integer orderId) {
        logger.info("线程开始");
        AtomicBoolean flag = new AtomicBoolean(false);
        EOrder eOrder = eOrderService.findByOrderId(orderId);
        if (eOrder!= null) flag.set(true);
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
                         eOrderService.findByOrderId(orderId);
                        for (Object driver :
                                array) {
                            JSONObject driverInfo = (JSONObject) driver;
                            if (driverInfo.get("driverPhone") != null && !driverInfo.getString("driverPhone").equals("")) {
                                driverPhone = driverInfo.getString("driverPhone");
                                //2.给driverPhone发送链接
                                int password = leanCloudUtils.getPassword();

                                eOrder.setVerifyCode(password);
                                eOrderRepository.saveAndFlush(eOrder);

                                String consumerOrderId = eOrder.getConsumerOrderId();
                                String link = "e-dirve/verifyCode?sign=";
                                Map<String, Object> param = new HashedMap<>();
                                param.put("password", password);
                                param.put("orderId", orderId);
                                link = link + "" + HttpRequest.getSign(param)
                                        + "&orderId=" + orderId
                                        + "&consumerOrderId="+consumerOrderId;
                                leanCloudUtils.sendTemplate(driverPhone, password, link);
                                break;
                            }
                        }
                        flag.set(false);
                    } else {//测试使用
                        String driverPhone = "18206295380";
                        //2.给driverPhone发送链接
                        int password = leanCloudUtils.getPassword();
                        eOrder.setVerifyCode(password);
                        eOrderRepository.saveAndFlush(eOrder);
                        String consumerOrderId = eOrder.getConsumerOrderId();
                        String link = "e-dirve/verifyCode?sign=";
                        Map<String, Object> param = new HashedMap<>();
                        param.put("password", password);
                        param.put("orderId", orderId);
                        link = link + "" + HttpRequest.getSign(param)
                                + "&password=" + password
                                + "&orderId=" + orderId
                                + "&consumerOrderId="+consumerOrderId;
                        leanCloudUtils.sendTemplate(driverPhone, password, link);
                        flag.set(false);
                    }
                }
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * 司机取完钥匙后，给服务商发送收车验证码
     *
     * @param orderId e代驾订单id
     * @param serviceProviderId
     */
    public void sendVerifyCode(Integer orderId, String serviceProviderId) {
        logger.info("线程开始");
        AtomicBoolean flag = new AtomicBoolean(true);
        //用于测试
        AtomicLong count = new AtomicLong(0);
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
                //测试用
                if (count.get()==2){
                    //1.获取服务商手机号
                    ServiceProvider serviceProvider = serviceProviderService.findById(serviceProviderId);
                    if (null == serviceProvider) {
                        logger.error("未找到对应的服务商信息 " + serviceProviderId);
                        throw new ObjectNotFoundException("未找到对应的服务商信息");
                    }
                    String serviceProviderPhone = serviceProvider.getPhone();
                    //2.发送验证短信
                    logger.info("向服务商发送收车验证码");
//                    String verifyCode = getVerifyCode(orderId);
                    leanCloudUtils.sendVerifyCode(serviceProviderPhone, "123456");
                    flag.set(false);
                }
                count.incrementAndGet();
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
                Map<String, Object> eParam = HttpRequest.getEParam(param);
                logger.info("sign:" + eParam.get("sign"));
                String result = HttpRequest.postCall2(url, eParam, null, null);
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
     * @param orderId e代驾订单id
     * @return
     */
    public ResultJsonObject verifyCode(String sign, Integer password, Integer orderId) throws ObjectNotFoundException, InterruptedException, OpenArkDoorFailedException, OpenArkDoorTimeOutException, ArgumentMissingException, NoEmptyArkException {
        logger.info("执行代驾司机接车操作：---start---" + orderId);
        //1.验证司机的订单验证码是否正确
        Map<String, Object> param = new HashedMap<>();
        param.put("password", password);
        param.put("orderId", orderId);
        if (sign.equals(HttpRequest.getSign(param))) {
            logger.info("签名正确，准备校验验证码");
            EOrder eOrder = eOrderService.findByOrderId(orderId);
            logger.info(eOrder.toString());
            if (eOrder != null && eOrder.getVerifyCode() != null && eOrder.getVerifyCode().equals(password) && eOrder.getDor() == 0) {
                logger.info("代驾司机准备取车送到服务商处服务");
                logger.info("司机输入验证码正确，准备打开柜门");
                String consumerOrderId = eOrder.getConsumerOrderId();
                //2.更新consumerOrder
                ConsumerOrder consumerOrder = consumerOrderRepository.findById(consumerOrderId).orElse(null);
                if (null == consumerOrder) {
                    return ResultJsonObject.getCustomResult("Not found consumerOrder object by orderId : " + orderId, ResultCode.RESULT_DATA_NONE);
                }
                if (consumerOrder.getState().equals(Constants.OrderState.RESERVATION.getValue())){
                    String driverPhone = ""; //司机手机号
                    String driverNo = "";   //司机工号
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
                    consumerOrder.setPickCarStaffName("e代驾" + driverPhone);
                    ConsumerOrder orderRes = consumerOrderService.updateOrder(consumerOrder);

                    // 回填服务技师的id和name
                    List<ConsumerProjectInfo> consumerProjectInfos = consumerProjectInfoService.getAllProjectInfoByOrderId(consumerOrderId);
                    for (ConsumerProjectInfo consumerProjectInfo : consumerProjectInfos) {
                        consumerProjectInfo.setStaffId(driverNo);
                        consumerProjectInfo.setStaffName("e代驾" + driverPhone);
                        consumerProjectInfoService.saveOrUpdate(consumerProjectInfo);
                    }

                    //3.更新door表数据状态
                    Door door = doorRepository.findTopByOrderId(consumerOrderId);

                    logger.info("arkOrderLog:智能柜柜门door信息：" + door);
                    //更新柜门状态
                    this.changeDoorState(door, null, Constants.DoorState.EMPTY.getValue());
                    //4. 调用硬件接口方法打开柜门
                    doorService.openDoorByDoorObject(door);

                    //5.推送微信公众号消息，通知用户已开始受理服务
                    consumerOrderService.sendWeChatMsg(orderRes);

                    logger.info("执行代驾司机接车操作：---end---" + orderId);

                    //6.给服务商发送接车短信
                    Boolean flag = false;
                    List<ConsumerProjectInfo> projectInfos = consumerProjectInfoService.getAllProjectInfoByOrderId(consumerOrderId);
                    for (ConsumerProjectInfo info:projectInfos
                    ) {
                        Project project = projectRepository.findById(info.getProjectId()).orElse(null);
                        if (project!=null && project.getServiceProviderId()!=null){
                            sendVerifyCode(orderId,project.getServiceProviderId());
                            flag = true;
                            break;
                        }
                    }
                    if (flag){
                        return ResultJsonObject.getDefaultResult(orderId);
                    }else {
                        return ResultJsonObject.getErrorResult(null,"未找到相关服务商，请联系工作人员。");
                    }
                }else {
                    return ResultJsonObject.getErrorResult(null,"钥匙已取出，验证码无效");
                }
            }else {
                return ResultJsonObject.getErrorResult("代驾订单不存在");
            }
        }else {
            return ResultJsonObject.getErrorResult(null, "签名错误");
        }
    }


    /**
     * 代驾司机还车，提醒用户订单已受理
     * @param orderId e代驾订单id
     * @return
     */
    public ResultJsonObject verifyCodeAndOpenDoor(String sign, Integer password, Integer orderId,String doorId) throws ObjectNotFoundException, InterruptedException, OpenArkDoorFailedException, OpenArkDoorTimeOutException, ArgumentMissingException, NoEmptyArkException {
        logger.info("执行代驾司机接车操作：---start---" + orderId);
        //1.验证司机的订单验证码是否正确
        Map<String, Object> param = new HashedMap<>();
        param.put("password", password);
        param.put("orderId", orderId);
        if (sign.equals(HttpRequest.getSign(param))) {
            logger.info("签名正确，准备校验验证码");
            EOrder eOrder = eOrderService.findByOrderId(orderId);
            logger.info(eOrder.toString());
            if (eOrder != null && eOrder.getVerifyCode() != null && eOrder.getVerifyCode().equals(password) && eOrder.getDor() == 1) {
                logger.info("代驾司机准备将车钥匙还回到智能柜中");
                logger.info("司机输入验证码正确，准备打开柜门");

                String consumerOrderId = eOrder.getConsumerOrderId();
                ConsumerOrder consumerOrder = consumerOrderRepository.findById(consumerOrderId).orElse(null);
                if (null == consumerOrder) {
                    return ResultJsonObject.getCustomResult("Not found consumerOrder object by orderId : " + orderId, ResultCode.RESULT_DATA_NONE);
                }
                //1.获取arkSn
                if (consumerOrder.getUserKeyLocationSn() == null) {
                    return ResultJsonObject.getErrorResult("未找到对应智能柜");
                } else {
                    /*String userKeyLocationSn = consumerOrder.getUserKeyLocationSn();
                    String arkSn = userKeyLocationSn.split(Constants.HYPHEN)[0];*/
                    Door door = doorService.findById(doorId);
                    consumerOrder.setFinishTime(new Timestamp(System.currentTimeMillis()));
                    consumerOrder.setState(Constants.OrderState.SERVICE_FINISH.getValue());

                    // 有效柜子分配逻辑
                    Door emptyDoor = (Door) ConcurrentHashMapCacheUtils.getCache(door.getId());
                    if (null == emptyDoor) {
                        return ResultJsonObject.getErrorResult("未找到分配的柜门号，请稍后重试");
                    }

                    logger.info("arkOrderLog:智能柜柜门door信息：" + emptyDoor);

                    // 更新代驾司机把钥匙存放在哪个柜子的哪个门
                    String staffKeyLocation = emptyDoor.getArkName() + Constants.HYPHEN + emptyDoor.getDoorSn() + "号门";
                    String staffKeyLocationSn = emptyDoor.getArkSn() + Constants.HYPHEN + emptyDoor.getDoorSn();
                    consumerOrder.setStaffKeyLocation(staffKeyLocation);
                    consumerOrder.setStaffKeyLocationSn(staffKeyLocationSn);


                    ConsumerOrder order;
                    try {
                        order = consumerOrderService.updateOrder(consumerOrder);
                    } catch (ObjectNotFoundException e) {
                        ConcurrentHashMapCacheUtils.deleteCache(door.getId());
                        throw e;
                    }



                    // 更新door表数据状态
                    this.changeDoorState(emptyDoor, consumerOrderId, Constants.DoorState.STAFF_FINISH.getValue());
                    // 调用硬件接口方法打开柜门
                    try {
                        doorService.openDoorByDoorObject(emptyDoor);
                    } catch (OpenArkDoorFailedException | OpenArkDoorTimeOutException | InterruptedException e) {
                        throw e;
                    } finally {
                        ConcurrentHashMapCacheUtils.deleteCache(door.getId());
                    }
                    // 推送微信公众号消息，通知用户取车
                    consumerOrderService.sendWeChatMsg(order);
                    return ResultJsonObject.getDefaultResult(emptyDoor.getDoorSn());
                }
            }else {
                return ResultJsonObject.getErrorResult("代驾订单不存在");
            }
        }else {
            return ResultJsonObject.getErrorResult(null, "签名错误");
        }
    }

    private void changeDoorState(Door door, String orderId, int doorState) throws ArgumentMissingException {
        if (null == door) {
            throw new ArgumentMissingException("没找到分配的智能柜door表信息，无法更新状态。预约服务状态终止。");
        }
        door.setOrderId(orderId);
        door.setState(doorState);
        doorRepository.save(door);
    }

    /**
     * 服务商完成服务，电话告知
     * 再次下单e代驾，代驾司机取车还回车钥匙
     *
     * @param orderId consumerOrderId
     * @return
     */
    public ResultJsonObject confirmService(String orderId) throws ObjectNotFoundException, NormalException {
        logger.info("服务商完成服务，电话告知");
        ConsumerOrder consumerOrder = consumerOrderRepository.findById(orderId).orElse(null);
        if (consumerOrder == null) {
            throw new ObjectNotFoundException("未查询到对应的订单");
        }
        List<EOrder> eOrderList = eOrderRepository.findByConsumerOrderId(orderId);
        boolean flag = false;
        EOrder pickUpOrder = null;
        for (EOrder order : eOrderList) {
            if (order.getDor() != null && order.getDor() == 0) {
                flag = true;
                pickUpOrder = order;
                break;
            }
        }
        if (flag) {
            //1.向e代驾下单
            String url = orderUrl + "create";
            EOrder eOrder = eOrderService.create(pickUpOrder);
            eOrder.setConsumerOrderId(orderId);
            eOrder.setDor(1);    //还车
            logger.info("向" + url + "发送下单请求");
            JSONObject param = JSONObject.parseObject(JSONObject.toJSONString(eOrder));
            HttpEntity entity = HttpRequest.getEntity(param);
            String result = HttpRequest.postCall2(url, HttpRequest.getEParam(param), entity, null);
            JSONObject jsonResult = JSONObject.parseObject(result);
            logger.info("下单结果：{}", jsonResult);
            if (jsonResult.getInteger("code") == 0) {//下单成功，查询司机详情（线程开启）
                Integer eOrderId = jsonResult.getInteger("data");
                eOrder.setOrderId(eOrderId);
                eOrderRepository.saveAndFlush(eOrder);
                consumerOrder.setState(Constants.OrderState.TO_BE_RETURNED.getValue());
                consumerOrderRepository.saveAndFlush(consumerOrder);
                logger.info("开始查询e代驾订单：{} 详情", eOrderId);
                sendTemplate(eOrderId);
                return ResultJsonObject.getDefaultResult("下单成功");
            } else throw new NormalException("创建e代驾订单失败");
        } else {
            return ResultJsonObject.getErrorResult("e代驾还车订单下单失败");
        }
    }
}
