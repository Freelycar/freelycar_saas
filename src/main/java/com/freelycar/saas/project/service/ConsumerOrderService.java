package com.freelycar.saas.project.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.basic.wrapper.*;
import com.freelycar.saas.exception.*;
import com.freelycar.saas.project.entity.*;
import com.freelycar.saas.project.model.*;
import com.freelycar.saas.project.repository.*;
import com.freelycar.saas.util.*;
import com.freelycar.saas.util.cache.ConcurrentHashMapCacheUtils;
import com.freelycar.saas.wechat.model.BaseOrderInfo;
import com.freelycar.saas.wechat.model.FinishOrderInfo;
import com.freelycar.saas.wechat.model.OrderChangedMessage;
import com.freelycar.saas.wechat.model.ReservationOrderInfo;
import com.freelycar.saas.wxutils.MiniMessage;
import com.freelycar.saas.wxutils.MiniProgramUtils;
import com.freelycar.saas.wxutils.WechatTemplateMessage;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author tangwei - Toby
 * @date 2018-12-28
 * @email toby911115@gmail.com
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ConsumerOrderService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private LocalContainerEntityManagerFactoryBean entityManagerFactory;

    @Autowired
    private ConsumerOrderRepository consumerOrderRepository;

    @Autowired
    private ConsumerProjectInfoService consumerProjectInfoService;

    @Autowired
    private AutoPartsService autoPartsService;

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CardService cardService;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private CarService carService;

    @Autowired
    private OrderIDGenerator orderIDGenerator;

    @Autowired
    private StaffService staffService;

    @Autowired
    private WxUserInfoService wxUserInfoService;

    @Autowired
    private DoorRepository doorRepository;

    @Autowired
    private DoorService doorService;

    @Autowired
    private CardServiceRepository cardServiceRepository;

    @Autowired
    private CouponServiceRepository couponServiceRepository;

    @Autowired
    private ClientOrderImgRepository clientOrderImgRepository;

    @Autowired
    private StaffOrderImgRepository staffOrderImgRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectTypeRepository projectTypeRepository;

    @Autowired
    private EdaijiaService edaijiaService;

    @Autowired
    private EOrderRepository eOrderRepository;

    @Autowired
    private ArkService arkService;

    @Autowired
    private RSPProjectRepository rspProjectRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RealServiceProviderRepository realServiceProviderRepository;

    @Autowired
    private ReminderRepository reminderRepository;

    @Autowired
    private MiniProgramUtils miniProgramUtils;

    public JSONObject getDoorState(String orderId) {
        JSONObject jsonObject = new JSONObject();
        boolean isBuffer = false;
        Integer doorSn = null;
        boolean isUser = false;
        ConsumerOrder consumerOrder = consumerOrderRepository.findById(orderId).orElse(null);
        if (null != consumerOrder
                && consumerOrder.getDelStatus() == Constants.DelStatus.NORMAL.isValue()) {
            Door door = doorRepository.findTopByOrderId(orderId);
            if (null != door) {
                doorSn = door.getDoorSn();
                if ((consumerOrder.getState().equals(Constants.OrderState.RESERVATION.getValue())
                        && door.getState().equals(Constants.DoorState.EMPTY.getValue()))
                        && !door.getOrderId().isEmpty()
                ) {
                    isBuffer = true;
                    isUser = true;
                }
                if (consumerOrder.getState().equals(Constants.OrderState.RECEIVE_CAR.getValue())
                        && door.getState().equals(Constants.DoorState.PER_STAFF_FINISH.getValue())
                        && !door.getOrderId().isEmpty()
                ) {
                    isBuffer = true;
                }
            }

        }
        jsonObject.put("isBuffer", isBuffer);
        jsonObject.put("doorSn", doorSn);
        jsonObject.put("isUser", isUser);
        return jsonObject;
    }

    /**
     * 保存和修改
     *
     * @param consumerOrder
     * @return
     */
    public ConsumerOrder saveOrUpdate(ConsumerOrder consumerOrder) throws ObjectNotFoundException, ArgumentMissingException {
        if (null == consumerOrder) {
            throw new ArgumentMissingException("参数consumerOrder对象为空");
        }
        String id = consumerOrder.getId();
        if (StringUtils.isEmpty(id)) {
            //订单号生成规则：订单类型编号（1位）+ 门店（3位）+ 日期（6位）+ 每日递增（4位）
            String newId;
            try {
                newId = orderIDGenerator.getOrderSn(consumerOrder.getStoreId(), consumerOrder.getOrderType());
            } catch (ArgumentMissingException | NumberOutOfRangeException | NormalException e) {
                e.printStackTrace();
                throw new ObjectNotFoundException("生成订单编号失败：" + e.getMessage());
            }
            consumerOrder.setId(newId);
            consumerOrder.setDelStatus(Constants.DelStatus.NORMAL.isValue());
            consumerOrder.setCreateTime(new Timestamp(System.currentTimeMillis()));
            return consumerOrderRepository.saveAndFlush(consumerOrder);
        }
        return this.updateOrder(consumerOrder);
    }

    /**
     * excel导入
     *
     * @param consumerOrder
     * @return
     * @throws ObjectNotFoundException
     * @throws ArgumentMissingException
     */
    public ConsumerOrder createOrder(ConsumerOrder consumerOrder) throws ObjectNotFoundException, ArgumentMissingException {
        if (null == consumerOrder) {
            throw new ArgumentMissingException("参数consumerOrder对象为空");
        }
        String id = consumerOrder.getId();
        if (StringUtils.isEmpty(id)) {
            //订单号生成规则：订单类型编号（1位）+ 门店（3位）+ 日期（6位）+ 每日递增（4位）
            String newId;
            try {
                newId = orderIDGenerator.getOrderSn(consumerOrder.getStoreId(), consumerOrder.getOrderType(), consumerOrder.getCreateTime());
            } catch (ArgumentMissingException | NumberOutOfRangeException | NormalException e) {
                e.printStackTrace();
                throw new ObjectNotFoundException("生成订单编号失败：" + e.getMessage());
            }
            consumerOrder.setId(newId);
            consumerOrder.setDelStatus(Constants.DelStatus.NORMAL.isValue());
            return consumerOrderRepository.saveAndFlush(consumerOrder);
        }
        return this.updateOrder(consumerOrder);
    }

    /**
     * 更新order信息
     *
     * @param consumerOrder
     * @return
     */
    public ConsumerOrder updateOrder(ConsumerOrder consumerOrder) throws ObjectNotFoundException {
        String id = consumerOrder.getId();
        Optional<ConsumerOrder> consumerOrderOptional = consumerOrderRepository.findById(id);
        if (!consumerOrderOptional.isPresent()) {
            throw new ObjectNotFoundException("更新订单失败：未找到id为：" + id + " 的订单");
        }
        ConsumerOrder source = consumerOrderOptional.get();
        UpdateTool.copyNullProperties(source, consumerOrder);
        return consumerOrderRepository.saveAndFlush(consumerOrder);
    }


    /**
     * 快速开单
     *
     * @param orderObject
     * @return
     */
    public ResultJsonObject handleOrder(OrderObject orderObject) {
        ConsumerOrder consumerOrder = orderObject.getConsumerOrder();
        List<ConsumerProjectInfo> consumerProjectInfos = orderObject.getConsumerProjectInfos();
        List<AutoParts> autoParts = orderObject.getAutoParts();

        //设置order的额外信息
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        consumerOrder.setOrderType(Constants.OrderType.SERVICE.getValue());
        consumerOrder.setPayState(Constants.PayState.NOT_PAY.getValue());
        //快速开单时订单状态直接为接车状态（不需要预约）
        consumerOrder.setState(Constants.OrderState.RECEIVE_CAR.getValue());
        consumerOrder.setPickTime(currentTime);
        //应付金额等于整单金额
        consumerOrder.setActualPrice(consumerOrder.getTotalPrice());

        ConsumerOrder consumerOrderRes;
        try {
            consumerOrderRes = this.saveOrUpdate(consumerOrder);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, "开单失败！" + e.getMessage());
        }
        if (null == consumerOrderRes) {
            return ResultJsonObject.getErrorResult(null, "开单失败！保存订单信息失败。如有疑问，请联系管理员！");
        }

        String orderId = consumerOrder.getId();

        //保存订单项目信息
        if (null != consumerProjectInfos && !consumerProjectInfos.isEmpty()) {
            for (ConsumerProjectInfo consumerProjectInfo : consumerProjectInfos) {
                consumerProjectInfo.setConsumerOrderId(orderId);
                consumerProjectInfoService.saveOrUpdate(consumerProjectInfo);
            }
        }

        //保存项目相关配件
        if (null != autoParts && !autoParts.isEmpty()) {
            for (AutoParts autoPart : autoParts) {
                autoPart.setConsumerOrderId(orderId);
                autoPartsService.saveOrUpdate(autoPart);
            }
        }

        return ResultJsonObject.getDefaultResult(consumerOrderRes.getId(), "订单生成成功！");
    }

    /**
     * 根据clientId查找所有有效订单
     *
     * @param clientId
     * @return
     */
    public List<ConsumerOrder> findAllEffectiveOrdersByClientId(String clientId) {
        return consumerOrderRepository.findAllByClientIdAndDelStatusOrderByCreateTimeDesc(clientId, Constants.DelStatus.NORMAL.isValue());
    }

    /**
     * 查询某人的某类类型的所有订单
     *
     * @param clientId
     * @param type
     * @return
     */
    public List<ConsumerOrder> findAllOrdersByTypeAndClientId(String clientId, String type) {
        if (Constants.OrderType.SERVICE.getName().equalsIgnoreCase(type)) {
            return consumerOrderRepository.findAllByClientIdAndDelStatusAndOrderTypeOrderByCreateTimeDesc(clientId, Constants.DelStatus.NORMAL.isValue(), Constants.OrderType.SERVICE.getValue());
        }
        if (Constants.OrderType.ARK.getName().equalsIgnoreCase(type)) {
            return consumerOrderRepository.findAllByClientIdAndDelStatusAndOrderTypeOrderByCreateTimeDesc(clientId, Constants.DelStatus.NORMAL.isValue(), Constants.OrderType.ARK.getValue());
        }
        if (Constants.OrderType.CARD.getName().equalsIgnoreCase(type)) {
            return consumerOrderRepository.findAllByClientIdAndDelStatusAndOrderTypeOrderByCreateTimeDesc(clientId, Constants.DelStatus.NORMAL.isValue(), Constants.OrderType.CARD.getValue());
        }
        return null;
    }

    public List<BaseOrderInfo> findAllOrdersByClientId(String clientId) {
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT co.id, co.licensePlate AS licensePlate, co.parkingLocation,co.userKeyLocation,co.comment,co.staffKeyLocation,co.carBrand AS carBrand, co.carType AS carType, co.clientName AS clientName, " +
                "( SELECT GROUP_CONCAT( cpi.projectName ) FROM consumerProjectInfo cpi WHERE cpi.consumerOrderId = co.id AND cpi.delStatus=0 GROUP BY cpi.consumerOrderId ) AS projectNames," +
                " (SELECT GROUP_CONCAT(rsp.phone) FROM realserviceprovider rsp WHERE rsp.id IN (SELECT p.rspId FROM rspproject p WHERE p.id IN (SELECT cpi.projectId FROM consumerProjectInfo cpi WHERE cpi.consumerOrderId = co.id AND cpi.delStatus=0)) GROUP BY rsp.name) AS rspPhone," +
                "(SELECT GROUP_CONCAT(rsp.name) FROM realserviceprovider rsp WHERE rsp.id IN (SELECT p.rspId FROM rspproject p WHERE p.id IN (SELECT cpi.projectId FROM consumerProjectInfo cpi WHERE cpi.consumerOrderId = co.id AND cpi.delStatus=0)) GROUP BY rsp.name) AS rspName," +
                "co.createTime AS createTime, co.pickTime AS pickTime, co.finishTime AS finishTime, co.state, co.actualPrice as actualPrice, co.totalPrice as totalPrice, co.payState AS payState, ( select GROUP_CONCAT(url) from stafforderimg soi where soi.orderId = co.id and soi.delStatus = 0) as staffOrderImgUrl," +
                "( select GROUP_CONCAT(url) from clientorderimg coi where coi.orderId = co.id and coi.delStatus = 0) as clientOrderImgUrl" +
                " FROM consumerOrder co WHERE co.delStatus = 0 ");
        sql.append("AND co.clientId = '").append(clientId).append("' ORDER BY co.state ASC ,co.payState ASC,co.createTime desc");

        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());
        nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(BaseOrderInfo.class));
        @SuppressWarnings({"unused", "unchecked"})
        List<BaseOrderInfo> baseOrderInfos = nativeQuery.getResultList();

        //关闭entityManagerFactory
        em.close();
        return baseOrderInfos;
    }

    /**
     * 首页订单查询接口
     *
     * @param clientId   客户id
     * @param employeeId 技师id
     * @param name       订单id或车牌号
     * @return
     */
    public List<QueryOrder> listOrdersByIdAndName(String clientId, String employeeId, String name) throws ObjectNotFoundException {
        StringBuilder sql = new StringBuilder();
        if (StringUtils.hasText(clientId)) {
            sql.append("SELECT id,state,licensePlate,createTime," +
                    "(SELECT GROUP_CONCAT(cpi.projectName) FROM consumerprojectinfo cpi WHERE cpi.consumerOrderId = co.id ) AS projectNames\n" +
                    "FROM `consumerorder` co  WHERE delStatus = FALSE and state <4 ");
            sql.append(" AND clientId = '").append(clientId).append("' ");
            sql.append(" AND (licensePlate LIKE '%").append(name).append("%' ");
            sql.append(" or id LIKE '%").append(name).append("%') ");
            sql.append(" ORDER BY createTime desc");
        }
        if (StringUtils.hasText(employeeId)) {
            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            if (null == employee) {
                throw new ObjectNotFoundException("未找到id为：" + employeeId + " 的技师信息");
            }
            String phone = employee.getPhone();
            List<Staff> staffList = staffService.findByPhone(phone);
            String staffIdStr = "";
            if (staffList.size() > 0) {
                StringBuilder staffIdsb = new StringBuilder();
                for (Staff staff :
                        staffList) {
                    staffIdsb.append(",").append("'").append(staff.getId()).append("'");
                }
                staffIdStr = staffIdsb.toString();
                staffIdStr = staffIdStr.substring(1);
            }
            sql.append("SELECT id,state,licensePlate,createTime," +
                    "(SELECT GROUP_CONCAT(cpi.projectName) FROM consumerprojectinfo cpi WHERE cpi.consumerOrderId = co.id ) AS projectNames\n" +
                    "FROM `consumerorder` co  WHERE delStatus = FALSE and state<4 ");
            sql.append(" AND pickCarStaffId IN (").append(staffIdStr).append(")");
            sql.append(" AND (licensePlate LIKE '%").append(name).append("%' ");
            sql.append(" or id LIKE '%").append(name).append("%') ");
            sql.append(" ORDER BY createTime desc");
        }

        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());
        nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(QueryOrder.class));
        @SuppressWarnings({"unused", "unchecked"})
        List<QueryOrder> queryOrderList = nativeQuery.getResultList();

        //关闭entityManagerFactory
        em.close();
        return queryOrderList;
    }

    public List<BaseOrderInfo> findAllOrdersByClientId(String clientId, String type) throws IllegalArgumentException {
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT co.id, co.licensePlate AS licensePlate, co.carBrand AS carBrand, co.carType AS carType, co.clientName AS clientName, ( SELECT GROUP_CONCAT( cpi.projectName ) FROM consumerProjectInfo cpi WHERE cpi.consumerOrderId = co.id AND cpi.delStatus=0 GROUP BY cpi.consumerOrderId ) AS projectNames, co.createTime AS createTime, co.pickTime AS pickTime, co.finishTime AS finishTime, co.state, co.actualPrice as actualPrice, co.totalPrice as totalPrice, co.payState AS payState, ( select url from stafforderimg soi where soi.orderId = co.id and soi.delStatus = 0 order by soi.createTime desc limit 0,1) as staffOrderImgUrl FROM consumerOrder co WHERE co.delStatus = 0 ");
        if (Constants.OrderType.SERVICE.getName().equalsIgnoreCase(type)) {
            sql.append(" AND co.orderType = ").append(Constants.OrderType.SERVICE.getValue());
        } else if (Constants.OrderType.ARK.getName().equalsIgnoreCase(type)) {
            sql.append(" AND co.orderType = ").append(Constants.OrderType.ARK.getValue());
        } else if (Constants.OrderType.CARD.getName().equalsIgnoreCase(type)) {
            sql.append(" AND co.orderType = ").append(Constants.OrderType.CARD.getValue());
        } else if (Constants.OrderType.RECHARGE.getName().equalsIgnoreCase(type)) {
            sql.append(" AND co.orderType = ").append(Constants.OrderType.RECHARGE.getValue());
        } else if ("nocard".equalsIgnoreCase(type)) {
            sql.append(" AND co.orderType < ").append(Constants.OrderType.CARD.getValue());
        } else if ("all".equalsIgnoreCase(type)) {
            sql.append(" AND co.orderType < 10 ");
        } else {
            throw new IllegalArgumentException("不支持参数type值为：" + type + " 的查询");
        }

        sql.append(" AND co.clientId = '").append(clientId).append("' ORDER BY co.createTime DESC ");

        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());
        nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(BaseOrderInfo.class));
        @SuppressWarnings({"unused", "unchecked"})
        List<BaseOrderInfo> baseOrderInfos = nativeQuery.getResultList();

        //关闭entityManagerFactory
        em.close();

        return baseOrderInfos;

    }

    /**
     * 查询可接车的智能柜预约
     *
     * @param licensePlate
     * @return
     */
    public List<ReservationOrderInfo> listReservationOrders(String licensePlate, String storeId, String staffId) throws ArgumentMissingException, ObjectNotFoundException {
        if (StringUtils.isEmpty(staffId)) {
            throw new ArgumentMissingException("参数staffId为空");
        }
        List<ReservationOrderInfo> reservationOrderInfos = null;
        try {
            StringBuilder sql = new StringBuilder();
            sql.append(" SELECT co.id,co.comment, co.licensePlate as licensePlate, co.carBrand as carBrand, co.carType as carType, co.carColor, co.carImageUrl, co.clientName AS clientName,(select c.phone from client c where c.id = co.clientId) as phone, ( SELECT GROUP_CONCAT( cpi.projectName ) FROM consumerProjectInfo cpi WHERE cpi.consumerOrderId = co.id GROUP BY cpi.consumerOrderId ) AS projectNames, co.createTime AS createTime, co.parkingLocation AS parkingLocation, d.arkSn AS arkSn, d.doorSn AS doorSn, concat( ( SELECT ark.`name` FROM ark WHERE ark.id = d.arkId ), '-', d.doorSn, '号门' ) AS keyLocation FROM door d LEFT JOIN consumerOrder co ON co.id = d.orderId WHERE co.state = 0 ")
                    .append(" AND co.storeId = '").append(storeId).append("' ");
            if (StringUtils.hasText(licensePlate)) {
                sql.append(" and (co.licensePlate like '%").append(licensePlate.toUpperCase()).append("%' or co.id like '%").append(licensePlate.toUpperCase()).append("%') ");
            }
            sql.append(" ORDER BY co.createTime ASC");

            EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
            Query nativeQuery = em.createNativeQuery(sql.toString());
            nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(ReservationOrderInfo.class));
            reservationOrderInfos = nativeQuery.getResultList();
            for (ReservationOrderInfo orderInfo :
                    reservationOrderInfos) {
                String orderId = orderInfo.getId();
                ClientOrderImg img = clientOrderImgRepository.findTopByOrderIdAndDelStatusOrderByCreateTimeDesc(orderId, false);
                if (img != null) {
                    orderInfo.setCarImageUrl(img.getUrl());
                }
            }
            //关闭em
            em.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return reservationOrderFilter(reservationOrderInfos, staffId);
    }

    /**
     * 技师的待服务订单
     * 预约订单-0
     *
     * @param licensePlate 车牌号
     * @param employeeId   技师id
     * @return
     */
    public List<ReservationOrderInfo> listReservationOrders(String licensePlate, String employeeId) throws ArgumentMissingException, ObjectNotFoundException {
        if (StringUtils.isEmpty(employeeId)) {
            throw new ArgumentMissingException("参数employeeId为空");
        }
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (null == employee) {
            throw new ObjectNotFoundException("未找到id为：" + employeeId + " 的技师信息");
        }
        List<ReservationOrderInfo> reservationOrderInfos = null;
        if (employee.getNotification()) {
            String phone = employee.getPhone();//手机作为技师的唯一标识
            List<Staff> staffList = staffService.findByPhoneAndIsArk(phone, true);
            List<Store> storeList = new ArrayList<>();
            for (Staff staff :
                    staffList) {
                String staffId = staff.getId();
                List<Store> stores = staffService.findServicingStoreByStaffId(staffId);
                storeList.addAll(stores);
            }
            Set<String> storeIds = new HashSet<>();
            for (Store store :
                    storeList) {
                storeIds.add(store.getId());
            }
            if (storeIds.size() > 0) {
                StringBuilder storeIdsb = new StringBuilder();
                for (String storeId :
                        storeIds) {
                    storeIdsb.append(",").append("'").append(storeId).append("'");
                }
                String storeIdStr = storeIdsb.toString();
                storeIdStr = storeIdStr.substring(1);
                try {
                    StringBuilder sql = new StringBuilder();
                    sql.append(" SELECT co.id,co.comment, co.licensePlate as licensePlate, co.carBrand as carBrand, " +
                            "co.carType as carType, co.carColor, co.carImageUrl, co.clientName AS clientName," +
                            "(select c.phone from client c where c.id = co.clientId) as phone, " +
                            "( SELECT GROUP_CONCAT( cpi.projectName ) FROM consumerProjectInfo cpi " +
                            "WHERE cpi.consumerOrderId = co.id GROUP BY cpi.consumerOrderId ) AS projectNames, " +
                            "(SELECT GROUP_CONCAT(rsp.name) FROM realserviceprovider rsp WHERE rsp.id IN (SELECT p.rspId FROM rspproject p WHERE p.id IN (SELECT cpi.projectId FROM consumerProjectInfo cpi WHERE cpi.consumerOrderId = co.id AND cpi.delStatus=0)) GROUP BY rsp.name) AS rspName," +
                            "co.createTime AS createTime, co.parkingLocation AS parkingLocation, d.arkSn AS arkSn, " +
                            "d.doorSn AS doorSn, concat( ( SELECT ark.`name` FROM ark WHERE ark.id = d.arkId ), '-', d.doorSn, '号门' ) AS keyLocation " +
                            "FROM door d LEFT JOIN consumerOrder co ON co.id = d.orderId WHERE co.state = 0 and co.delStatus = false ")
                            .append(" AND co.storeId in (").append(storeIdStr).append(") ");
                    if (StringUtils.hasText(licensePlate)) {
                        sql.append(" and (co.licensePlate like '%").append(licensePlate.toUpperCase()).append("%' or co.id like '%").append(licensePlate.toUpperCase()).append("%') ");
                    }
                    sql.append(" ORDER BY co.createTime ASC");

                    EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
                    Query nativeQuery = em.createNativeQuery(sql.toString());
                    nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(ReservationOrderInfo.class));
                    reservationOrderInfos = nativeQuery.getResultList();
                    for (ReservationOrderInfo orderInfo :
                            reservationOrderInfos) {
                        String orderId = orderInfo.getId();
//                    ClientOrderImg img = clientOrderImgRepository.findTopByOrderIdAndDelStatusOrderByCreateTimeDesc(orderId, false);
                        List<ClientOrderImg> imgs = clientOrderImgRepository.findByOrderIdAndDelStatusOrderByCreateTimeDesc(orderId, false);
                        StringBuilder img = new StringBuilder("");
                        for (ClientOrderImg clientOrderImg :
                                imgs) {
                            img.append(",").append(clientOrderImg.getUrl());
                        }
//                    logger.info(img.toString());
                        if (imgs.size() > 0) {
                            orderInfo.setCarImageUrl(img.substring(1));
                        }

                    }
                    //关闭em
                    em.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return reservationOrderInfos;
    }

    public List<ReservationOrderInfo> reservationOrderFilter(List<ReservationOrderInfo> reservationOrderInfos, String staffId) throws ObjectNotFoundException {
        Staff staff = staffService.findById(staffId);
        if (null == staff) {
            throw new ObjectNotFoundException("未找到id为：" + staffId + " 的技师信息");
        }

        List<ReservationOrderInfo> resultList = new ArrayList<>();

        List<String> projectIds = new ArrayList<>();
        List<Project> projects = staff.getProjects();
        for (Project project : projects) {
            projectIds.add(project.getId());
        }

        for (ReservationOrderInfo reservationOrderInfo : reservationOrderInfos) {
            boolean orderShow = false;
            String orderId = reservationOrderInfo.getId();
            List<ConsumerProjectInfo> consumerProjectInfos = consumerProjectInfoService.getAllProjectInfoByOrderId(orderId);
            for (ConsumerProjectInfo consumerProjectInfo : consumerProjectInfos) {
                if (projectIds.contains(consumerProjectInfo.getProjectId())) {
                    orderShow = true;
                    break;
                }
            }
            if (orderShow) {
                resultList.add(reservationOrderInfo);
            }
        }

        return resultList;
    }


    /**
     * 查询可完工的智能柜订单
     *
     * @param licensePlate
     * @param storeId
     * @return
     */
    public List<FinishOrderInfo> listServicingOrders(String licensePlate, String storeId, String staffId) {
        List<FinishOrderInfo> finishOrderInfo = null;
        try {
            StringBuilder sql = new StringBuilder();
            sql.append(" SELECT co.id,co.state,co.orderTakingTime, co.parkingLocation,co.clientName AS clientName,(select c.phone from client c where c.id = co.clientId) as phone, co.licensePlate as licensePlate, co.carBrand as carBrand, co.carType as carType, co.carColor, co.carImageUrl, ( SELECT GROUP_CONCAT( cpi.projectName ) FROM consumerProjectInfo cpi WHERE cpi.consumerOrderId = co.id GROUP BY cpi.consumerOrderId ) projectNames, co.pickTime as pickTime, co.userKeyLocationSn, co.userKeyLocation FROM consumerOrder co WHERE co.delStatus = 0 AND co.orderType = 2 AND (co.state = -1 or co.state = 1) ")
                    .append(" AND co.storeId = '").append(storeId).append("' ")
                    // 添加staffId条件筛选，技师只能还自己接单的订单，不能其他技师代还
                    .append(" AND co.pickCarStaffId = '").append(staffId).append("' ");
            if (StringUtils.hasText(licensePlate)) {
                sql.append(" and (co.licensePlate like '%").append(licensePlate).append("%' or co.id like '%").append(licensePlate).append("%') ");
            }
            sql.append(" ORDER BY co.pickTime ASC ");
            EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
            Query nativeQuery = em.createNativeQuery(sql.toString());
            nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(FinishOrderInfo.class));
            finishOrderInfo = nativeQuery.getResultList();

            //关闭em
            em.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


        return finishOrderInfo;
    }

    /**
     * 查询技师服务中订单
     *
     * @param licensePlate
     * @param employeeId
     * @return
     */
    public List<FinishOrderInfo> listServicingOrders(String licensePlate, String employeeId) throws ObjectNotFoundException, ArgumentMissingException {
        List<FinishOrderInfo> finishOrderInfo = null;
        if (StringUtils.isEmpty(employeeId)) {
            throw new ArgumentMissingException("参数employeeId为空");
        }
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (null == employee) {
            throw new ObjectNotFoundException("未找到id为：" + employeeId + " 的技师信息");
        }
        String phone = employee.getPhone();//手机作为技师的唯一标识
        List<Staff> staffList = staffService.findByPhone(phone);
        List<Store> storeList = new ArrayList<>();
        Set<String> staffIds = new HashSet<>();
        for (Staff staff :
                staffList) {
            String staffId = staff.getId();
            staffIds.add(staffId);
        }
        if (staffIds.size() > 0) {
            StringBuilder staffIdSb = new StringBuilder();
            for (String staffId :
                    staffIds) {
                staffIdSb.append(",").append("'").append(staffId).append("'");
            }
            String staffStr = staffIdSb.toString();
            staffStr = staffStr.substring(1);
            StringBuilder sql = new StringBuilder();
            sql.append(" SELECT co.id,co.comment,co.state,co.orderTakingTime, co.parkingLocation,co.clientName AS clientName,(select c.phone from client c where c.id = co.clientId) as phone, co.licensePlate as licensePlate, co.carBrand as carBrand, co.carType as carType, co.carColor, co.carImageUrl, ( SELECT GROUP_CONCAT( cpi.projectName ) FROM consumerProjectInfo cpi WHERE cpi.consumerOrderId = co.id GROUP BY cpi.consumerOrderId ) projectNames, " +
                    "(SELECT GROUP_CONCAT(rsp.name) FROM realserviceprovider rsp WHERE rsp.id IN (SELECT p.rspId FROM rspproject p WHERE p.id IN (SELECT cpi.projectId FROM consumerProjectInfo cpi WHERE cpi.consumerOrderId = co.id AND cpi.delStatus=0)) GROUP BY rsp.name) AS rspName," +
                    "co.pickTime as pickTime, co.userKeyLocationSn, co.userKeyLocation FROM consumerOrder co WHERE co.delStatus = 0 AND co.orderType = 2 AND (co.state = -1 or co.state = 1) ")
                    // 添加staffId条件筛选，技师只能还自己接单的订单，不能其他技师代还
                    .append(" AND co.pickCarStaffId in (").append(staffStr).append(") ");
            if (StringUtils.hasText(licensePlate)) {
                sql.append(" and (co.licensePlate like '%").append(licensePlate).append("%' or co.id like '%").append(licensePlate).append("%') ");
            }
            sql.append(" ORDER BY co.pickTime ASC ");
            EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
            Query nativeQuery = em.createNativeQuery(sql.toString());
            nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(FinishOrderInfo.class));
            finishOrderInfo = nativeQuery.getResultList();

            //关闭em
            em.close();

        }
        for (FinishOrderInfo info :
                finishOrderInfo) {
            String orderId = info.getId();
            List<ClientOrderImg> imgs = clientOrderImgRepository.findByOrderIdAndDelStatusOrderByCreateTimeDesc(orderId, Constants.DelStatus.NORMAL.isValue());
            if (imgs.size() > 0) {
                StringBuilder imgSb = new StringBuilder("");
                for (ClientOrderImg img :
                        imgs) {
                    imgSb.append(",").append(img.getUrl());
                }
                info.setClientOrderImgUrl(imgSb.substring(1));
            }
        }
        for (FinishOrderInfo info :
                finishOrderInfo) {
            String orderId = info.getId();
            Door door = doorRepository.findTopByOrderId(orderId);
            if (null != door
                    && door.getState() == Constants.DoorState.PER_STAFF_FINISH.getValue()) {
                info.setIsBuffer(true);
            }
        }
        return finishOrderInfo;
    }

    public String getRspNameByConsumerOrderId(String id) {
        String rspName = "";
        List<ConsumerProjectInfo> projectInfoList = consumerProjectInfoService.getAllProjectInfoByOrderId(id);
        if (projectInfoList != null && projectInfoList.size() > 0) {
            ConsumerProjectInfo projectInfo = projectInfoList.get(0);
            String projectId = projectInfo.getProjectId();
            RSPProject project = rspProjectRepository.findByIdAndDelStatus(projectId, Constants.DelStatus.NORMAL.isValue()).orElse(null);
            if (null != project) {
                RealServiceProvider realServiceProvider = realServiceProviderRepository.findByIdAndDelStatus(project.getRspId(),
                        Constants.DelStatus.NORMAL.isValue()).orElse(null);
                if (null != realServiceProvider) {
                    rspName = realServiceProvider.getName();
                }
            }
        }
        return rspName;
    }

    /**
     * 获取订单详情的数据（用于详情页展示和结算中心展示）
     *
     * @param id
     * @return
     */
    public ResultJsonObject getOrderObjectDetail(String id) {
        if (StringUtils.isEmpty(id)) {
            return ResultJsonObject.getErrorResult(null, "参数id为NULL");
        }

        OrderObject orderObject = new OrderObject();

        Optional<ConsumerOrder> optionalConsumerOrder = consumerOrderRepository.findById(id);
        if (!optionalConsumerOrder.isPresent()) {
            return ResultJsonObject.getErrorResult(null, "未找到id为：" + id + " 的订单数据");
        }

        ConsumerOrder consumerOrder = optionalConsumerOrder.get();

        //获取服务项目
        List<ConsumerProjectInfo> consumerProjectInfos = consumerProjectInfoService.getAllProjectInfoByOrderId(id);
        if (consumerProjectInfos.size() > 0) {
            ConsumerProjectInfo projectInfo = consumerProjectInfos.get(0);
            String projectId = projectInfo.getProjectId();
            RSPProject rspProject = rspProjectRepository.findById(projectId).orElse(null);
            if (null != rspProject) {
                RealServiceProvider realServiceProvider = realServiceProviderRepository.findById(rspProject.getRspId()).orElse(null);
                if (null != realServiceProvider) {
                    orderObject.setRspName(realServiceProvider.getName());
                    orderObject.setRspPhone(realServiceProvider.getPhone());
                }
            }
        }
        //获取配件
//        List<AutoParts> autoPartsList = autoPartsService.getAllAutoPartsByOrderId(id);
        //获取用户上传的智能柜订单图片
//        ClientOrderImg clientOrderImg = clientOrderImgRepository.findTopByOrderIdAndDelStatusOrderByCreateTimeDesc(id, Constants.DelStatus.NORMAL.isValue());
        List<ClientOrderImg> clientOrderImgs = clientOrderImgRepository.findByOrderIdAndDelStatusOrderByCreateTimeDesc(id, Constants.DelStatus.NORMAL.isValue());
        //获取技师上传的智能柜订单图片
//        StaffOrderImg staffOrderImg = staffOrderImgRepository.findTopByOrderIdAndDelStatusOrderByCreateTimeDesc(id, Constants.DelStatus.NORMAL.isValue());
        List<StaffOrderImg> staffOrderImgs = staffOrderImgRepository.findByOrderIdAndDelStatusOrderByCreateTimeDesc(id, Constants.DelStatus.NORMAL.isValue());

        //获取相关的卡信息或券信息
        /*if (Constants.OrderIdSn.CARD.getName().equals(id.substring(0, 1))) {
            String cardOrCouponId = consumerOrder.getCardOrCouponId();
            if (StringUtils.hasText(cardOrCouponId)) {
                Card card = cardRepository.findById(cardOrCouponId).orElse(null);
                Coupon coupon = couponRepository.findById(cardOrCouponId).orElse(null);
                if (null != card) {
                    orderObject.setCard(card);
                }
                if (null != coupon) {
                    orderObject.setCoupon(coupon);
                }
            }
        }*/
        String arkSn = consumerOrder.getUserKeyLocationSn();
        if (arkSn != null && arkSn.length() > 15) {
            arkSn = arkSn.substring(0, 15);
            Ark ark = arkService.findByArkSn(arkSn);
            orderObject.setArk(ark);
        }
        orderObject.setConsumerOrder(consumerOrder);
        orderObject.setConsumerProjectInfos(consumerProjectInfos);
//        orderObject.setAutoParts(autoPartsList);

        /*if (null != clientOrderImg) {
            orderObject.setClientOrderImg(clientOrderImg);
        }*/

        if (null != clientOrderImgs) {
            orderObject.setClientOrderImgs(clientOrderImgs);
        }

        /*if (null != staffOrderImg) {
            orderObject.setStaffOrderImg(staffOrderImg);
        }*/

        if (null != staffOrderImgs) {
            orderObject.setStaffOrderImgs(staffOrderImgs);
        }

        //查询接单的店员信息（包含头像、姓名、联系电话）
        String staffId = consumerOrder.getPickCarStaffId();
        if (StringUtils.hasText(staffId)) {
            orderObject.setStaffInfo(staffService.findStaffInfoForOrderByStaffId(staffId));
        }

        //返回订单所属门店消息
        String storeId = consumerOrder.getStoreId();
        if (StringUtils.hasText(storeId)) {
            Store store = storeRepository.findById(storeId).orElse(null);
            orderObject.setStore(store);
        }
//        orderObject.setRspName(getRspNameByConsumerOrderId(id));
        return ResultJsonObject.getDefaultResult(orderObject);
    }

    /**
     * 结算（需要处理卡券抵扣等业务逻辑）
     *
     * @param payOrder
     * @return
     */
    public ResultJsonObject payment(PayOrder payOrder) throws ObjectNotFoundException {
        ConsumerOrder consumerOrder = payOrder.getConsumerOrder();
        if (null == consumerOrder) {
            return ResultJsonObject.getErrorResult(null, "consumerOrder对象为NULL！");
        }

        String orderId = consumerOrder.getId();

        consumerOrder.setPayState(Constants.PayState.FINISH_PAY.getValue());



        /*
        处理抵用券的结算
         */
        //查询所有关联的抵用券，并将所有orderId和status初始化
        couponService.initCouponByOrderId(orderId);

        //将抵用券设置为已使用
        List<Coupon> coupons = payOrder.getUseCoupons();
        for (Coupon coupon : coupons) {
            coupon.setOrderId(orderId);
            coupon.setStatus(Constants.CouponStatus.BEEN_USED.getValue());
            couponService.saveOrUpdate(coupon);
        }

        /*
        结算方式一
         */
        Integer firstPayMethod = consumerOrder.getFirstPayMethod();
        Double firstActualPrice = consumerOrder.getFirstActualPrice();
        String firstCardId = consumerOrder.getFirstCardId();

        //如果支付方式为空，不结算支付方式一
        if (null != firstPayMethod) {
            //如果支付一是卡支付，进行余额扣除
            if (Constants.PayMethod.CARD.getCode().intValue() == firstPayMethod) {
                cardService.cardSettlement(firstCardId, firstActualPrice.floatValue());
            }
            //如果是其他支付方式，直接保存即可
        }


        /*
        结算方式二
         */
        Integer secondPayMethod = consumerOrder.getSecondPayMethod();
        Double secondActualPrice = consumerOrder.getSecondActualPrice();
        String secondCardId = consumerOrder.getSecondCardId();

        //如果支付方式为空，不结算支付方式一
        if (null != secondPayMethod) {
            //如果支付一是卡支付，进行余额扣除
            if (Constants.PayMethod.CARD.getCode().intValue() == secondPayMethod) {
                cardService.cardSettlement(secondCardId, secondActualPrice.floatValue());
            }
            //如果是其他支付方式，直接保存即可
        }

        //判断实付总金额是否为两种支付方式的总和
        if (!this.isTotalActualPriceRight(consumerOrder)) {
            consumerOrder.setActualPrice(this.sumActualPrice(consumerOrder));
        }

        //保存订单信息（结算）

        this.updateOrder(consumerOrder);

        // 处理其他逻辑，比如推送消息和消费金额叠加之类的
        clientService.updateClientAcount(consumerOrder.getClientId(), consumerOrder.getActualPrice());

        return ResultJsonObject.getDefaultResult(consumerOrder.getId(), "结算成功");
    }

    /**
     * 挂单（本质是保存，但是需要考虑抵用券选用的情况）
     *
     * @param payOrder
     * @return
     */
    public ResultJsonObject pendingOrder(PayOrder payOrder) throws ObjectNotFoundException {
        ConsumerOrder consumerOrder = payOrder.getConsumerOrder();
        if (null == consumerOrder) {
            return ResultJsonObject.getErrorResult(null, "consumerOrder对象为NULL！");
        }

        String orderId = consumerOrder.getId();

        consumerOrder = this.updateOrder(consumerOrder);

        //查询所有关联的抵用券，并将所有orderId和status初始化
        couponService.initCouponByOrderId(orderId);

        //将抵用券设置为挂单
        List<Coupon> coupons = payOrder.getUseCoupons();
        for (Coupon coupon : coupons) {
            coupon.setOrderId(orderId);
            coupon.setStatus(Constants.CouponStatus.KEEP.getValue());
            couponService.saveOrUpdate(coupon);
        }

        return ResultJsonObject.getDefaultResult(orderId);
    }

    /**
     * 判断总实付金额是否等于两种支付方式总和
     *
     * @param consumerOrder
     * @return
     */
    private boolean isTotalActualPriceRight(ConsumerOrder consumerOrder) {
        Double firstActualPrice = consumerOrder.getFirstActualPrice() == null ? 0 : consumerOrder.getFirstActualPrice();
        Double secondActualPrice = consumerOrder.getSecondActualPrice() == null ? 0 : consumerOrder.getSecondActualPrice();
        Double actualPrice = consumerOrder.getActualPrice();
        return null != actualPrice && firstActualPrice + secondActualPrice == actualPrice;
    }

    /**
     * 计算实付金额
     *
     * @param consumerOrder
     * @return
     */
    private double sumActualPrice(ConsumerOrder consumerOrder) {
        Double firstActualPrice = consumerOrder.getFirstActualPrice() == null ? 0 : consumerOrder.getFirstActualPrice();
        Double secondActualPrice = consumerOrder.getSecondActualPrice() == null ? 0 : consumerOrder.getSecondActualPrice();
        return firstActualPrice + secondActualPrice;
    }


    /**
     * 单据列表条件查询（作废）
     *
     * @param params
     * @return
     */
    @Deprecated
    public ResultJsonObject list(String storeId, Integer currentPage, Integer pageSize, OrderListParam params) {
        String orderId = params.getOrderId();
        String licensePlate = params.getLicensePlate();

        // 如何查出订单类型，还得考虑一下
        String projectId = params.getProjectId();

        Integer orderState = params.getOrderState();
        Integer orderType = params.getOrderType();
        Integer payState = params.getPayState();

        Integer dateType = params.getDateType();
        String startTime = params.getStartTime();
        String endTime = params.getEndTime();

        Page<ConsumerOrder> resultPage;
        Specification<ConsumerOrder> querySpecification = (Specification<ConsumerOrder>) (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("delStatus"), 0));
            predicates.add(criteriaBuilder.equal(root.get("storeId"), storeId));

            //订单号模糊查询
            if (StringUtils.hasText(orderId)) {
                predicates.add(criteriaBuilder.like(root.get("id"), "%" + orderId + "%"));
            }
            //车牌号模糊查询
            if (StringUtils.hasText(licensePlate)) {
                predicates.add(criteriaBuilder.like(root.get("licensePlate"), "%" + licensePlate + "%"));
            }
            //订单状态条件查询
            if (null != orderState) {
                predicates.add(criteriaBuilder.equal(root.get("state"), orderState));
            }
            //订单类型条件查询（如果没传，默认是查询出非办卡类的）
            if (null != orderType) {
                predicates.add(criteriaBuilder.equal(root.get("orderType"), orderType));
            } else {
                predicates.add(criteriaBuilder.notEqual(root.get("orderType"), Constants.OrderType.CARD.getValue()));
            }
            //支付状态条件查询
            if (null != payState) {
                predicates.add(criteriaBuilder.equal(root.get("payState"), payState));
            }
            //时间范围条件查询
            if (null != dateType) {
                Date start = null;
                Date end = null;
                if (StringUtils.hasText(startTime)) {
                    try {
                        start = DateUtils.parseDate(startTime + " 00:00:00", "yyyy-MM-dd HH:mm:ss");
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
                if (StringUtils.hasText(endTime)) {
                    try {
                        end = DateUtils.parseDate(endTime + " 23:59:59", "yyyy-MM-dd HH:mm:ss");
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                if (Constants.DateType.ORDER.getValue().intValue() == dateType) {
                    if (null != start) {
                        predicates.add(criteriaBuilder.greaterThan(root.get("createTime"), start));
                    }
                    if (null != end) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createTime"), end));
                    }
                }

                if (Constants.DateType.PICK.getValue().intValue() == dateType) {
                    if (null != start) {
                        predicates.add(criteriaBuilder.greaterThan(root.get("pickTime"), start));
                    }
                    if (null != end) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("pickTime"), end));
                    }
                }

                if (Constants.DateType.FINISH.getValue().intValue() == dateType) {
                    if (null != start) {
                        predicates.add(criteriaBuilder.greaterThan(root.get("finishTime"), start));
                    }
                    if (null != end) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("finishTime"), end));
                    }
                }

                if (Constants.DateType.DELIVER.getValue().intValue() == dateType) {
                    if (null != start) {
                        predicates.add(criteriaBuilder.greaterThan(root.get("deliverTime"), start));
                    }
                    if (null != end) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("deliverTime"), end));
                    }
                }
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        resultPage = consumerOrderRepository.findAll(querySpecification, PageableTools.basicPage(currentPage, pageSize));
        return ResultJsonObject.getDefaultResult(PaginationRJO.of(resultPage));
    }

    /**
     * 单据列表条件查询
     * （替代原方法）
     *
     * @param storeId
     * @param currentPage
     * @param pageSize
     * @param params
     * @return
     */
    public ResultJsonObject listSql(String storeId, Integer currentPage, Integer pageSize, OrderListParam params, boolean export) {
        String orderId = params.getOrderId();
        String licensePlate = params.getLicensePlate();

        String projectId = params.getProjectId();

        Integer orderState = params.getOrderState();
        Integer orderType = params.getOrderType();
        Integer payState = params.getPayState();

        Integer dateType = params.getDateType();
        String startTime = params.getStartTime();
        String endTime = params.getEndTime();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT co.id AS id, co.createTime,co.licensePlate,co.orderType,co.totalPrice,co.actualPrice,co.state,co.payState,co.phone, group_concat(pt.name) AS project,co.parkingLocation,co.pickTime,co.finishTime,co.deliverTime FROM consumerorder co JOIN consumerprojectinfo cpi ON cpi.consumerOrderId = co.id LEFT JOIN project p ON p.id = cpi.projectId LEFT JOIN projectType pt on pt.id=p.projectTypeId WHERE co.delStatus = 0 ");
        sql.append(" AND co.storeId = '").append(storeId).append("' ");

        //订单号模糊查询
        if (StringUtils.hasText(orderId)) {
            sql.append(" AND co.id like '%").append(orderId).append("%' ");
        }
        //车牌号模糊查询
        if (StringUtils.hasText(licensePlate)) {
            sql.append(" AND co.licensePlate like '%").append(licensePlate).append("%' ");
        }
        //项目类型查询
        if (StringUtils.hasText(projectId)) {
            sql.append(" AND p.projectTypeId = '").append(projectId).append("' ");
        }

        //订单状态条件查询
        if (null != orderState) {
            sql.append(" AND co.state = ").append(orderState).append(" ");
        }
        //订单类型条件查询（如果没传，默认是查询出非办卡类的）
        if (null != orderType) {
            sql.append(" AND co.orderType=").append(orderType).append(" ");
        } else {
            sql.append(" AND co.orderType !=").append(Constants.OrderType.CARD.getValue()).append(" ");
        }
        //支付状态条件查询
        if (null != payState) {
            sql.append(" AND co.payState=").append(payState).append(" ");
        }
        //时间范围条件查询
        if (null != dateType) {
            String start = null;
            String end = null;

            if (StringUtils.hasText(startTime)) {
                start = startTime + " 00:00:00";
            }
            if (StringUtils.hasText(endTime)) {
                end = endTime + " 23:59:59";
            }

            if (Constants.DateType.ORDER.getValue().intValue() == dateType) {
                if (StringUtils.hasText(start)) {
                    sql.append(" AND co.createTime >= '").append(start).append("' ");
                }
                if (StringUtils.hasText(end)) {
                    sql.append(" AND co.createTime <= '").append(end).append("' ");
                }
            }

            if (Constants.DateType.PICK.getValue().intValue() == dateType) {
                if (StringUtils.hasText(start)) {
                    sql.append(" AND co.pickTime >= '").append(start).append("' ");
                }
                if (StringUtils.hasText(end)) {
                    sql.append(" AND co.pickTime <= '").append(end).append("' ");
                }
            }

            if (Constants.DateType.FINISH.getValue().intValue() == dateType) {
                if (StringUtils.hasText(start)) {
                    sql.append(" AND co.finishTime >= '").append(start).append("' ");
                }
                if (StringUtils.hasText(end)) {
                    sql.append(" AND co.finishTime <= '").append(end).append("' ");
                }
            }

            if (Constants.DateType.DELIVER.getValue().intValue() == dateType) {
                if (StringUtils.hasText(start)) {
                    sql.append(" AND co.deliverTime >= '").append(start).append("' ");
                }
                if (StringUtils.hasText(end)) {
                    sql.append(" AND co.deliverTime <= '").append(end).append("' ");
                }
            }
        }
        sql.append(" GROUP BY co.id ORDER BY co.createTime DESC");

        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());
        nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(CustomerOrderListObject.class));

        // 如果是导出EXCEL方法，就不分页
        if (export) {
            @SuppressWarnings({"unused", "unchecked"})
            List<CustomerOrderListObject> customerOrderListObjects = nativeQuery.getResultList();

            //关闭em
            em.close();

            return ResultJsonObject.getDefaultResult(customerOrderListObjects);
        } else {
            Pageable pageable = PageableTools.basicPage(currentPage, pageSize);
            int total = nativeQuery.getResultList().size();
            @SuppressWarnings({"unused", "unchecked"})
            List<CustomerOrderListObject> customerOrderListObjects = nativeQuery.setFirstResult(MySQLPageTool.getStartPosition(currentPage, pageSize)).setMaxResults(pageSize).getResultList();

            //关闭em
            em.close();
            @SuppressWarnings("unchecked")
            Page<CustomerOrderListObject> page = new PageImpl(customerOrderListObjects, pageable, total);


            return ResultJsonObject.getDefaultResult(PaginationRJO.of(page));
        }
    }


    /**
     * 完工
     *
     * @param consumerOrder
     * @return
     */
    public ResultJsonObject serviceFinish(ConsumerOrder consumerOrder) throws ObjectNotFoundException {
        if (StringUtils.isEmpty(consumerOrder)) {
            return ResultJsonObject.getErrorResult(consumerOrder, "参数consumerOrder为NULL，完工操作失败");
        }
        String orderId = consumerOrder.getId();
        Timestamp finishTime = consumerOrder.getFinishTime() == null ? new Timestamp(System.currentTimeMillis()) : consumerOrder.getFinishTime();
        ConsumerOrder source = consumerOrderRepository.getOne(orderId);
        source.setFinishTime(finishTime);
        source.setParkingLocation(consumerOrder.getParkingLocation());
        source.setState(Constants.OrderState.SERVICE_FINISH.getValue());

        this.updateOrder(source);
        return ResultJsonObject.getDefaultResult(orderId);
    }


    /**
     * 交车
     *
     * @param consumerOrder
     * @return
     */
    public ResultJsonObject handOver(ConsumerOrder consumerOrder) throws ObjectNotFoundException {
        if (StringUtils.isEmpty(consumerOrder)) {
            return ResultJsonObject.getErrorResult(consumerOrder, "参数consumerOrder为NULL，交车操作失败");
        }
        String orderId = consumerOrder.getId();
        Timestamp deliverTime = consumerOrder.getDeliverTime() == null ? new Timestamp(System.currentTimeMillis()) : consumerOrder.getDeliverTime();
        ConsumerOrder source = consumerOrderRepository.getOne(orderId);
        source.setDeliverTime(deliverTime);
        source.setState(Constants.OrderState.HAND_OVER.getValue());

        this.updateOrder(source);
        return ResultJsonObject.getDefaultResult(orderId);
    }


    /**
     * 获取当前用户，当前活跃的智能柜订单
     *
     * @param clientId
     * @return
     */
    public ResultJsonObject getActiveOrder(String clientId) {
        if (StringUtils.isEmpty(clientId)) {
            return ResultJsonObject.getErrorResult(clientId, "参数clientId为空值");
        }
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT co.id, co.licensePlate AS licensePlate, co.carBrand AS carBrand, co.carType AS carType, co.clientName AS clientName, ( SELECT GROUP_CONCAT( cpi.projectName ) FROM consumerProjectInfo cpi WHERE cpi.consumerOrderId = co.id GROUP BY cpi.consumerOrderId ) AS projectNames, co.createTime AS createTime, co.pickTime AS pickTime, co.finishTime AS finishTime, co.state, co.actualPrice as actualPrice, co.totalPrice as totalPrice FROM consumerOrder co WHERE co.orderType=2 AND co.delStatus = 0 AND co.state < 3 ")
                .append(" AND co.clientId = '").append(clientId).append("' ORDER BY co.createTime DESC ");

        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());
        nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(BaseOrderInfo.class));
        @SuppressWarnings({"unused", "unchecked"})
        List<BaseOrderInfo> activeArkOrderInfos = nativeQuery.getResultList();

        //关闭em
        em.close();

        if (null != activeArkOrderInfos && !activeArkOrderInfos.isEmpty()) {
            return ResultJsonObject.getDefaultResult(activeArkOrderInfos.get(0));
        }
        return ResultJsonObject.getDefaultResult(null);
    }


    /**
     * 智能柜开单
     *
     * @param orderObject
     * @return
     */
    public ResultJsonObject arkHandleOrder(OrderObject orderObject) throws ArgumentMissingException, ObjectNotFoundException, UpdateDataErrorException, NormalException, DoorInUseException, OpenArkDoorFailedException {
        logger.info("执行智能柜开单操作：---start---");
        String doorId = orderObject.getDoorId();
        Door door = doorRepository.findById(doorId).orElse(null);
        if (null == door) {
            throw new ObjectNotFoundException("未找到分配的柜门，请稍后重试");
        }
        if (door.getState() != Constants.DoorState.EMPTY.getValue()
                || !(null == door.getOrderId() || door.getOrderId().isEmpty())) {
            throw new DoorInUseException("柜门占用中,请稍后重试");
        }
        //获取提交过来的数据：
        //订单信息
        ConsumerOrder consumerOrder = orderObject.getConsumerOrder();

        logger.info("arkOrderLog:前端提交过来的柜门id：" + doorId);
        logger.info("arkOrderLog:前端提交过来的订单表单数据对象：" + consumerOrder);
        //订单项目
        List<ConsumerProjectInfo> consumerProjectInfos = orderObject.getConsumerProjectInfos();
        logger.info("arkOrderLog:前端提交过来的项目数：" + consumerProjectInfos.size());

        if (StringUtils.isEmpty(doorId)) {
            throw new ArgumentMissingException("参数中的doorId对象为空");
        }

        if (null == consumerOrder) {
            throw new ArgumentMissingException("参数中的consumerOrder对象为空");
        }

        String carId = consumerOrder.getCarId();
        String clientId = consumerOrder.getClientId();


        //获取车辆信息
        Car carInfo = carService.findById(carId);
        if (null == carInfo) {
            logger.error("未找到对应的车辆信息 " + carId);
            throw new ObjectNotFoundException("未找到对应的车辆信息");
        }

        logger.info("arkOrderLog:车辆carInfo：" + carInfo);

        //获取客户信息
        Client clientInfo = clientService.findById(clientId);
        if (null == clientInfo) {
            logger.error("未找到对应的车主信息 " + clientId);
            throw new ObjectNotFoundException("未找到对应的车主信息");
        }

        logger.info("arkOrderLog:用户clientInfo：" + clientInfo);

        //设置order中的车辆信息
        consumerOrder.setCarId(carId);
        consumerOrder.setLicensePlate(carInfo.getLicensePlate());
        consumerOrder.setCarBrand(carInfo.getCarBrand());
        consumerOrder.setCarType(carInfo.getCarType());
        consumerOrder.setLastMiles(carInfo.getLastMiles());
        consumerOrder.setMiles(carInfo.getMiles());
        //新增车辆颜色和图片url
        consumerOrder.setCarColor(carInfo.getColor());
        consumerOrder.setCarImageUrl(carInfo.getCarImageUrl());

        logger.info("arkOrderLog：车辆信息完善");

        //设置车主信息
        consumerOrder.setClientId(clientId);
        consumerOrder.setClientName(clientInfo.getTrueName());
        consumerOrder.setPhone(clientInfo.getPhone());
        consumerOrder.setIsMember(clientInfo.getMember());
        consumerOrder.setStoreId(clientInfo.getStoreId());
        logger.info("arkOrderLog：车主信息完善");

        //设置order的其他信息
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        consumerOrder.setOrderType(Constants.OrderType.ARK.getValue());
        consumerOrder.setPayState(Constants.PayState.NOT_PAY.getValue());
        //设置订单状态为“预约”
        consumerOrder.setState(Constants.OrderState.RESERVATION.getValue());
        logger.info("arkOrderLog：订单状态修改");

        //计算项目金额
        double totalPrice = consumerProjectInfoService.sumAllProjectPrice(consumerProjectInfos, false);
        consumerOrder.setTotalPrice(totalPrice);
        consumerOrder.setActualPrice(totalPrice);
        //计算会员价总计金额
        double memberPrice = consumerProjectInfoService.sumAllProjectPrice(consumerProjectInfos, true);
        consumerOrder.setMemberPrice(memberPrice);
        logger.info("arkOrderLog：订单价格计算完成");


        // 有效柜子分配逻辑
        Door emptyDoor = (Door) ConcurrentHashMapCacheUtils.getCache(doorId);
        if (null == emptyDoor) {
            throw new ObjectNotFoundException("未找到分配的柜门号，请稍后重试");
        }

        logger.info("arkOrderLog:智能柜柜门door信息");
        if (emptyDoor != null) {
            logger.info("arkOrderLog：" + emptyDoor);
        }
        // 更新用户把钥匙存放在哪个柜子的哪个门
        String userKeyLocation = emptyDoor.getArkName() + Constants.HYPHEN + emptyDoor.getDoorSn() + "号门";
        String userKeyLocationSn = emptyDoor.getArkSn() + Constants.HYPHEN + emptyDoor.getDoorSn();
        consumerOrder.setUserKeyLocation(userKeyLocation);
        consumerOrder.setUserKeyLocationSn(userKeyLocationSn);

        ConsumerOrder consumerOrderRes;
        try {
            consumerOrderRes = this.saveOrUpdate(consumerOrder);
        } catch (ArgumentMissingException | ObjectNotFoundException e) {
            ConcurrentHashMapCacheUtils.deleteCache(doorId);
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            throw new UpdateDataErrorException("保存订单信息失败");
        }

        String orderId = consumerOrder.getId();

        String rspId = null;
        //保存订单项目信息
        if (null != consumerProjectInfos && !consumerProjectInfos.isEmpty()) {
            for (ConsumerProjectInfo consumerProjectInfo : consumerProjectInfos) {
                //服务商id
                if (StringUtils.isEmpty(rspId)) {
                    String rspProjectId = consumerProjectInfo.getProjectId();
                    Optional<RSPProject> projectOptional = rspProjectRepository.findByIdAndDelStatus(rspProjectId, Constants.DelStatus.NORMAL.isValue());
                    if (projectOptional.isPresent()) {
                        rspId = projectOptional.get().getRspId();
                    }
                }
                consumerProjectInfo.setConsumerOrderId(orderId);
                consumerProjectInfoService.saveOrUpdate(consumerProjectInfo);
            }
        }

        //关联用户上传的图片
        List<ClientOrderImg> clientOrderImgs = orderObject.getClientOrderImgs();
        if (null != clientOrderImgs) {
            for (ClientOrderImg img :
                    clientOrderImgs) {
                img.setCreateTime(currentTime);
                img.setOrderId(orderId);
                img.setDelStatus(false);
            }
            clientOrderImgRepository.saveAll(clientOrderImgs);
        }
        //判断客户所选服务项目是否为代驾项目
        boolean orderType = false;
        String serviceProviderId = null;
        assert consumerProjectInfos != null;
        for (ConsumerProjectInfo project :
                Objects.requireNonNull(consumerProjectInfos)) {
            String projectId = project.getProjectId();
            Optional<Project> projectOptional = projectRepository.findById(projectId);
            if (projectOptional.isPresent()) {
                Optional<ProjectType> projectTypeOptional = projectTypeRepository.findById(projectOptional.get().getProjectTypeId());
                if (projectTypeOptional.isPresent() && projectTypeOptional.get().getName().trim().contains("代驾")) {
                    orderType = true;
                    serviceProviderId = projectOptional.get().getServiceProviderId();
                    break;
                }
            }
        }
        //开门，放钥匙
        //door表数据更新，更新柜门所属订单号
        this.changeDoorState(emptyDoor, orderId, Constants.DoorState.EMPTY.getValue());
        // 数据保存完毕之后操作硬件：
        // 1.打开柜门指令失败抛出异常，事务回滚
        // 2.打开柜门指令成功，关门超时，删除订单
        try {
            doorService.openDoorByDoorObjectForBeginAndEnd(emptyDoor, orderType, serviceProviderId, rspId);
        } catch (OpenArkDoorFailedException e) {
            logger.error("开门失败，失败原因：{}", e.getMessage());
            throw e;
        }
        //更新door表的数据状态
        this.changeDoorState(emptyDoor, orderId, Constants.DoorState.EMPTY.getValue());
        logger.info("执行智能柜开单操作---end---：" + orderId);
        return ResultJsonObject.getDefaultResult(consumerOrderRes.getId(), "订单生成成功！");
    }

    /**
     * 设置订单为取消状态
     *
     * @param orderId
     * @return
     */
    public ResultJsonObject cancelOrder(String orderId) throws ArgumentMissingException, OpenArkDoorFailedException, OpenArkDoorTimeOutException, InterruptedException, ObjectNotFoundException, NormalException {
        logger.info("执行用户取消订单操作：---start---" + orderId);
        ConsumerOrder consumerOrder = consumerOrderRepository.findById(orderId).orElse(null);
        if (null == consumerOrder) {
            return ResultJsonObject.getErrorResult(null, "未找到id为：" + orderId + " 的订单");
        }
        consumerOrder.setState(Constants.OrderState.CANCEL.getValue());
        ConsumerOrder consumerOrderRes = this.updateOrder(consumerOrder);

        //获取订单对应的柜子信息
        Door door = doorRepository.findTopByOrderId(orderId);

        logger.info("arkOrderLog:智能柜柜门door信息：" + door);

        //更新door表数据
        this.changeDoorState(door, null, Constants.DoorState.EMPTY.getValue());
        //打开柜门
        doorService.openDoorByDoorObject(door);

        ConcurrentHashMapCacheUtils.deleteCache(door.getId());

        //判断客户所选服务项目是否为代驾项目
        List<ConsumerProjectInfo> projectInfos = consumerProjectInfoService.getAllProjectInfoByOrderId(orderId);
        boolean orderType = false;
//        String serviceProviderId = null;
        for (ConsumerProjectInfo project :
                projectInfos) {
            String projectId = project.getProjectId();
            logger.info("projectId:" + projectId);
            Optional<Project> projectOptional = projectRepository.findById(projectId);
            if (projectOptional.isPresent()) {
                logger.info("projectTypeId:" + projectOptional.get().getProjectTypeId());
                Optional<ProjectType> projectTypeOptional = projectTypeRepository.findById(projectOptional.get().getProjectTypeId());
                if (projectTypeOptional.isPresent() && projectTypeOptional.get().getName().trim().contains("代驾")) {
                    orderType = true;
//                    serviceProviderId = projectOptional.get().getServiceProviderId();
                    break;
                }
                /*else {
                    throw new ObjectNotFoundException("未找到对应的项目类型信息");
                }*/
            }
            /*else {
                throw new ObjectNotFoundException("未找到对应的项目信息");
            }*/
        }
        String rspId = null;
        for (ConsumerProjectInfo project :
                projectInfos) {
            if (StringUtils.isEmpty(rspId)) {
                String rspProjectId = project.getProjectId();
                Optional<RSPProject> rspProjectOptional = rspProjectRepository.findByIdAndDelStatus(rspProjectId, Constants.DelStatus.NORMAL.isValue());
                if (rspProjectOptional.isPresent()) {
                    rspId = rspProjectOptional.get().getRspId();
                }
            } else {
                break;
            }
        }
        if (orderType) {
            edaijiaService.cancelConsumerOrder(orderId);
        } else {
            //用户取消服务订单的时候推送消息给技师
            staffService.sendWeChatMessageToStaff(consumerOrderRes, door, null, rspId);
        }
        logger.info("执行用户取消订单操作：---end---" + orderId);
        return ResultJsonObject.getDefaultResult(orderId);
    }


    /**
     * 根据车牌号和门店ID查询客户开单时所需的信息
     *
     * @param licensePlate
     * @param storeId
     * @return
     */
    public ResultJsonObject loadClientInfoByLicensePlate(String licensePlate, String storeId) {
        //查找车辆
        Car car = carService.findCarByLicensePlateAndStoreId(licensePlate, storeId);
        if (null == car) {
            return ResultJsonObject.getErrorResult(null, "未找到对应车辆信息");
        }
        String clientId = car.getClientId();
        if (StringUtils.isEmpty(clientId)) {
            return ResultJsonObject.getErrorResult(null, "未找到该车牌绑定的客户信息");
        }
        Client client = (Client) clientService.getDetail(clientId).getData();
        if (null == client) {
            return ResultJsonObject.getErrorResult(null, "未找到该车牌绑定的客户信息");
        }
        if (client.getDelStatus()) {
            return ResultJsonObject.getErrorResult(null, "未找到该车牌绑定的客户信息");
        }

        double cardBalance;
        Float balance = cardRepository.sumBalanceByClientId(clientId);
        if (null != balance) {
            //格式化精度
            cardBalance = RoundTool.round(balance.doubleValue(), 2, BigDecimal.ROUND_HALF_UP);
        } else {
            cardBalance = 0.0;
        }

        double consumeAmount = client.getConsumeAmount() == null ? 0.0 : client.getConsumeAmount();

        OrderClientInfo orderClientInfo = new OrderClientInfo();

        orderClientInfo.setCarId(car.getId());
        orderClientInfo.setLicensePlate(car.getLicensePlate());
        orderClientInfo.setCarBrand(car.getCarBrand());
        orderClientInfo.setCarType(car.getCarType());
        orderClientInfo.setStoreId(car.getStoreId());
        orderClientInfo.setLastMiles(String.valueOf(car.getLastMiles()));

        orderClientInfo.setClientId(client.getId());
        orderClientInfo.setClientName(client.getName());
        orderClientInfo.setPhone(client.getPhone());
        orderClientInfo.setIsMember(client.getMember());
        orderClientInfo.setHistoryConsumption(RoundTool.round(consumeAmount, 2, BigDecimal.ROUND_HALF_UP));

        orderClientInfo.setBalance(cardBalance);


        return ResultJsonObject.getDefaultResult(orderClientInfo);
    }

    /**
     * 用户开柜取车
     *
     * @param orderId
     * @return
     */
    public ResultJsonObject orderFinish(String orderId) throws Exception {
        logger.info("执行用户开柜取车操作：---start---" + orderId);
        if (StringUtils.isEmpty(orderId)) {
            return ResultJsonObject.getCustomResult(orderId, ResultCode.PARAM_NOT_COMPLETE);
        }
        ConsumerOrder consumerOrder = consumerOrderRepository.findById(orderId).orElse(null);
        if (null == consumerOrder) {
            return ResultJsonObject.getCustomResult(orderId, ResultCode.RESULT_DATA_NONE);
        }
        //设置单据状态为"已取车（交车）"
        consumerOrder.setState(Constants.OrderState.HAND_OVER.getValue());
        consumerOrder.setDeliverTime(new Timestamp(System.currentTimeMillis()));

        ConsumerOrder res = this.updateOrder(consumerOrder);


        //获取订单对应的柜子信息
        Door door = doorRepository.findTopByOrderId(orderId);

        logger.info("arkOrderLog:智能柜柜门door信息：" + door);

        //更新door表数据
        this.changeDoorState(door, null, Constants.DoorState.EMPTY.getValue());
        //打开柜门
        doorService.openDoorByDoorObject(door);
        ConcurrentHashMapCacheUtils.deleteCache(door.getId());


        //推送微信公众号消息，通知用户服务完全结束
        sendWeChatMsg(res);
        //消息通知：用户：订单完成
        Reminder reminder = new Reminder();
        reminder.setToClient(res.getClientId());
        reminder.setType(Constants.MessageType.CLIENT_FINISH_REMINDER.getType());
        reminder.setMessage(Constants.MessageType.CLIENT_FINISH_REMINDER.getMessage());
        reminderRepository.saveAndFlush(reminder);
        logger.info("执行用户开柜取车操作：---end---" + orderId);
        return ResultJsonObject.getDefaultResult(orderId);
    }

    /**
     * 技师接单接口
     *
     * @param orderId
     * @param employeeId
     * @return
     */
    public ResultJsonObject orderTaking(String orderId, String employeeId) throws ObjectNotFoundException {
        logger.info("执行技师接单操作：---start---" + orderId);
        if (StringUtils.isEmpty(orderId)) {
            return ResultJsonObject.getCustomResult("The param 'orderId' is null", ResultCode.PARAM_NOT_COMPLETE);
        }
        if (StringUtils.isEmpty(employeeId)) {
            return ResultJsonObject.getCustomResult("The param 'employeeId' is null", ResultCode.PARAM_NOT_COMPLETE);
        }
        ConsumerOrder consumerOrder = consumerOrderRepository.findById(orderId).orElse(null);
        if (null == consumerOrder) {
            return ResultJsonObject.getCustomResult("Not found consumerOrder object by orderId : " + orderId, ResultCode.RESULT_DATA_NONE);
        } else if (!consumerOrder.getState().equals(Constants.OrderState.RESERVATION.getValue())) {
            return ResultJsonObject.getCustomResult("The consumerOrder can't been taken. The param 'orderId' is  " + orderId, ResultCode.ORDER_ACCEPTED);
        }
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        List<ConsumerProjectInfo> consumerProjectInfos = consumerProjectInfoService.getAllProjectInfoByOrderId(orderId);
        //服务商id
        String rspId = null;
        for (ConsumerProjectInfo consumerProjectInfo : consumerProjectInfos) {
            if (StringUtils.isEmpty(rspId)) {
                String rspProjectId = consumerProjectInfo.getProjectId();
                Optional<RSPProject> rspProjectOptional = rspProjectRepository.findByIdAndDelStatus(rspProjectId, Constants.DelStatus.NORMAL.isValue());
                if (rspProjectOptional.isPresent()) {
                    rspId = rspProjectOptional.get().getRspId();
                }
            } else {
                break;
            }
        }
        Staff staff = null;
        if (null != employee) {
            List<Staff> staffList = staffService.findByPhoneAndRspId(employee.getPhone(), rspId);
            if (staffList.size() > 0) staff = staffList.get(0);
        }
        if (null == staff) {
            return ResultJsonObject.getCustomResult("Not found staff object by employeeId : " + employeeId, ResultCode.RESULT_DATA_NONE);
        }
        String staffName = staff.getName();
        consumerOrder.setState(Constants.OrderState.ORDER_TAKING.getValue());
        consumerOrder.setPickCarStaffId(staff.getId());
        consumerOrder.setPickCarStaffName(staffName);
        consumerOrder.setOrderTakingTime(new Timestamp(System.currentTimeMillis()));
        ConsumerOrder orderRes = this.updateOrder(consumerOrder);

        Door door = doorRepository.findTopByOrderId(orderId);
        logger.info("arkOrderLog:智能柜柜门door信息：" + door);

        //推送微信公众号消息，通知用户已开始受理服务
        sendWeChatMsg(orderRes);

        Reminder reminder = new Reminder();
        reminder.setToClient(orderRes.getClientId());
        reminder.setType(Constants.MessageType.CLIENT_ORDER_TAKING_REMINDER.getType());
        reminder.setMessage(Constants.MessageType.CLIENT_ORDER_TAKING_REMINDER.getMessage());
        reminderRepository.saveAndFlush(reminder);

        //通知其他技师，该订单已经受理，
        // 消息通知：接单技师：还车提醒
        String staffOpenId = staff.getOpenId();
        staffService.sendWeChatMessageToStaff(orderRes, door, staffOpenId, rspId);
        logger.info("执行技师接单操作：---end---" + orderId);
        return ResultJsonObject.getDefaultResult(orderId);
    }

    /**
     * 技师取消接单:通知其他技师
     *
     * @param orderId
     * @param employeeId
     */
    public ResultJsonObject cancelOrderTaking(String orderId, String employeeId) throws ObjectNotFoundException {
        logger.info("执行技师取消接单操作：---start---" + orderId);
        if (StringUtils.isEmpty(orderId)) {
            return ResultJsonObject.getCustomResult("The param 'orderId' is null", ResultCode.PARAM_NOT_COMPLETE);
        }
        if (StringUtils.isEmpty(employeeId)) {
            return ResultJsonObject.getCustomResult("The param 'employeeId' is null", ResultCode.PARAM_NOT_COMPLETE);
        }
        ConsumerOrder consumerOrder = consumerOrderRepository.findById(orderId).orElse(null);
        if (null == consumerOrder) {
            return ResultJsonObject.getCustomResult("Not found consumerOrder object by orderId : " + orderId, ResultCode.RESULT_DATA_NONE);
        } else if (!consumerOrder.getState().equals(Constants.OrderState.ORDER_TAKING.getValue())) {
            return ResultJsonObject.getCustomResult("The staff can't cancel the operation. The param 'orderId' is  " + orderId, ResultCode.ORDER_ACCEPTED);
        }
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        List<ConsumerProjectInfo> consumerProjectInfos = consumerProjectInfoService.getAllProjectInfoByOrderId(orderId);
        //服务商id
        String rspId = null;
        for (ConsumerProjectInfo consumerProjectInfo : consumerProjectInfos) {
            if (StringUtils.isEmpty(rspId)) {
                String rspProjectId = consumerProjectInfo.getProjectId();
                Optional<RSPProject> rspProjectOptional = rspProjectRepository.findByIdAndDelStatus(rspProjectId, Constants.DelStatus.NORMAL.isValue());
                if (rspProjectOptional.isPresent()) {
                    rspId = rspProjectOptional.get().getRspId();
                }
            } else {
                break;
            }
        }
        Staff staff = null;
        if (null != employee) {
            List<Staff> staffList = staffService.findByPhoneAndRspId(employee.getPhone(), rspId);
            if (staffList.size() > 0) staff = staffList.get(0);
        }
        if (null == staff) {
            return ResultJsonObject.getCustomResult("Not found staff object by employeeId : " + employeeId, ResultCode.RESULT_DATA_NONE);
        }
        consumerOrder.setState(Constants.OrderState.RESERVATION.getValue());
        consumerOrder.setPickCarStaffId(null);
        consumerOrder.setPickCarStaffName(null);
        ConsumerOrder orderRes = this.updateOrder(consumerOrder);

        Reminder reminder = new Reminder();
        reminder.setToEmployee(employeeId);
        reminder.setType(Constants.MessageType.EMPLOYEE_CANCEL_REMINDER.getType());
        reminder.setMessage(Constants.MessageType.EMPLOYEE_CANCEL_REMINDER.getMessage());
        reminderRepository.saveAndFlush(reminder);

        Door door = doorRepository.findTopByOrderId(orderId);
        logger.info("arkOrderLog:智能柜柜门door信息：" + door);
        //推送微信公众号消息，通知用户
//        sendWeChatMsg(orderRes);
        //该订单已经取消，通知技师接单
        String staffOpenId = staff.getOpenId();
        staffService.sendWeChatMessageToStaff(orderRes, door, staffOpenId, rspId);
        logger.info("执行技师取消接单操作：---end---" + orderId);
        return ResultJsonObject.getDefaultResult(orderId);
    }

    /**
     * 技师去取车，提醒用户订单已经被受理
     *
     * @param orderId
     * @param employeeId
     * @return
     */
    public ResultJsonObject pickCar(String orderId, String employeeId) throws Exception {
        logger.info("执行技师接车操作：---start---" + orderId);
        if (StringUtils.isEmpty(orderId)) {
            return ResultJsonObject.getCustomResult("The param 'orderId' is null", ResultCode.PARAM_NOT_COMPLETE);
        }
        if (StringUtils.isEmpty(employeeId)) {
            return ResultJsonObject.getCustomResult("The param 'staffId' is null", ResultCode.PARAM_NOT_COMPLETE);
        }
        ConsumerOrder consumerOrder = consumerOrderRepository.findById(orderId).orElse(null);
        if (null == consumerOrder) {
            return ResultJsonObject.getCustomResult("Not found consumerOrder object by orderId : " + orderId, ResultCode.RESULT_DATA_NONE);
        } else if (!consumerOrder.getState().equals(Constants.OrderState.ORDER_TAKING.getValue())) {
            return ResultJsonObject.getCustomResult("Wrong state. The param 'orderId' is  " + orderId, ResultCode.ORDER_ACCEPTED);
        }
        List<ConsumerProjectInfo> consumerProjectInfos = consumerProjectInfoService.getAllProjectInfoByOrderId(orderId);
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        //服务商id
        String rspId = null;
        for (ConsumerProjectInfo consumerProjectInfo : consumerProjectInfos) {
            if (StringUtils.isEmpty(rspId)) {
                String rspProjectId = consumerProjectInfo.getProjectId();
                Optional<RSPProject> rspProjectOptional = rspProjectRepository.findByIdAndDelStatus(rspProjectId, Constants.DelStatus.NORMAL.isValue());
                if (rspProjectOptional.isPresent()) {
                    rspId = rspProjectOptional.get().getRspId();
                }
            } else {
                break;
            }
        }
        Staff staff = null;
        if (null != employee) {
            List<Staff> staffList = staffService.findByPhoneAndRspId(employee.getPhone(), rspId);
            if (staffList.size() > 0) staff = staffList.get(0);
        }
        if (null == staff) {
            return ResultJsonObject.getCustomResult("Not found staff object by employeeId : " + employeeId, ResultCode.RESULT_DATA_NONE);
        }

        String staffName = staff.getName();

        consumerOrder.setPickTime(new Timestamp(System.currentTimeMillis()));
        consumerOrder.setState(Constants.OrderState.RECEIVE_CAR.getValue());
        consumerOrder.setPickCarStaffId(staff.getId());
        consumerOrder.setPickCarStaffName(staffName);
        ConsumerOrder orderRes = this.updateOrder(consumerOrder);

        // 回填服务技师的id和name

        for (ConsumerProjectInfo consumerProjectInfo : consumerProjectInfos) {
            consumerProjectInfo.setStaffId(staff.getId());
            consumerProjectInfo.setStaffName(staffName);
            consumerProjectInfoService.saveOrUpdate(consumerProjectInfo);
        }


        //更新door表数据状态
        Door door = doorRepository.findTopByOrderId(orderId);

        logger.info("arkOrderLog:智能柜柜门door信息：" + door);

        this.changeDoorState(door, null, Constants.DoorState.EMPTY.getValue());
        // 调用硬件接口方法打开柜门
        doorService.openDoorByDoorObject(door);
        ConcurrentHashMapCacheUtils.deleteCache(door.getId());


        //推送微信公众号消息，通知用户已开始受理服务
        sendWeChatMsg(orderRes);

        //消息通知：还车提醒
        Reminder reminder = new Reminder();
        reminder.setToEmployee(employeeId);
        reminder.setType(Constants.MessageType.EMPLOYEE_CAR_RETURN_REMINDER.getType());
        reminder.setMessage(Constants.MessageType.EMPLOYEE_CAR_RETURN_REMINDER.getMessage());
        reminderRepository.saveAndFlush(reminder);
        //通知其他技师，该订单已经受理
        /*String staffOpenId = staff.getOpenId();
        staffService.sendWeChatMessageToStaff(orderRes, door, staffOpenId, rspId);*/

        logger.info("执行技师接车操作：---end---" + orderId);
        return ResultJsonObject.getDefaultResult(orderId);
    }

    /**
     * 技师还车，提醒用户来取车
     *
     * @param orderObject
     * @return
     */
    public ResultJsonObject finishCar(OrderObject orderObject) throws ArgumentMissingException, ObjectNotFoundException, OpenArkDoorTimeOutException, InterruptedException, OpenArkDoorFailedException, DoorInUseException {
        logger.info("执行技师还车操作：---start---");
        ConsumerOrder consumerOrder = orderObject.getConsumerOrder();
        if (null == consumerOrder) {
            throw new ArgumentMissingException("参数中的consumerOrder对象为空");
        }

        String orderId = consumerOrder.getId();
        if (StringUtils.isEmpty(orderId)) {
            return ResultJsonObject.getCustomResult("参数orderId值为null", ResultCode.PARAM_NOT_COMPLETE);
        }

        Door doorOld = doorRepository.findTopByOrderId(consumerOrder.getId());
        String doorId;
        Door emptyDoor;
        if (null == doorOld) {
            doorId = orderObject.getDoorId();
            emptyDoor = (Door) ConcurrentHashMapCacheUtils.getCache(doorId);
            Door door = doorRepository.findById(doorId).orElse(null);
            if (null == door) {
                throw new ObjectNotFoundException("未找到分配的柜门，请稍后重试");
            }
            if (door.getState() != Constants.DoorState.EMPTY.getValue()) {
                throw new DoorInUseException("柜门占用中,请稍后重试");
            }

            logger.info("arkOrderLog:前端传入的doorId：" + doorId);

            if (StringUtils.isEmpty(doorId)) {
                throw new ArgumentMissingException("参数中的doorId对象为空");
            }
        } else {
            doorId = doorOld.getId();
            emptyDoor = doorOld;
        }

        /*consumerOrder.setFinishTime(new Timestamp(System.currentTimeMillis()));
        consumerOrder.setState(Constants.OrderState.SERVICE_FINISH.getValue());*/


        if (null == emptyDoor) {
            throw new ObjectNotFoundException("未找到分配的柜门号，请稍后重试");
        }
        if (StringUtils.isEmpty(consumerOrder.getStaffKeyLocation())) {
            logger.info("arkOrderLog:智能柜柜门door信息：" + emptyDoor);

            // 更新技师把钥匙存放在哪个柜子的哪个门
            String staffKeyLocation = emptyDoor.getArkName() + Constants.HYPHEN + emptyDoor.getDoorSn() + "号门";
            String staffKeyLocationSn = emptyDoor.getArkSn() + Constants.HYPHEN + emptyDoor.getDoorSn();
            consumerOrder.setStaffKeyLocation(staffKeyLocation);
            consumerOrder.setStaffKeyLocationSn(staffKeyLocationSn);
        }
        /*else {
            emptyDoor = doorRepository.findTopByOrderId(consumerOrder.getId());
        }*/
        ConsumerOrder order;
        try {
            order = this.updateOrder(consumerOrder);
        } catch (ObjectNotFoundException e) {
            ConcurrentHashMapCacheUtils.deleteCache(doorId);
            throw e;
        }


        //关联技师上传订单车辆图片
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        List<StaffOrderImg> staffOrderImgs = orderObject.getStaffOrderImgs();
        if (null != staffOrderImgs) {
            for (StaffOrderImg img :
                    staffOrderImgs) {
                img.setOrderId(orderId);
                img.setDelStatus(false);
                img.setCreateTime(currentTime);
            }
            staffOrderImgRepository.saveAll(staffOrderImgs);
        }

        // 更新door表数据状态
        this.changeDoorState(emptyDoor, orderId, Constants.DoorState.PER_STAFF_FINISH.getValue());
        // 调用硬件接口方法打开柜门
        try {
            //todo
            doorService.openDoorByDoorObjectForBeginAndEnd(emptyDoor, false, null, null);
        } catch (OpenArkDoorFailedException e) {
            throw e;
        } finally {
            ConcurrentHashMapCacheUtils.deleteCache(doorId);
        }

        logger.info("执行技师还车操作：---end---" + orderId);
        return ResultJsonObject.getDefaultResult(orderId);
    }

    /**
     * 微信支付成功后回调，处理订单业务
     *
     * @param orderId
     * @return
     */
    public ResultJsonObject wechatPaySuccess(String orderId) {
        if (StringUtils.hasText(orderId)) {
            //判断是哪种订单
            String firstCharacter = orderId.substring(0, 1);
            switch (firstCharacter) {
                case "A":
                    try {
                        return this.arkPaySuccess(orderId);
                    } catch (ObjectNotFoundException e) {
                        e.printStackTrace();
                        return ResultJsonObject.getErrorResult(null, e.getMessage());
                    }
                case "C":
                    try {
                        return this.buyCardPaySuccess(orderId);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                        e.printStackTrace();
                        return ResultJsonObject.getErrorResult(null, e.getMessage());
                    }
                case "R":
                    try {
                        return this.rechargeCardPaySuccess(orderId);
                    } catch (ObjectNotFoundException | ArgumentMissingException | UpdateDataErrorException e) {
                        logger.error(e.getMessage(), e);
                        e.printStackTrace();
                        return ResultJsonObject.getErrorResult(null, e.getMessage());
                    }

                default:
                    return ResultJsonObject.getErrorResult(null, "该类型的订单不支持微信支付，请联系平台管理员核实。");
            }
        }
        String msg = "orderId为空，无法执行回调业务，会出现重复支付的情况，请联系门店或管理平台处理。";
        logger.error(msg);
        return ResultJsonObject.getErrorResult(null, msg);
    }


    /**
     * 智能柜服订单支付成功后更新支付状态
     *
     * @param orderId
     * @return
     */
    public ResultJsonObject arkPaySuccess(String orderId) throws ObjectNotFoundException {
        ConsumerOrder consumerOrder = consumerOrderRepository.findById(orderId).orElse(null);
        if (null == consumerOrder) {
            return ResultJsonObject.getCustomResult(null, ResultCode.RESULT_DATA_NONE);
        }
        consumerOrder.setPayState(Constants.PayState.FINISH_PAY.getValue());

        //设置支付方式一为微信支付，且支付方式一的金额为订单总体实付金额
        consumerOrder.setFirstPayMethod(Constants.PayMethod.WECHAT_PAY.getCode());
        consumerOrder.setFirstActualPrice(consumerOrder.getActualPrice());

        updateOrder(consumerOrder);

        //消息通知:用户：取车提醒
        Reminder reminder = new Reminder();
        reminder.setToClient(consumerOrder.getClientId());
        reminder.setType(Constants.MessageType.CLIENT_PICK_UP_REMINDER.getType());
        reminder.setMessage(Constants.MessageType.CLIENT_PICK_UP_REMINDER.getMessage());
        reminderRepository.saveAndFlush(reminder);
        return ResultJsonObject.getDefaultResult(orderId);
    }

    /**
     * 购买卡券的订单支付成功后更新支付状态，并更新对应的卡券信息
     *
     * @param orderId
     * @return
     */
    public ResultJsonObject buyCardPaySuccess(String orderId) throws Exception {
        ConsumerOrder consumerOrder = consumerOrderRepository.findById(orderId).orElse(null);
        if (null == consumerOrder) {
            throw new ObjectNotFoundException("订单数据未查询到，无法更新卡券状态和订单状态");
        }
        consumerOrder.setPayState(Constants.PayState.FINISH_PAY.getValue());
        //设置支付方式一为微信支付，且支付方式一的金额为订单总体实付金额
        consumerOrder.setFirstPayMethod(Constants.PayMethod.WECHAT_PAY.getCode());
        consumerOrder.setFirstActualPrice(consumerOrder.getActualPrice());

        //更新卡/券的状态，是其可以使用
        String cardOrCouponId = consumerOrder.getCardOrCouponId();
        if (StringUtils.isEmpty(cardOrCouponId)) {
            throw new ArgumentMissingException("订单中的卡券ID为空，无法更新订单状态");
        }
        Card card = cardRepository.findById(cardOrCouponId).orElse(null);
        Coupon coupon = couponRepository.findById(cardOrCouponId).orElse(null);
        if (null == card && null == coupon) {
            throw new ObjectNotFoundException("未找到卡/券的对象，无法更新卡券状态和订单状态");
        }

        Timestamp currentTime = new Timestamp(System.currentTimeMillis());

        if (null != card) {
            card.setDelStatus(Constants.DelStatus.NORMAL.isValue());
            card.setPayDate(currentTime);
            card.setPayMethod(Constants.PayMethod.WECHAT_PAY.getCode());
            com.freelycar.saas.project.entity.CardService cardServiceObject = cardServiceRepository.findById(card.getCardServiceId()).orElse(null);
            if (cardServiceObject == null) {
                throw new ObjectNotFoundException("对象cardServiceObject为空值，无法更新卡券状态和订单状态");
            }
            card.setExpirationDate(TimestampUtil.getExpirationDateForYear(cardServiceObject.getValidTime()));
            card.setPayDate(currentTime);
            cardRepository.save(card);
        }
        if (null != coupon) {
            coupon.setDelStatus(Constants.DelStatus.NORMAL.isValue());
            coupon.setPayMethod(Constants.PayMethod.WECHAT_PAY.getCode());
            com.freelycar.saas.project.entity.CouponService couponServiceObject = couponServiceRepository.findById(coupon.getCouponServiceId()).orElse(null);
            if (couponServiceObject == null) {
                throw new ObjectNotFoundException("对象couponServiceObject为空值，无法更新卡券状态和订单状态");
            }
            coupon.setDeadline(TimestampUtil.getExpirationDateForMonth(couponServiceObject.getValidTime()));
            couponRepository.save(coupon);
        }

        //更新订单状态
        ConsumerOrder orderRes = updateOrder(consumerOrder);
        if (null == orderRes) {
            throw new UpdateDataErrorException("单据数据更新失败，无法更新卡券状态和订单状态");
        }


        return ResultJsonObject.getDefaultResult(orderId);
    }

    /**
     * 给用户推送微信公众号消息
     *
     * @param consumerOrder
     */
    /*public void sendWeChatMsg(ConsumerOrder consumerOrder) {
        //推送微信公众号消息，通知用户已开始受理服务
        String phone = consumerOrder.getPhone();
        String openId = wxUserInfoService.getOpenId(phone);
        if (StringUtils.isEmpty(openId)) {
            logger.error("未获得到对应的openId，微信消息推送失败");
        } else {
            WechatTemplateMessage.orderChanged(consumerOrder, openId);
        }
    }*/
    //更新为小程序openId
    public void sendWeChatMsg(ConsumerOrder consumerOrder) {
        //推送微信消息，通知用户已服务状态变化：订单生成成功
        String phone = consumerOrder.getPhone();
        WxUserInfo wxUserInfo = wxUserInfoService.getWxUserByPhone(phone);
        if (StringUtils.isEmpty(wxUserInfo.getOpenId()) && StringUtils.isEmpty(wxUserInfo.getMiniOpenId())) {
            logger.error("未获得到对应的openId或miniOpenId，微信消息推送失败");
        } else {
            //推送统一服务消息或订阅消息，统一服务消息发送失败时推送订阅消息，订阅消息在微信服务通知中
            OrderChangedMessage message = MiniMessage.genMessageForClient(consumerOrder, wxUserInfo);
            message.setAccessToken(miniProgramUtils.getAccessTokenForInteface().getString("access_token"));
            int state = consumerOrder.getState();
            switch (state) {
                case -1://已接单
                    message.setTemplateId(MiniMessage.CLIENT_ORDER_TAKING_ID);
                    break;
                case 3://已交车
                    message.setTemplateId(MiniMessage.CLIENT_ORDER_FINISH_ID);
                    break;
                default:
                    break;
            }
            boolean r1 = MiniMessage.sendUniformMessage(message);
            boolean r2;
            if (!r1) {
                r2 = MiniMessage.sendSubscribeMessage(message);
                if (!r2) {
                    logger.error("sendWeChatMsg:{}失败", message);
                }
            }
        }
    }

    /**
     * 改变door表的数据状态
     *
     * @param arkSn
     * @param doorSn
     * @param orderId
     * @param doorState
     * @throws Exception
     */
    /*private void changeDoorState(String arkSn, String doorSn, String orderId, int doorState) throws Exception {
        Door door = doorRepository.findTopByArkSnAndDoorSn(arkSn, doorSn);
        this.changeDoorState(door, orderId, doorState);
    }*/

    /**
     * 改变door表的数据状态
     *
     * @param door
     * @param orderId
     * @param doorState
     * @throws Exception
     */
    private void changeDoorState(Door door, String orderId, int doorState) throws ArgumentMissingException {
        if (null == door) {
            throw new ArgumentMissingException("没找到分配的智能柜door表信息，无法更新状态。预约服务状态终止。");
        }
        door.setOrderId(orderId);
        door.setState(doorState);
        doorRepository.saveAndFlush(door);
    }

    /**
     * 生成购买卡券的订单
     *
     * @param client
     * @param price
     * @param cardOrCouponId
     * @return
     * @throws Exception
     */
    public ConsumerOrder generateOrderForBuyCardOrCoupon(Client client, double price, String cardOrCouponId) throws Exception {
        if (null == client || StringUtils.isEmpty(cardOrCouponId)) {
            throw new ArgumentMissingException("生成购买卡券的订单失败，原因：参数缺失");
        }

        String clientName = client.getTrueName();
        if (StringUtils.isEmpty(clientName)) {
            clientName = client.getName();
        }

        ConsumerOrder order = new ConsumerOrder();
        order.setPayState(Constants.PayState.NOT_PAY.getValue());
        order.setOrderType(Constants.OrderType.CARD.getValue());
        order.setClientId(client.getId());
        order.setTotalPrice(price);
        order.setActualPrice(price);

        order.setClientName(clientName);
        order.setPhone(client.getPhone());
        order.setIsMember(client.getMember());
        order.setGender(client.getGender());
        order.setStoreId(client.getStoreId());
        order.setCardOrCouponId(cardOrCouponId);

        return this.saveOrUpdate(order);
    }

    /**
     * 生成会员卡充值订单
     *
     * @param client
     * @param price
     * @param cardId
     * @return
     * @throws Exception
     */
    public ConsumerOrder generateOrderForRecharge(Client client, double price, String cardId) throws Exception {
        if (null == client || StringUtils.isEmpty(cardId)) {
            throw new ArgumentMissingException("生成会员卡充值的订单失败，原因：参数缺失");
        }

        String clientName = client.getTrueName();
        if (StringUtils.isEmpty(clientName)) {
            clientName = client.getName();
        }

        ConsumerOrder order = new ConsumerOrder();
        order.setPayState(Constants.PayState.NOT_PAY.getValue());
        order.setOrderType(Constants.OrderType.RECHARGE.getValue());
        order.setClientId(client.getId());
        order.setTotalPrice(price);
        order.setActualPrice(price);

        order.setClientName(clientName);
        order.setPhone(client.getPhone());
        order.setIsMember(client.getMember());
        order.setGender(client.getGender());
        order.setStoreId(client.getStoreId());
        order.setCardOrCouponId(cardId);

        return this.saveOrUpdate(order);
    }


    /**
     * 查询消费记录（分页）
     *
     * @param params
     * @param currentPage
     * @param pageSize
     * @return
     * @throws ArgumentMissingException
     */
    public PaginationRJO orderRecord(Map<String, Object> params, Integer currentPage, Integer pageSize) throws ArgumentMissingException {
        if (null == params) {
            throw new ArgumentMissingException("参数params为空值，无法查询消费记录");
        }
        String clientId = (String) params.get("clientId");
        String startTime = (String) params.get("startTime");
        String endTime = (String) params.get("endTime");

        if (StringUtils.isEmpty(clientId)) {
            throw new ArgumentMissingException("参数clientId为空值，无法查询消费记录");
        }

        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT co.id, ( SELECT group_concat( cpi.projectName ) FROM consumerprojectinfo cpi WHERE cpi.consumerOrderId = co.id ) AS projectNames, co.actualPrice AS cost, CONCAT((CASE co.firstPayMethod WHEN 0 THEN '储值卡' WHEN 1 THEN '现金' WHEN 2 THEN '微信' WHEN 3 THEN '支付宝' WHEN 4 THEN '易付宝' WHEN 5 THEN '刷卡' ELSE '无' END ), (CASE co.secondPayMethod WHEN 0 THEN '，储值卡' WHEN 1 THEN '，现金' WHEN 2 THEN '，微信' WHEN 3 THEN '，支付宝' WHEN 4 THEN '，易付宝' WHEN 5 THEN '，刷卡' ELSE '' END ) ) AS payMethod, (CASE (( CASE co.firstPayMethod WHEN 0 THEN TRUE ELSE FALSE END ) OR ( CASE co.secondPayMethod WHEN 0 THEN TRUE ELSE FALSE END )) WHEN TRUE THEN '是' ELSE '否' END) AS useCard, co.createTime AS serviceTime FROM consumerorder co WHERE co.clientId = '").append(clientId).append("' AND co.delStatus = 0 AND payState = 2 ");
        if (StringUtils.hasText(startTime)) {
            sql.append(" AND co.createTime > '").append(startTime).append(" 00:00:00' ");
        }
        if (StringUtils.hasText(endTime)) {
            sql.append(" AND co.createTime <= '").append(endTime).append(" 23:59:59' ");
        }
        sql.append(" ORDER BY co.createTime DESC ");

        Pageable pageable = PageableTools.basicPage(currentPage, pageSize);

        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());
        nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(OrderRecordObject.class));

        int total = nativeQuery.getResultList().size();
        @SuppressWarnings({"unused", "unchecked"})
        List<OrderRecordObject> orderRecordObjects = nativeQuery.setFirstResult(MySQLPageTool.getStartPosition(currentPage, pageSize)).setMaxResults(pageSize).getResultList();

        //关闭em
        em.close();

        @SuppressWarnings("unchecked")
        Page<OrderRecordObject> page = new PageImpl(orderRecordObjects, pageable, total);

        return PaginationRJO.of(page);
    }

    /**
     * 查询/导出流水明细
     *
     * @param storeId
     * @param startTime
     * @param endTime
     * @return
     */
    private String generateOrderParticularsSQL(String storeId, String startTime, String endTime) {

        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT co.id, co.carBrand, co.licensePlate, co.clientName, co.phone, IFNULL(( SELECT group_concat( cpi.projectName ) FROM consumerprojectinfo cpi WHERE cpi.consumerOrderId = co.id ),'购卡/充值') AS projectNames, co.actualPrice AS cost, co.createTime AS serviceTime, (case co.isMember when 1 then '是' else '否' end) as isMember FROM consumerorder co WHERE co.delStatus = 0 AND payState = 2 ");
        if (StringUtils.hasText(storeId)) {
            sql.append(" AND co.storeId = '").append(storeId).append("' ");
        }
        if (StringUtils.hasText(startTime)) {
            sql.append(" AND co.createTime > '").append(startTime).append(" 00:00:00' ");
        }
        if (StringUtils.hasText(endTime)) {
            sql.append(" AND co.createTime <= '").append(endTime).append(" 23:59:59' ");
        }
        sql.append(" ORDER BY co.createTime DESC ");

        return sql.toString();
    }

    /**
     * 查询流水明细
     * ps:因为管理端需要，这个sql返回的方法会多一些
     *
     * @param storeId
     * @param startTime
     * @param endTime
     * @return
     */
    private String generateOrderParticularsDetailSQL(String storeId, String startTime, String endTime) {

        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT co.id, co.carBrand, co.licensePlate, co.clientName, co.phone, IFNULL(( SELECT group_concat( cpi.projectName ) FROM consumerprojectinfo cpi WHERE cpi.consumerOrderId = co.id ),'购卡/充值') AS projectNames, co.actualPrice AS cost, co.createTime AS serviceTime, ( CASE co.isMember WHEN 1 THEN '是' ELSE '否' END ) AS isMember, (SELECT s.`name` FROM store s WHERE co.storeId=s.id) AS storeName, co.totalPrice AS orderCost, CONCAT(CASE IFNULL(co.firstPayMethod,6) WHEN 0 THEN '储值卡'WHEN 1 THEN '现金'WHEN 2 THEN '微信'WHEN 3 THEN '支付宝'WHEN 4 THEN '易付宝'WHEN 5 THEN '刷卡'ELSE '未指定'END,CASE IFNULL(co.secondPayMethod,6) WHEN 0 THEN '储值卡'WHEN 1 THEN '现金'WHEN 2 THEN '微信'WHEN 3 THEN '支付宝'WHEN 4 THEN '易付宝'WHEN 5 THEN '刷卡'ELSE ''END) AS payMethod FROM consumerorder co WHERE co.delStatus = 0 AND payState = 2 ");
        if (StringUtils.hasText(storeId)) {
            sql.append(" AND co.storeId = '").append(storeId).append("' ");
        }
        if (StringUtils.hasText(startTime)) {
            sql.append(" AND co.createTime > '").append(startTime).append(" 00:00:00' ");
        }
        if (StringUtils.hasText(endTime)) {
            sql.append(" AND co.createTime <= '").append(endTime).append(" 23:59:59' ");
        }
        sql.append(" ORDER BY co.createTime DESC ");

        return sql.toString();
    }

    /**
     * 查询流水报表
     *
     * @param storeId
     * @param startTime
     * @param endTime
     * @return
     */
    private String generateListOrderReportSQL(String storeId, String startTime, String endTime, Integer currentPage, Integer pageSize) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT \n" +
                "co.id, \n" +
                "co.carBrand, \n" +
                "co.licensePlate, \n" +
                "co.clientName, \n" +
                "co.phone, \n" +
                "IFNULL(( SELECT group_concat( cpi.projectName ) FROM consumerprojectinfo cpi WHERE cpi.consumerOrderId = co.id ),'购卡/充值') AS projectNames, \n" +
                "co.actualPrice AS cost, \n" +
                "co.createTime AS serviceTime,  \n" +
                "co.totalPrice AS orderCost\n" +
                "FROM consumerorder co \n" +
                "WHERE co.delStatus = 0 AND payState = 2 ");
        if (StringUtils.hasText(storeId)) {
            sql.append(" AND co.storeId = '").append(storeId).append("' ");
        }
        if (StringUtils.hasText(startTime)) {
            sql.append(" AND co.createTime > '").append(startTime).append(" 00:00:00' ");
        }
        if (StringUtils.hasText(endTime)) {
            sql.append(" AND co.createTime <= '").append(endTime).append(" 23:59:59' ");
        }
        sql.append(" ORDER BY co.createTime DESC LIMIT ").append((currentPage - 1) * pageSize).append(",").append(pageSize);

        return sql.toString();
    }

    private String generateListOrderReportSQL(String storeId, String startTime, String endTime) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT \n" +
                "co.id, \n" +
                "co.carBrand, \n" +
                "co.licensePlate, \n" +
                "co.clientName, \n" +
                "co.phone, \n" +
                "IFNULL(( SELECT group_concat( cpi.projectName ) FROM consumerprojectinfo cpi WHERE cpi.consumerOrderId = co.id ),'购卡/充值') AS projectNames, \n" +
                "co.actualPrice AS cost, \n" +
                "co.createTime AS serviceTime,  \n" +
                "co.totalPrice AS orderCost\n" +
                "FROM consumerorder co \n" +
                "WHERE co.delStatus = 0 AND payState = 2 ");
        if (StringUtils.hasText(storeId)) {
            sql.append(" AND co.storeId = '").append(storeId).append("' ");
        }
        if (StringUtils.hasText(startTime)) {
            sql.append(" AND co.createTime > '").append(startTime).append(" 00:00:00' ");
        }
        if (StringUtils.hasText(endTime)) {
            sql.append(" AND co.createTime <= '").append(endTime).append(" 23:59:59' ");
        }
        return sql.toString();
    }

    private String generateListOrderReportByRspIdSQL(String rspId, String startTime, String endTime, Integer currentPage, Integer pageSize) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT \n" +
                "co.id,\n" +
                "co.carBrand,\n" +
                "co.licensePlate,\n" +
                "co.clientName,\n" +
                "co.phone, \n" +
                "IFNULL(( SELECT group_concat( cpi.projectName ) FROM consumerprojectinfo cpi WHERE cpi.consumerOrderId = co.id ),'购卡/充值') AS projectNames,\n" +
                "co.actualPrice AS cost,\n" +
                "co.createTime AS serviceTime,  \n" +
                "co.totalPrice AS orderCost\n" +
                "FROM consumerorder co WHERE co.id \n" +
                "IN(\n" +
                "SELECT cpi.consumerOrderId FROM consumerprojectinfo cpi WHERE cpi.projectId \n" +
                "IN (\n" +
                "SELECT p.id FROM rspproject p WHERE p.rspId = '").append(rspId).append("')\n" +
                ")" +
                "and co.delStatus = 0 and co.payState = 2 AND co.state = 3 ");
        if (StringUtils.hasText(startTime)) {
            sql.append(" AND co.createTime > '").append(startTime).append(" 00:00:00' ");
        }
        if (StringUtils.hasText(endTime)) {
            sql.append(" AND co.createTime <= '").append(endTime).append(" 23:59:59' ");
        }
        sql.append(" ORDER BY co.createTime DESC LIMIT ").append((currentPage - 1) * pageSize).append(",").append(pageSize);
        return sql.toString();
    }

    private String generateListOrderReportByRspIdSQL(String rspId, String startTime, String endTime) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT \n" +
                "co.id,\n" +
                "co.carBrand,\n" +
                "co.licensePlate,\n" +
                "co.clientName,\n" +
                "co.phone, \n" +
                "IFNULL(( SELECT group_concat( cpi.projectName ) FROM consumerprojectinfo cpi WHERE cpi.consumerOrderId = co.id ),'购卡/充值') AS projectNames,\n" +
                "co.actualPrice AS cost,\n" +
                "co.createTime AS serviceTime,  \n" +
                "co.totalPrice AS orderCost\n" +
                "FROM consumerorder co WHERE co.id \n" +
                "IN(\n" +
                "SELECT cpi.consumerOrderId FROM consumerprojectinfo cpi WHERE cpi.projectId \n" +
                "IN (\n" +
                "SELECT p.id FROM rspproject p WHERE p.rspId = '").append(rspId).append("')\n" +
                ")" +
                "and co.delStatus = 0 and co.payState = 2 AND co.state = 3 ");
        if (StringUtils.hasText(startTime)) {
            sql.append(" AND co.createTime > '").append(startTime).append(" 00:00:00' ");
        }
        if (StringUtils.hasText(endTime)) {
            sql.append(" AND co.createTime <= '").append(endTime).append(" 23:59:59' ");
        }
        return sql.toString();
    }

    private String generateListOrderReportSumSQL(String storeId, String startTime, String endTime) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT \n" +
                "count(1) \n" +
                "FROM consumerorder co \n" +
                "WHERE co.delStatus = 0 AND payState = 2 ");
        if (StringUtils.hasText(storeId)) {
            sql.append(" AND co.storeId = '").append(storeId).append("' ");
        }
        if (StringUtils.hasText(startTime)) {
            sql.append(" AND co.createTime > '").append(startTime).append(" 00:00:00' ");
        }
        if (StringUtils.hasText(endTime)) {
            sql.append(" AND co.createTime <= '").append(endTime).append(" 23:59:59' ");
        }
        return sql.toString();
    }

    private String generateListOrderReportByRspIdSumSQL(String rspId, String startTime, String endTime) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT \n" +
                "count(1)\n" +
                "FROM consumerorder co WHERE co.id \n" +
                "IN(\n" +
                "SELECT cpi.consumerOrderId FROM consumerprojectinfo cpi WHERE cpi.projectId \n" +
                "IN (\n" +
                "SELECT p.id FROM rspproject p WHERE p.rspId = '").append(rspId).append("')\n" +
                ")\n" +
                "AND co.delStatus = 0 and co.payState = 2 AND co.state = 3");
        if (StringUtils.hasText(startTime)) {
            sql.append(" AND co.createTime > '").append(startTime).append(" 00:00:00' ");
        }
        if (StringUtils.hasText(endTime)) {
            sql.append(" AND co.createTime <= '").append(endTime).append(" 23:59:59' ");
        }
        return sql.toString();
    }

    //计算总金额
    public BigDecimal sumOrderParticularsTotalAccount(String storeId, String startTime, String endTime) {

        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT IFNULL(cast( sum( co.actualPrice ) AS DECIMAL ( 15, 2 ) ),0) AS result FROM consumerorder co WHERE co.delStatus = 0 AND payState = 2 ");
        if (StringUtils.hasText(storeId)) {
            sql.append(" AND co.storeId = '").append(storeId).append("' ");
        }
        if (StringUtils.hasText(startTime)) {
            sql.append(" AND co.createTime > '").append(startTime).append(" 00:00:00' ");
        }
        if (StringUtils.hasText(endTime)) {
            sql.append(" AND co.createTime <= '").append(endTime).append(" 23:59:59' ");
        }

//        logger.info(sql.toString());

        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());

        @SuppressWarnings({"unused", "unchecked"})
        List<BigDecimal> resultList = nativeQuery.getResultList();

        //关闭em
        em.close();

        if (null != resultList && !resultList.isEmpty()) {
            return resultList.get(0);
        } else {
            return BigDecimal.valueOf(0);
        }

    }

    public BigDecimal sumOrderParticularsTotalAccountByRspId(String rspId, String startTime, String endTime) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT IFNULL(cast( sum( co.actualPrice ) AS DECIMAL ( 15, 2 ) ),0) AS result \n" +
                "FROM consumerorder co \n" +
                "WHERE \n" +
                "co.id \n" +
                "IN(\n" +
                "SELECT cpi.consumerOrderId FROM consumerprojectinfo cpi WHERE cpi.projectId \n" +
                "IN (\n" +
                "SELECT p.id FROM rspproject p WHERE p.rspId = '").append(rspId).append("')\n" +
                ")\n" +
                "and co.delStatus = FALSE AND co.payState = 2  AND co.state = 3 ");
        if (StringUtils.hasText(startTime)) {
            sql.append(" AND co.createTime > '").append(startTime).append(" 00:00:00' ");
        }
        if (StringUtils.hasText(endTime)) {
            sql.append(" AND co.createTime <= '").append(endTime).append(" 23:59:59' ");
        }

//        logger.info(sql.toString());

        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());

        @SuppressWarnings({"unused", "unchecked"})
        List<BigDecimal> resultList = nativeQuery.getResultList();

        //关闭em
        em.close();

        if (null != resultList && !resultList.isEmpty()) {
            return resultList.get(0);
        } else {
            return BigDecimal.valueOf(0);
        }
    }

    /**
     * 导出流水明细Excel
     *
     * @param storeId
     * @param startTime
     * @param endTime
     * @return
     */
    public List<OrderParticulars> exportOrderParticulars(String storeId, String startTime, String endTime) {
        String sql = generateOrderParticularsSQL(storeId, startTime, endTime);

        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql);
        nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(OrderParticulars.class));

        @SuppressWarnings({"unused", "unchecked"})
        List<OrderParticulars> orderParticulars = nativeQuery.getResultList();

        //关闭em
        em.close();

        return orderParticulars;
    }

    /**
     * 查询流水明细分页
     *
     * @param storeId
     * @param startTime
     * @param endTime
     * @param currentPage
     * @param pageSize
     * @return
     */
    public PaginationRJO listPageOrderParticulars(String storeId, String startTime, String endTime, Integer currentPage, Integer pageSize) {
        String sql = generateOrderParticularsDetailSQL(storeId, startTime, endTime);

        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql);
        nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(OrderParticulars.class));

        Pageable pageable = PageableTools.basicPage(currentPage, pageSize);
        int total = nativeQuery.getResultList().size();
        @SuppressWarnings({"unused", "unchecked"})
        List<OrderParticulars> orderParticulars = nativeQuery.setFirstResult(MySQLPageTool.getStartPosition(currentPage, pageSize)).setMaxResults(pageSize).getResultList();

        //关闭em
        em.close();

        @SuppressWarnings("unchecked")
        Page<OrderRecordObject> page = new PageImpl(orderParticulars, pageable, total);

        return PaginationRJO.of(page);
    }

    /**
     * 查询流水报表
     */
    public PaginationRJO listOrderReport(String storeId, String startTime, String endTime, Integer currentPage, Integer pageSize) {
        String sql1 = generateListOrderReportSQL(storeId, startTime, endTime, currentPage, pageSize);
        EntityManager em1 = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery1 = em1.createNativeQuery(sql1);
        nativeQuery1.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(OrderParticulars.class));

        @SuppressWarnings({"unused", "unchecked"})
//        List<OrderParticulars> orderParticulars = nativeQuery.setFirstResult(MySQLPageTool.getStartPosition(currentPage, pageSize)).setMaxResults(pageSize).getResultList();
        List<OrderParticulars> orderParticulars = nativeQuery1.getResultList();
        //关闭em
        em1.close();


        String sql2 = generateListOrderReportSumSQL(storeId, startTime, endTime);
        EntityManager em2 = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery2 = em2.createNativeQuery(sql2);

        @SuppressWarnings({"unused", "unchecked"})
//        List<OrderParticulars> orderParticulars = nativeQuery.setFirstResult(MySQLPageTool.getStartPosition(currentPage, pageSize)).setMaxResults(pageSize).getResultList();
        BigInteger total = (BigInteger) nativeQuery2.getSingleResult();
        logger.info("result:{}", total);
        //关闭em
        em2.close();


        Pageable pageable = PageableTools.basicPage(currentPage, pageSize);
        @SuppressWarnings("unchecked")
        Page<OrderRecordObject> page = new PageImpl(orderParticulars, pageable, total.intValue());

        return PaginationRJO.of(page);

    }

    public List<OrderParticulars> listOrderReport(String storeId, String startTime, String endTime) {
        String sql1 = generateListOrderReportSQL(storeId, startTime, endTime);
        EntityManager em1 = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery1 = em1.createNativeQuery(sql1);
        nativeQuery1.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(OrderParticulars.class));

        @SuppressWarnings({"unused", "unchecked"})
//        List<OrderParticulars> orderParticulars = nativeQuery.setFirstResult(MySQLPageTool.getStartPosition(currentPage, pageSize)).setMaxResults(pageSize).getResultList();
        List<OrderParticulars> orderParticulars = nativeQuery1.getResultList();
        //关闭em
        em1.close();

        return orderParticulars;

    }

    public PaginationRJO listOrderReportByRspId(String rspId, String startTime, String endTime, Integer currentPage, Integer pageSize) {
        String sql1 = generateListOrderReportByRspIdSQL(rspId, startTime, endTime, currentPage, pageSize);
        EntityManager em1 = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery1 = em1.createNativeQuery(sql1);
        nativeQuery1.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(OrderParticulars.class));

        @SuppressWarnings({"unused", "unchecked"})
//        List<OrderParticulars> orderParticulars = nativeQuery.setFirstResult(MySQLPageTool.getStartPosition(currentPage, pageSize)).setMaxResults(pageSize).getResultList();
        List<OrderParticulars> orderParticulars = nativeQuery1.getResultList();
        //关闭em
        em1.close();

        String sql2 = generateListOrderReportByRspIdSumSQL(rspId, startTime, endTime);
        EntityManager em2 = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery2 = em2.createNativeQuery(sql2);

        @SuppressWarnings({"unused", "unchecked"})
//        List<OrderParticulars> orderParticulars = nativeQuery.setFirstResult(MySQLPageTool.getStartPosition(currentPage, pageSize)).setMaxResults(pageSize).getResultList();
        BigInteger total = (BigInteger) nativeQuery2.getSingleResult();
        logger.info("result:{}", total);
        //关闭em
        em2.close();

        Pageable pageable = PageableTools.basicPage(currentPage, pageSize);
        @SuppressWarnings("unchecked")
        Page<OrderRecordObject> page = new PageImpl(orderParticulars, pageable, total.intValue());

        return PaginationRJO.of(page);
    }

    public List<OrderParticulars> listOrderReportByRspId(String rspId, String startTime, String endTime) {
        String sql1 = generateListOrderReportByRspIdSQL(rspId, startTime, endTime);
        EntityManager em1 = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery1 = em1.createNativeQuery(sql1);
        nativeQuery1.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(OrderParticulars.class));

        @SuppressWarnings({"unused", "unchecked"})
//        List<OrderParticulars> orderParticulars = nativeQuery.setFirstResult(MySQLPageTool.getStartPosition(currentPage, pageSize)).setMaxResults(pageSize).getResultList();
        List<OrderParticulars> orderParticulars = nativeQuery1.getResultList();
        //关闭em
        em1.close();
        return orderParticulars;
    }

    public List getMongthlyIncomeByYear(String year) {
        String startTime = year;
        String endTime = (Integer.valueOf(year) + 1) + "";
        startTime += "-01-01 00:00:00";
        endTime += "-01-01 00:00:00";
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT DATE_FORMAT(createTime,\"%Y-%m\") as createDate,SUM(totalPrice) as orderCount FROM `consumerorder` \n" +
                "WHERE delstatus=0 and orderType=2 and state<4  AND createTime >= \'" + startTime + "\' and createTime < \'" + endTime +
                "\' GROUP BY createDate order by createDate DESC");

        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());

        nativeQuery.unwrap(NativeQuery.class);

        @SuppressWarnings({"unused", "unchecked"})
        List res = nativeQuery.getResultList();
        em.close();
        return res;
    }

    /**
     * 营业汇总-时间
     *
     * @param year
     * @return
     */
    public JSONObject getIncomeByYear(String year) {
        List year1 = getMongthlyIncomeByYear(year);
        List yearList = new ArrayList();
        //验证数据完整性，1-12月数据是否完全
        for (int i = 0; i < 12; i++) {
            int month = 12 - i;
            String month_year = year + "-" + (month < 10 ? ("0" + month) : month + "");
            boolean flag = true;//是否需要添加时间
            for (int j = 0; j < year1.size(); j++) {
                Object[] obj = (Object[]) year1.get(j);
                String month1 = (String) obj[0];
                if (month_year.equals(month1)) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                Object[] obj = {month_year, 0.0};
                year1.add(obj);
            }
        }
        //月数据重新排序
        for (int i = 0; i < year1.size(); i++) {
            int month = 12 - i;
            String month_year = year + "-" + (month < 10 ? ("0" + month) : month + "");
            for (int j = 0; j < year1.size(); j++) {
                Object[] obj = (Object[]) year1.get(j);
                String month1 = (String) obj[0];
                if (month_year.equals(month1)) {
                    yearList.add(year1.get(j));
                    break;
                }
            }
        }
        //计算当年总额
        double sum = 0;
        for (int i = 0; i < year1.size(); i++) {
            Object[] objs = (Object[]) year1.get(i);
            sum += (double) objs[1];
        }
        //去年12月数据
        List year2 = getMongthlyIncomeByYear((Integer.valueOf(year) - 1) + "");
        //计算环比
        JSONArray m2m = new JSONArray();
        for (int i = 0; i < yearList.size(); i++) {
            Object[] objs1 = (Object[]) yearList.get(i);
            String month1 = (String) objs1[0];
            double value1 = (double) objs1[1];
            double value2 = 0;
            if (i < yearList.size() - 1) {
                Object[] objs2 = (Object[]) yearList.get(i + 1);
                String month2 = (String) objs2[0];
                if (Integer.valueOf(month2.split("-")[1]) == (Integer.valueOf(month1.split("-")[1]) - 1)) {
                    value2 = (double) objs2[1];
                }
            } else {
                if (year2.size() > 0) {
                    Object[] objs2 = (Object[]) year2.get(0);
                    String month2 = (String) objs2[0];
                    if (Integer.valueOf(month2.split("-")[1]) == 12) {
                        value2 = (double) objs2[1];
                    }
                }
            }
            double value = 0;
            if (value2 > 0) {
                value = (double) Math.round((value1 - value2) / value2 * 100) / 100;
            }
            Object[] res = {month1, value};
            m2m.add(res);
        }
        //计算同比
        JSONArray y2y = new JSONArray();
        for (int i = 0; i < yearList.size(); i++) {
            Object[] objs1 = (Object[]) yearList.get(i);
            String month1 = (String) objs1[0];
            double value1 = (double) objs1[1];
            double value2 = 0;
            String month = month1.split("-")[1];
            for (int j = 0; j < year2.size(); j++) {
                Object[] objs2 = (Object[]) year2.get(j);
                String month2 = (String) objs2[0];
                if (month2.split("-")[1].equals(month)) {
                    value2 = (double) objs2[1];
                    break;
                }
            }
            double value = 0;
            if (value2 > 0) {
                value = (double) Math.round((value1 - value2) / value2 * 100) / 100;
            }
            Object[] res = {month1, value};
            y2y.add(res);
        }
        JSONObject res = new JSONObject();
        res.put("sum", sum);
        res.put("year", yearList);
        res.put("M2M", m2m);
        res.put("Y2Y", y2y);
        return res;
    }


    public List<Map<String, Object>> exportIncomeByYearExcel(String year) {
        List<Map<String, Object>> result = new ArrayList<>();
        JSONObject data = getIncomeByYear(year);

        List yearList = (List) data.get("year");
        Map<String, Object> yearMap = new HashMap<>();
        yearMap.put("time", "营业额");
        for (int i = 0; i < yearList.size(); i++) {
            Object[] res = (Object[]) yearList.get(i);
            String month = (String) res[0];
            yearMap.put(month, res[1]);
        }
        result.add(yearMap);

        JSONArray m2m = (JSONArray) data.get("M2M");
        Map<String, Object> m2mMap = new HashMap<>();
        m2mMap.put("time", "环比增长率");
        for (int i = 0; i < m2m.size(); i++) {
            Object[] res = (Object[]) m2m.get(i);
            String month = (String) res[0];
            m2mMap.put(month, res[1]);
        }
        result.add(m2mMap);

        JSONArray y2y = (JSONArray) data.get("Y2Y");
        Map<String, Object> y2yMap = new HashMap<>();
        y2yMap.put("time", "同比增长率");
        for (int i = 0; i < y2y.size(); i++) {
            Object[] res = (Object[]) y2y.get(i);
            String month = (String) res[0];
            y2yMap.put(month, res[1]);
        }
        result.add(y2yMap);

        return result;
    }


    /**
     * 营业汇总-网点
     *
     * @param storeId
     * @param startTime
     * @param endTime
     * @return
     */
    public JSONObject getIncomeByStore(String storeId, String startTime, String endTime) throws ParseException {
        JSONObject result = new JSONObject();
        if (StringUtils.isEmpty(storeId)) {//全部网点统计：各网点的营业总额
            StringBuffer sql = new StringBuffer();
            sql.append("SELECT s.name,SUM(co.actualPrice) FROM consumerorder co \n" +
                    "LEFT JOIN store s ON s.id = co.storeId \n" +
                    "WHERE \n" +
                    "co.delStatus = FALSE AND (co.state = 3 or co.state = 2) AND co.payState = 2 ")
                    .append("AND co.createTime > '").append(startTime).append("' \n" +
                    "AND co.createTime < '").append(endTime).append("'\n" +
                    "GROUP BY co.storeId");
            EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
            Query nativeQuery = em.createNativeQuery(sql.toString());

            nativeQuery.unwrap(NativeQuery.class);

            @SuppressWarnings({"unused", "unchecked"})
            List res = nativeQuery.getResultList();
            em.close();
            //排序
            for (int i = 0; i < res.size() - 1; i++) {
                for (int j = i + 1; j < res.size(); j++) {
                    Object[] oo1 = (Object[]) res.get(i);
                    Double value1 = (Double) oo1[1];

                    Object[] oo2 = (Object[]) res.get(j);
                    Double value2 = (Double) oo2[1];
                    if (value1 < value2) {
                        Collections.swap(res, i, j);
                    }
                }
            }
            BigDecimal sum = new BigDecimal("0");
            for (Object o :
                    res) {
                Object[] oo = (Object[]) o;
                BigDecimal bg = new BigDecimal((Double) oo[1]);
                sum = sum.add(bg);
            }
            List resList = new ArrayList();
            for (Object o :
                    res) {
                Object[] oo = (Object[]) o;
                BigDecimal bg = new BigDecimal((Double) oo[1]);
                Double value = bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                Object[] or = {oo[0], value, sum.doubleValue() > 0 ? bg.divide(sum, 2, BigDecimal.ROUND_HALF_UP) : 0};
                resList.add(or);
            }
            result.put("list", resList);
            result.put("sum", sum.doubleValue());
        } else {//单个网点下项目统计
            StringBuffer sql = new StringBuffer();
            sql.append("SELECT t.projectId,SUM(t.actualPrice) FROM (\n" +
                    "SELECT co.*,cpi.projectId FROM `consumerorder` co \n" +
                    "LEFT JOIN consumerprojectinfo cpi ON co.id = cpi.consumerOrderId\n" +
                    "WHERE co.storeId = '")
                    .append(storeId).append("' AND co.orderType = 2 AND (co.state = 3 or co.state = 2) AND co.delStatus = FALSE\n" +
                    "AND co.createTime >= '")
                    .append(startTime).append("' AND co.createTime <= '").append(endTime).append("'\n" +
                    ") t GROUP BY t.projectId");
            EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
            Query nativeQuery = em.createNativeQuery(sql.toString());
            nativeQuery.unwrap(NativeQuery.class);
            @SuppressWarnings({"unused", "unchecked"})
            List res = nativeQuery.getResultList();
            em.close();
            //排序：由大到小
            for (int i = 0; i < res.size() - 1; i++) {
                for (int j = i + 1; j < res.size(); j++) {
                    Object[] oo1 = (Object[]) res.get(i);
                    Double value1 = (Double) oo1[1];

                    Object[] oo2 = (Object[]) res.get(j);
                    Double value2 = (Double) oo2[1];
                    if (value1 < value2) {
                        Collections.swap(res, i, j);
                    }
                }
            }
            BigDecimal sum = new BigDecimal("0");
            for (Object o :
                    res) {
                Object[] oo = (Object[]) o;
                BigDecimal bg = new BigDecimal((Double) oo[1]);
                sum = sum.add(bg);
            }
            logger.info("总额：{}", sum.doubleValue());
            //返回数据结构：项目名+服务商+总额
            List resList = new ArrayList();
            for (Object o :
                    res) {
                Object[] oo = (Object[]) o;
                //处理金额
                BigDecimal bg = new BigDecimal((Double) oo[1]);
                Double value = bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                //处理项目id
                String projectId = (String) oo[0];
                Project project = projectRepository.findById(projectId).orElse(null);
                String projectName = "";
                String serviceProviderName = "";
                if (null != project) {
                    projectName = project.getName();
                } else {
                    RSPProject rspProject = rspProjectRepository.findById(projectId).orElse(null);
                    if (null != rspProject) {
                        projectName = rspProject.getName();
                        RealServiceProvider realServiceProvider = realServiceProviderRepository.findById(rspProject.getRspId()).orElse(null);
                        if (null != realServiceProvider) {
                            serviceProviderName = realServiceProvider.getName();
                        }
                    }
                }
                Object[] r0 = {projectName, serviceProviderName, value, sum.doubleValue() > 0 ? (bg.divide(sum, 2, BigDecimal.ROUND_HALF_UP)).doubleValue() : 0};
                resList.add(r0);
            }
            result.put("list", resList);
            result.put("sum", sum.doubleValue());
        }
        return result;
    }

    public JSONObject getIncomeByRsp(String rspId, String startTime, String endTime) {
        JSONObject result = new JSONObject();
        if (StringUtils.isEmpty(rspId)) {//全部服务商
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT \n" +
                    "r.name ,SUM(r.actualPrice) FROM\n" +
                    "( SELECT \n" +
                    "co.actualPrice,\n" +
                    "rsp.name\n" +
                    "FROM consumerorder co\n" +
                    "LEFT JOIN consumerprojectinfo cpi ON cpi.consumerorderId = co.id\n" +
                    "LEFT JOIN rspproject p ON cpi.projectId = p.id\n" +
                    "LEFT JOIN realserviceprovider rsp ON rsp.id = p.rspId\n" +
                    "WHERE \n" +
                    "co.delStatus = FALSE AND (co.state = 3 or co.state = 2)  AND co.payState = 2) r GROUP BY r.name");
            EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
            Query nativeQuery = em.createNativeQuery(sql.toString());
            nativeQuery.unwrap(NativeQuery.class);
            @SuppressWarnings({"unused", "unchecked"})
            List res = nativeQuery.getResultList();
            em.close();
            List resList0 = new ArrayList();
            for (Object o :
                    res) {
                Object[] oo1 = (Object[]) o;
                if (StringUtils.isEmpty(oo1[0])) continue;
                resList0.add(oo1);
            }
            for (int i = 0; i < resList0.size() - 1; i++) {
                for (int j = i + 1; j < resList0.size(); j++) {
                    Object[] oo1 = (Object[]) resList0.get(i);
                    Double value1 = (Double) oo1[1];

                    Object[] oo2 = (Object[]) resList0.get(j);
                    Double value2 = (Double) oo2[1];
                    if (value1 < value2) {
                        Collections.swap(resList0, i, j);
                    }
                }
            }
            BigDecimal sum = new BigDecimal("0");
            for (Object o :
                    resList0) {
                Object[] oo = (Object[]) o;
                BigDecimal bg = new BigDecimal((Double) oo[1]);
                sum = sum.add(bg);
            }
            List resList = new ArrayList();
            for (Object o :
                    resList0) {
                Object[] oo = (Object[]) o;
                BigDecimal bg = new BigDecimal((Double) oo[1]);
                Double value = bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                Object[] or = {oo[0], value, sum.doubleValue() > 0 ? bg.divide(sum, 2, BigDecimal.ROUND_HALF_UP) : 0};
                resList.add(or);
            }
            //统计服务商
            result.put("list", resList);
            result.put("sum", sum.doubleValue());
        } else {//指定服务商
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT r.name,sum(r.actualPrice) FROM (\n" +
                    "SELECT \n" +
                    "s.name,\n" +
                    "co.storeId,\n" +
                    "co.actualPrice\n" +
                    "FROM consumerorder co\n" +
                    "LEFT JOIN consumerprojectinfo cpi ON cpi.consumerorderId = co.id\n" +
                    "LEFT JOIN store s on s.id = co.storeId\n" +
                    "LEFT JOIN rspproject p ON cpi.projectId = p.id\n" +
                    "LEFT JOIN realserviceprovider rsp ON rsp.id = p.rspId\n" +
                    "WHERE \n" +
                    "co.delStatus = FALSE AND (co.state = 3 or co.state = 2)  AND co.payState = 2 AND rsp.id = '").append(rspId).append("'\n" +
                    ") r GROUP BY r.name");
            EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
            Query nativeQuery = em.createNativeQuery(sql.toString());
            nativeQuery.unwrap(NativeQuery.class);
            @SuppressWarnings({"unused", "unchecked"})
            List res = nativeQuery.getResultList();
            em.close();
            for (Object o :
                    res) {
                Object[] oo1 = (Object[]) o;
                if (StringUtils.isEmpty(oo1[0])) {
                    res.remove(oo1);
                }
            }
            for (int i = 0; i < res.size() - 1; i++) {
                for (int j = i + 1; j < res.size(); j++) {
                    Object[] oo1 = (Object[]) res.get(i);
                    Double value1 = (Double) oo1[1];

                    Object[] oo2 = (Object[]) res.get(j);
                    Double value2 = (Double) oo2[1];
                    if (value1 < value2) {
                        Collections.swap(res, i, j);
                    }
                }
            }
            BigDecimal sum = new BigDecimal("0");
            for (Object o :
                    res) {
                Object[] oo = (Object[]) o;
                BigDecimal bg = new BigDecimal((Double) oo[1]);
                sum = sum.add(bg);
            }
            List resList = new ArrayList();
            for (Object o :
                    res) {
                Object[] oo = (Object[]) o;
                BigDecimal bg = new BigDecimal((Double) oo[1]);
                Double value = bg.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                Object[] or = {oo[0], value, sum.doubleValue() > 0 ? bg.divide(sum, 2, BigDecimal.ROUND_HALF_UP) : 0};
                resList.add(or);
            }
            //统计服务商
            result.put("list", resList);
            result.put("sum", sum.doubleValue());
        }
        return result;
    }


    /**
     * 获取某门店实际收入
     *
     * @param storeId
     * @param isMember null：所有；true：会员消费；false：散客消费
     * @return
     * @throws ArgumentMissingException
     */
    public BigDecimal sumStoreIncome(String storeId, Boolean isMember, String startTime, String endTime) throws ArgumentMissingException {

        Map result;
        if (StringUtils.hasText(storeId)) {
            if (null == isMember) {
                result = consumerOrderRepository.sumAllIncomeForOneStore(storeId, startTime, endTime);
            } else {
                if (isMember) {
                    result = consumerOrderRepository.sumIncomeForOneStoreByMember(storeId, 1, startTime, endTime);
                } else {
                    result = consumerOrderRepository.sumIncomeForOneStoreByMember(storeId, 0, startTime, endTime);
                }
            }
        } else {
//            throw new ArgumentMissingException("参数storeId值为空");
            if (null == isMember) {
                result = consumerOrderRepository.sumAllIncomeForAllStore(startTime, endTime);
            } else {
                if (isMember) {
                    result = consumerOrderRepository.sumIncomeForAllStoreByMember(1, startTime, endTime);
                } else {
                    result = consumerOrderRepository.sumIncomeForAllStoreByMember(0, startTime, endTime);
                }
            }
        }
        BigDecimal income = (BigDecimal) result.get("result");
        if (null == income) {
            income = BigDecimal.valueOf(0);
        }
        return income;
    }

    /**
     * 获取门店的收入总计（总体、会员、散客）
     *
     * @param storeId
     * @param startTime
     * @param endTime
     * @return
     * @throws ArgumentMissingException
     */
    public JSONObject getStoreIncome(String storeId, String startTime, String endTime) throws ArgumentMissingException {
        BigDecimal allActualIncome = sumStoreIncome(storeId, null, startTime, endTime);
        BigDecimal memberActualIncome = sumStoreIncome(storeId, true, startTime, endTime);
        BigDecimal nonMemberActualIncome = sumStoreIncome(storeId, false, startTime, endTime);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("allActualIncome", allActualIncome);
        jsonObject.put("memberActualIncome", memberActualIncome);
        jsonObject.put("nonMemberActualIncome", nonMemberActualIncome);
        return jsonObject;
    }

    /**
     * 获取某门店、某种支付方式的总和
     *
     * @param storeId
     * @param payMethodCode
     * @return
     * @throws ArgumentMissingException
     */
    public double sumIncomeForOneStoreByPayMethod(String storeId, int payMethodCode, String startTime, String endTime) throws ArgumentMissingException {
        BigDecimal firstIncome;
        BigDecimal secondIncome;
        Map firstIncomeResult;
        Map secondIncomeResult;
        if (StringUtils.hasText(storeId)) {
            firstIncomeResult = consumerOrderRepository.sumFirstIncomeForOneStoreByPayMethod(storeId, payMethodCode, startTime, endTime);
            secondIncomeResult = consumerOrderRepository.sumSecondIncomeForOneStoreByPayMethod(storeId, payMethodCode, startTime, endTime);

        } else {
//            throw new ArgumentMissingException("参数storeId值为空");
            firstIncomeResult = consumerOrderRepository.sumFirstIncomeForAllStoreByPayMethod(payMethodCode, startTime, endTime);
            secondIncomeResult = consumerOrderRepository.sumSecondIncomeForAllStoreByPayMethod(payMethodCode, startTime, endTime);
        }

        firstIncome = (BigDecimal) firstIncomeResult.get("result");
        secondIncome = (BigDecimal) secondIncomeResult.get("result");
        if (null == firstIncome) {
            firstIncome = BigDecimal.valueOf(0);
        }
        if (null == secondIncome) {
            secondIncome = BigDecimal.valueOf(0);
        }

        double result = firstIncome.doubleValue() + secondIncome.doubleValue();
        return RoundTool.round(result, 2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 获取某门店所有支付方式的总和
     *
     * @param storeId
     * @param startTime
     * @param endTime
     * @return
     * @throws ArgumentMissingException
     */
    public JSONObject getAllPayMethodIncomeForOneStore(String storeId, String startTime, String endTime) throws ArgumentMissingException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("cash", sumIncomeForOneStoreByPayMethod(storeId, Constants.PayMethod.CASH.getCode(), startTime, endTime));
        jsonObject.put("creditCard", sumIncomeForOneStoreByPayMethod(storeId, Constants.PayMethod.CREDIT_CARD.getCode(), startTime, endTime));
        jsonObject.put("suningPay", sumIncomeForOneStoreByPayMethod(storeId, Constants.PayMethod.SUNING_PAY.getCode(), startTime, endTime));
        jsonObject.put("alipay", sumIncomeForOneStoreByPayMethod(storeId, Constants.PayMethod.ALIPAY.getCode(), startTime, endTime));
        jsonObject.put("wechatPay", sumIncomeForOneStoreByPayMethod(storeId, Constants.PayMethod.WECHAT_PAY.getCode(), startTime, endTime));
        jsonObject.put("card", sumIncomeForOneStoreByPayMethod(storeId, Constants.PayMethod.CARD.getCode(), startTime, endTime));
        return jsonObject;
    }

    /**
     * 获取按项目的收入统计饼图数据
     *
     * @param storeId
     * @param startTime
     * @param endTime
     * @return
     * @throws ArgumentMissingException
     */
    public List<ProjectPieChart> getProjectPieChart(String storeId, String startTime, String endTime) throws ArgumentMissingException {
        StringBuffer sql = new StringBuffer();

        sql.append(" SELECT cpi.projectId, cpi.projectName, ROUND(sum( cpi.price ), 2 ) AS projectPrice, ROUND( sum( cpi.price ) / t.totalPrice, 4 ) AS percent, COUNT(1) AS `count` FROM consumerprojectinfo cpi LEFT JOIN consumerorder co ON cpi.consumerOrderId = co.id, (SELECT round( sum( cpi.price ), 2 ) AS totalPrice FROM consumerprojectinfo cpi LEFT JOIN consumerorder co ON cpi.consumerOrderId = co.id WHERE cpi.delStatus = 0 ");
        if (StringUtils.hasText(storeId)) {
            sql.append(" AND co.storeId = '" + storeId + "' ");
        }
        sql.append(" AND co.payState = 2 AND co.createTime >= '" + startTime + "' AND co.createTime <= '" + endTime + "' ) AS t WHERE cpi.delStatus = 0 ");
        if (StringUtils.hasText(storeId)) {
            sql.append(" AND co.storeId = '" + storeId + "' ");
        }
        sql.append(" AND co.payState = 2 AND co.createTime >= '" + startTime + "' AND co.createTime <= '" + endTime + "' GROUP BY cpi.projectId ");


//        if (StringUtils.isEmpty(storeId)) {
//            throw new ArgumentMissingException("参数storeId为空值，无法查询流水明细");
//        }


        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());

        nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(ProjectPieChart.class));


        @SuppressWarnings({"unused", "unchecked"})
        List<ProjectPieChart> projectPieCharts = nativeQuery.getResultList();

        //关闭em
        em.close();

        return projectPieCharts;
    }

    /**
     * 查询员工的智能柜服务订单数
     *
     * @param staffId
     * @return
     * @throws ArgumentMissingException
     */
    public int getStaffOrderServiced(String staffId) throws ArgumentMissingException {
        if (StringUtils.isEmpty(staffId)) {
            throw new ArgumentMissingException("查询员工的智能柜服务订单数失败：参数staffId为空值");
        }
        return consumerOrderRepository.countAllByPickCarStaffIdAndDelStatusAndOrderType(staffId, Constants.DelStatus.NORMAL.isValue(), Constants.OrderType.ARK.getValue());

    }

    public List<HistoryOrder> listHistoryOrder(String employeeId, String keyword) throws ArgumentMissingException, ObjectNotFoundException {
        if (StringUtils.isEmpty(employeeId)) {
            throw new ArgumentMissingException("查询失败：参数staffId为空值");
        }
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (null == employee) {
            throw new ObjectNotFoundException("未找到id为：" + employeeId + " 的技师信息");
        }
        String phone = employee.getPhone();//手机作为技师的唯一标识
        List<Staff> staffList = staffService.findByPhone(phone);
        StringBuilder staffIdsb = new StringBuilder();
        for (Staff staff :
                staffList) {
            String staffId = staff.getId();
            staffIdsb.append(",").append("'").append(staffId).append("'");
        }
        String staffIdStr = staffIdsb.toString();
        staffIdStr = staffIdStr.substring(1);

        StringBuilder sql = new StringBuilder();

        sql.append(" select co.id, co.clientName,co.phone,co.comment,\n" +
                "co.licensePlate,\n" +
                "co.carColor,\n" +
                "co.carImageUrl, \n" +
                "co.carBrand, \n" +
                "co.finishTime,\n" +
                "co.staffKeyLocation as keyLocation,\n" +
                "co.parkingLocation,\n" +
                "case co.payState when 1 then '待支付' else '已交付' end as payState ,\n" +
                "(SELECT GROUP_CONCAT(rsp.name) FROM realserviceprovider rsp WHERE rsp.id IN (SELECT p.rspId FROM rspproject p WHERE p.id IN (SELECT cpi.projectId FROM consumerProjectInfo cpi WHERE cpi.consumerOrderId = co.id AND cpi.delStatus=0)) GROUP BY rsp.name) AS rspName," +
                "(SELECT  group_concat(cpi.projectName) as names FROM consumerprojectinfo cpi WHERE cpi.consumerOrderId = co.id AND cpi.delStatus = FALSE) as projectNames" +
                " from consumerorder co where co.delStatus = 0 and (state = 2 or state = 3) and co.pickCarStaffId in (").append(staffIdStr).append(") ");
        if (StringUtils.hasText(keyword)) {
            sql.append(" and (co.licensePlate like '%").append(keyword).append("%' or co.id like '%").append(keyword).append("%') ");
        }
        sql.append(" order by co.createTime desc ");

        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());

        nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(HistoryOrder.class));


        @SuppressWarnings({"unused", "unchecked"})
        List<HistoryOrder> historyOrders = nativeQuery.getResultList();

        //关闭em
        em.close();

        return historyOrders;
    }

    public ResultJsonObject rechargeCardPaySuccess(String orderId) throws ObjectNotFoundException, ArgumentMissingException, UpdateDataErrorException {
        ConsumerOrder consumerOrder = consumerOrderRepository.findById(orderId).orElse(null);
        if (null == consumerOrder) {
            throw new ObjectNotFoundException("订单数据未查询到，无法更新卡券状态和订单状态");
        }
        consumerOrder.setPayState(Constants.PayState.FINISH_PAY.getValue());
        //设置支付方式一为微信支付，且支付方式一的金额为订单总体实付金额
        consumerOrder.setFirstPayMethod(Constants.PayMethod.WECHAT_PAY.getCode());
        consumerOrder.setFirstActualPrice(consumerOrder.getActualPrice());

        String cardOrCouponId = consumerOrder.getCardOrCouponId();
        if (StringUtils.isEmpty(cardOrCouponId)) {
            throw new ArgumentMissingException("订单中的卡券ID为空，无法更新订单状态");
        }
        Card card = cardRepository.findById(cardOrCouponId).orElse(null);
        if (null == card) {
            throw new ObjectNotFoundException("未找到会员卡的对象，无法更新充值卡状态和订单状态");
        }

        // 更新余额和会员卡有效期
        com.freelycar.saas.project.entity.CardService cardServiceObject = cardServiceRepository.findById(card.getCardServiceId()).orElse(null);
        if (null == cardServiceObject) {
            throw new ObjectNotFoundException("未找到充值信息，无法更新充值卡状态和订单状态");
        }
        float rechargePrice = cardServiceObject.getActualPrice();

        double cardBalance;
        Float balance = cardRepository.sumBalanceByClientId(consumerOrder.getClientId());
        if (null != balance) {
            cardBalance = balance.doubleValue();
        } else {
            cardBalance = 0.0;
        }

        card.setBalance(RoundTool.round((float) cardBalance + rechargePrice, 2, BigDecimal.ROUND_HALF_UP));
        card.setExpirationDate(TimestampUtil.getExpirationDateForYear(cardServiceObject.getValidTime()));
        cardRepository.save(card);

        //更新订单状态
        ConsumerOrder orderRes = updateOrder(consumerOrder);
        if (null == orderRes) {
            throw new UpdateDataErrorException("单据数据更新失败，无法更新充值卡状态和订单状态");
        }

        return ResultJsonObject.getDefaultResult(orderId);
    }

    /**
     * 用户是否享受第一次优惠政策
     * 根据用户的手机号码，查询是否存在有效的智能柜（已支付）订单
     *
     * @param phone
     * @return
     * @throws ArgumentMissingException
     */
    public boolean userHasPreferentialPolicy(String phone) throws ArgumentMissingException {
        if (StringUtils.isEmpty(phone)) {
            throw new ArgumentMissingException("参数phone为空");
        }

        int count = consumerOrderRepository.countAllByPhoneAndDelStatusAndOrderTypeAndPayState(phone, Constants.DelStatus.NORMAL.isValue(), Constants.OrderType.ARK.getValue(), Constants.PayState.FINISH_PAY.getValue());
        if (count == 0) {
            return true;
        }
        return false;
    }

    public JSONObject getStaffKeyLocation(String orderId) throws ArgumentMissingException, ObjectNotFoundException {
        if (StringUtils.isEmpty(orderId)) {
            throw new ArgumentMissingException("参数orderId为空");
        }
        ConsumerOrder order = consumerOrderRepository.findById(orderId).orElse(null);
        if (null == order) {
            throw new ObjectNotFoundException("未找到id为：" + orderId + " 的订单");
        }
        String staffKeyLocation = order.getStaffKeyLocation();
        String staffKeyLocationSn = order.getStaffKeyLocationSn();
        if (StringUtils.isEmpty(staffKeyLocation)) {
            throw new ObjectNotFoundException("未查询到订单中的柜子信息，订单数据有误，请联系管理员或稍后重试");
        }
        JSONObject res = new JSONObject();
        res.put("staffKeyLocation", staffKeyLocation);
        res.put("staffKeyLocationSn", staffKeyLocationSn);
        return res;
    }

    /**
     * 统计订单数量
     */
    public Long countArkOrder(String storeId, String refDate) throws ArgumentMissingException {
        if (StringUtils.isEmpty(refDate)) {
            throw new ArgumentMissingException();
        }

        StringBuilder sql = new StringBuilder();

        sql.append(" select count(1) from consumerorder where delstatus=0 and orderType=2 and state<4 ")
                .append(" and createTime>\"").append(refDate).append(" 00:00:00\" ")
                .append(" and createTime<\"").append(refDate).append(" 23:59:59\" ");
        if (StringUtils.hasText(storeId)) {
            sql.append(" and storeId=\"").append(storeId).append("\" ");
        }

        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());

        nativeQuery.unwrap(NativeQuery.class);


        @SuppressWarnings({"unused", "unchecked"})
        BigInteger res = (BigInteger) nativeQuery.getSingleResult();

        //关闭em
        em.close();

        return res.longValue();
    }

    public ResultJsonObject EdaijiaList(Integer currentPage, Integer pageSize) {
        List<String> stringList = eOrderRepository.findDistinctConsumerOrderId();
        stringList.removeAll(Collections.singleton(null));
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "createTime"));
        Page<ConsumerOrder> page = consumerOrderRepository.findByIdIn(stringList, new PageRequest(currentPage - 1, pageSize, new Sort(orders)));
        return ResultJsonObject.getDefaultResult(PaginationRJO.of(page));
    }
}
