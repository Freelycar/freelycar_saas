package com.freelycar.saas.project.service;

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
import com.freelycar.saas.wechat.model.ReservationOrderInfo;
import com.freelycar.saas.wxutils.WechatTemplateMessage;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
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

        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT co.id, co.licensePlate as licensePlate, co.carBrand as carBrand, co.carType as carType, co.carColor, co.carImageUrl, co.clientName AS clientName,(select c.phone from client c where c.id = co.clientId) as phone, ( SELECT GROUP_CONCAT( cpi.projectName ) FROM consumerProjectInfo cpi WHERE cpi.consumerOrderId = co.id GROUP BY cpi.consumerOrderId ) AS projectNames, co.createTime AS createTime, co.parkingLocation AS parkingLocation, d.arkSn AS arkSn, d.doorSn AS doorSn, concat( ( SELECT ark.`name` FROM ark WHERE ark.id = d.arkId ), '-', d.doorSn, '号门' ) AS keyLocation FROM door d LEFT JOIN consumerOrder co ON co.id = d.orderId WHERE co.state = 0 ")
                .append(" AND co.storeId = '").append(storeId).append("' ");
        if (StringUtils.hasText(licensePlate)) {
            sql.append(" and (co.licensePlate like '%").append(licensePlate.toUpperCase()).append("%' or co.id like '%").append(licensePlate.toUpperCase()).append("%') ");
        }
        sql.append(" ORDER BY co.createTime ASC");

        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());
        nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(ReservationOrderInfo.class));
        @SuppressWarnings({"unused", "unchecked"})
        List<ReservationOrderInfo> reservationOrderInfos = nativeQuery.getResultList();

        //关闭em
        em.close();

        return reservationOrderFilter(reservationOrderInfos, staffId);
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
    public List<FinishOrderInfo> listServicingOrders(String licensePlate, String storeId) {
        StringBuilder sql = new StringBuilder();
        sql.append(" SELECT co.id, co.clientName AS clientName,(select c.phone from client c where c.id = co.clientId) as phone, co.licensePlate as licensePlate, co.carBrand as carBrand, co.carType as carType, co.carColor, co.carImageUrl, ( SELECT GROUP_CONCAT( cpi.projectName ) FROM consumerProjectInfo cpi WHERE cpi.consumerOrderId = co.id GROUP BY cpi.consumerOrderId ) projectNames, co.pickTime as pickTime, co.userKeyLocationSn, co.userKeyLocation FROM consumerOrder co WHERE co.delStatus = 0 AND co.orderType = 2 AND co.state = 1 ")
                .append(" AND co.storeId = '").append(storeId).append("' ");
        if (StringUtils.hasText(licensePlate)) {
            sql.append(" and (co.licensePlate like '%").append(licensePlate).append("%' or co.id like '%").append(licensePlate).append("%') ");
        }
        sql.append(" ORDER BY co.pickTime ASC ");
        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());
        nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(FinishOrderInfo.class));
        @SuppressWarnings({"unused", "unchecked"})
        List<FinishOrderInfo> finishOrderInfo = nativeQuery.getResultList();

        //关闭em
        em.close();

        return finishOrderInfo;
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

        //获取配件
        List<AutoParts> autoPartsList = autoPartsService.getAllAutoPartsByOrderId(id);

        //获取用户上传的智能柜订单图片
        ClientOrderImg clientOrderImg = clientOrderImgRepository.findTopByOrderIdAndDelStatusOrderByCreateTimeDesc(id, Constants.DelStatus.NORMAL.isValue());

        //获取技师上传的智能柜订单图片
        StaffOrderImg staffOrderImg = staffOrderImgRepository.findTopByOrderIdAndDelStatusOrderByCreateTimeDesc(id, Constants.DelStatus.NORMAL.isValue());

        //获取相关的卡信息或券信息
        if (Constants.OrderIdSn.CARD.getName().equals(id.substring(0, 1))) {
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
        }

        orderObject.setConsumerOrder(consumerOrder);
        orderObject.setConsumerProjectInfos(consumerProjectInfos);
        orderObject.setAutoParts(autoPartsList);

        if (null != clientOrderImg) {
            orderObject.setClientOrderImg(clientOrderImg);
        }

        if (null != staffOrderImg) {
            orderObject.setStaffOrderImg(staffOrderImg);
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
            sql.append(" AND co.orderState = ").append(orderState).append(" ");
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
    public ResultJsonObject arkHandleOrder(OrderObject orderObject) throws ArgumentMissingException, ObjectNotFoundException, NoEmptyArkException, OpenArkDoorTimeOutException, InterruptedException, OpenArkDoorFailedException, UpdateDataErrorException {
//        logger.info("执行智能柜开单操作：---start---" + orderObject);
        String doorId = orderObject.getDoorId();
        //获取提交过来的数据
        ConsumerOrder consumerOrder = orderObject.getConsumerOrder();
        List<ConsumerProjectInfo> consumerProjectInfos = orderObject.getConsumerProjectInfos();

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

        //获取客户信息
        Client clientInfo = clientService.findById(clientId);
        if (null == clientInfo) {
            logger.error("未找到对应的车主信息 " + clientId);
            throw new ObjectNotFoundException("未找到对应的车主信息");
        }

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

        //设置车主信息
        consumerOrder.setClientId(clientId);
        consumerOrder.setClientName(clientInfo.getTrueName());
        consumerOrder.setPhone(clientInfo.getPhone());
        consumerOrder.setIsMember(clientInfo.getMember());
        consumerOrder.setStoreId(clientInfo.getStoreId());

        //设置order的其他信息
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        consumerOrder.setOrderType(Constants.OrderType.ARK.getValue());
        consumerOrder.setPayState(Constants.PayState.NOT_PAY.getValue());
        //设置订单状态为“预约”
        consumerOrder.setState(Constants.OrderState.RESERVATION.getValue());

        //计算项目金额
        double totalPrice = consumerProjectInfoService.sumAllProjectPrice(consumerProjectInfos, false);
        consumerOrder.setTotalPrice(totalPrice);
        consumerOrder.setActualPrice(totalPrice);
        //计算会员价总计金额
        double memberPrice = consumerProjectInfoService.sumAllProjectPrice(consumerProjectInfos, true);
        consumerOrder.setMemberPrice(memberPrice);


        // 有效柜子分配逻辑
        Door emptyDoor = (Door) ConcurrentHashMapCacheUtils.getCache(doorId);
        if (null == emptyDoor) {
            throw new ObjectNotFoundException("未找到分配的柜门号，请稍后重试");
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


        //保存订单项目信息
        if (null != consumerProjectInfos && !consumerProjectInfos.isEmpty()) {
            for (ConsumerProjectInfo consumerProjectInfo : consumerProjectInfos) {
                consumerProjectInfo.setConsumerOrderId(orderId);
                consumerProjectInfoService.saveOrUpdate(consumerProjectInfo);
            }
        }

        //关联用户上传的图片
        ClientOrderImg clientOrderImg = orderObject.getClientOrderImg();
        if (null != clientOrderImg) {
            clientOrderImg.setOrderId(orderId);
            clientOrderImg.setCreateTime(currentTime);
            clientOrderImgRepository.save(clientOrderImg);
        }


        // door表数据更新，根据智能柜编号获取door对象，并更新状态为"预约状态"
        this.changeDoorState(emptyDoor, orderId, Constants.DoorState.USER_RESERVATION.getValue());
        // 数据保存完毕之后操作硬件，成功后返回成功，否则抛出异常进行回滚操作
        try {
            doorService.openDoorByDoorObject(emptyDoor);
        } catch (OpenArkDoorFailedException | OpenArkDoorTimeOutException | InterruptedException e) {
            throw e;
        } finally {
            ConcurrentHashMapCacheUtils.deleteCache(doorId);
        }


        //推送微信消息给技师 需要给这个柜子相关的技师都推送
        staffService.sendWeChatMessageToStaff(consumerOrderRes, emptyDoor, null);

        // 推送微信公众号消息，通知用户订单生成成功
        sendWeChatMsg(consumerOrderRes);

//        logger.info("执行智能柜开单操作---end---：" + consumerOrderRes);
        return ResultJsonObject.getDefaultResult(consumerOrderRes.getId(), "订单生成成功！");
    }

    /**
     * 设置订单为取消状态
     *
     * @param orderId
     * @return
     */
    public ResultJsonObject cancelOrder(String orderId) throws ArgumentMissingException, OpenArkDoorFailedException, OpenArkDoorTimeOutException, InterruptedException, ObjectNotFoundException {
        logger.info("执行用户取消订单操作：---start---" + orderId);
        ConsumerOrder consumerOrder = consumerOrderRepository.findById(orderId).orElse(null);
        if (null == consumerOrder) {
            return ResultJsonObject.getErrorResult(null, "未找到id为：" + orderId + " 的订单");
        }
        consumerOrder.setState(Constants.OrderState.CANCEL.getValue());
        ConsumerOrder consumerOrderRes = this.updateOrder(consumerOrder);

        //获取订单对应的柜子信息
        Door door = doorRepository.findTopByOrderId(orderId);
        //更新door表数据
        this.changeDoorState(door, null, Constants.DoorState.EMPTY.getValue());
        //打开柜门
        doorService.openDoorByDoorObject(door);

        //用户取消服务订单的时候推送消息给技师
        staffService.sendWeChatMessageToStaff(consumerOrderRes, door, null);

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
        //更新door表数据
        this.changeDoorState(door, null, Constants.DoorState.EMPTY.getValue());
        //打开柜门
        doorService.openDoorByDoorObject(door);


        //推送微信公众号消息，通知用户服务完全结束
        sendWeChatMsg(res);

        return ResultJsonObject.getDefaultResult(orderId);
    }

    /**
     * 技师去取车，提醒用户订单已经被受理
     *
     * @param orderId
     * @param staffId
     * @return
     */
    public ResultJsonObject pickCar(String orderId, String staffId) throws Exception {
        if (StringUtils.isEmpty(orderId)) {
            return ResultJsonObject.getCustomResult("The param 'orderId' is null", ResultCode.PARAM_NOT_COMPLETE);
        }
        if (StringUtils.isEmpty(staffId)) {
            return ResultJsonObject.getCustomResult("The param 'staffId' is null", ResultCode.PARAM_NOT_COMPLETE);
        }
        ConsumerOrder consumerOrder = consumerOrderRepository.findById(orderId).orElse(null);
        if (null == consumerOrder) {
            return ResultJsonObject.getCustomResult("Not found consumerOrder object by orderId : " + orderId, ResultCode.RESULT_DATA_NONE);
        }

        Staff staff = staffService.findById(staffId);
        if (null == staff) {
            return ResultJsonObject.getCustomResult("Not found staff object by staffId : " + staffId, ResultCode.RESULT_DATA_NONE);
        }

        consumerOrder.setPickTime(new Timestamp(System.currentTimeMillis()));
        consumerOrder.setState(Constants.OrderState.RECEIVE_CAR.getValue());
        consumerOrder.setPickCarStaffId(staffId);
        consumerOrder.setPickCarStaffName(staff.getName());
        ConsumerOrder orderRes = this.updateOrder(consumerOrder);

        //更新door表数据状态
        Door door = doorRepository.findTopByOrderId(orderId);
        this.changeDoorState(door, null, Constants.DoorState.EMPTY.getValue());
        // 调用硬件接口方法打开柜门
        doorService.openDoorByDoorObject(door);

        //推送微信公众号消息，通知用户已开始受理服务
        sendWeChatMsg(orderRes);

        //通知其他技师，该订单已经受理
        String staffOpenId = staff.getOpenId();
        staffService.sendWeChatMessageToStaff(orderRes, door, staffOpenId);

        return ResultJsonObject.getDefaultResult(orderId);
    }

    /**
     * 技师还车，提醒用户来取车
     *
     * @param orderObject
     * @return
     */
    public ResultJsonObject finishCar(OrderObject orderObject) throws ArgumentMissingException, NoEmptyArkException, ObjectNotFoundException, OpenArkDoorTimeOutException, InterruptedException, OpenArkDoorFailedException {
        ConsumerOrder consumerOrder = orderObject.getConsumerOrder();
        String doorId = orderObject.getDoorId();

        if (StringUtils.isEmpty(doorId)) {
            throw new ArgumentMissingException("参数中的doorId对象为空");
        }

        if (null == consumerOrder) {
            throw new ArgumentMissingException("参数中的consumerOrder对象为空");
        }

        String orderId = consumerOrder.getId();
        if (StringUtils.isEmpty(orderId)) {
            return ResultJsonObject.getCustomResult("参数orderId值为null", ResultCode.PARAM_NOT_COMPLETE);
        }


        consumerOrder.setFinishTime(new Timestamp(System.currentTimeMillis()));
        consumerOrder.setState(Constants.OrderState.SERVICE_FINISH.getValue());

        // 有效柜子分配逻辑
        Door emptyDoor = (Door) ConcurrentHashMapCacheUtils.getCache(doorId);
        if (null == emptyDoor) {
            throw new ObjectNotFoundException("未找到分配的柜门号，请稍后重试");
        }
        // 更新技师把钥匙存放在哪个柜子的哪个门
        String staffKeyLocation = emptyDoor.getArkName() + Constants.HYPHEN + emptyDoor.getDoorSn() + "号门";
        String staffKeyLocationSn = emptyDoor.getArkSn() + Constants.HYPHEN + emptyDoor.getDoorSn();
        consumerOrder.setStaffKeyLocation(staffKeyLocation);
        consumerOrder.setStaffKeyLocationSn(staffKeyLocationSn);


        ConsumerOrder order;
        try {
            order = this.updateOrder(consumerOrder);
        } catch (ObjectNotFoundException e) {
            ConcurrentHashMapCacheUtils.deleteCache(doorId);
            throw e;
        }


        //关联技师上传订单车辆图片
        StaffOrderImg staffOrderImg = orderObject.getStaffOrderImg();
        if (null != staffOrderImg) {
            staffOrderImg.setOrderId(orderId);
            staffOrderImgRepository.save(staffOrderImg);
        }


        // 更新door表数据状态
        this.changeDoorState(emptyDoor, orderId, Constants.DoorState.STAFF_FINISH.getValue());
        // 调用硬件接口方法打开柜门
        try {
            doorService.openDoorByDoorObject(emptyDoor);
        } catch (OpenArkDoorFailedException | OpenArkDoorTimeOutException | InterruptedException e) {
            throw e;
        } finally {
            ConcurrentHashMapCacheUtils.deleteCache(doorId);
        }


        // 推送微信公众号消息，通知用户取车
        sendWeChatMsg(order);

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

    public void sendWeChatMsg(ConsumerOrder consumerOrder) {
        //推送微信公众号消息，通知用户已开始受理服务
        String phone = consumerOrder.getPhone();
        String openId = wxUserInfoService.getOpenId(phone);
        if (StringUtils.isEmpty(openId)) {
            logger.error("未获得到对应的openId，微信消息推送失败");
        } else {
            WechatTemplateMessage.orderChanged(consumerOrder, openId);
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
    private void changeDoorState(String arkSn, String doorSn, String orderId, int doorState) throws Exception {
        Door door = doorRepository.findTopByArkSnAndDoorSn(arkSn, doorSn);
        this.changeDoorState(door, orderId, doorState);
    }

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
        doorRepository.save(door);
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

    public List<HistoryOrder> listHistoryOrder(String staffId, String keyword) throws ArgumentMissingException {
        if (StringUtils.isEmpty(staffId)) {
            throw new ArgumentMissingException("查询失败：参数staffId为空值");
        }

        StringBuilder sql = new StringBuilder();

        sql.append(" select co.id, co.licensePlate, co.carColor, co.carImageUrl, co.carBrand, case co.payState when 1 then '待支付' else '已交付' end as payState from consumerorder co where co.delStatus = 0 and co.pickCarStaffId = '").append(staffId).append("' ");
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

    public boolean userHasPreferentialPolicy(String phone) throws ArgumentMissingException {
        if (StringUtils.isEmpty(phone)) {
            throw new ArgumentMissingException("参数phone为空");
        }

        int count = consumerOrderRepository.countAllByPhoneAndDelStatus(phone, Constants.DelStatus.NORMAL.isValue());
        if (count == 0) {
            return true;
        }
        return false;
    }

    public String getStaffKeyLocation(String orderId) throws ArgumentMissingException, ObjectNotFoundException {
        if (StringUtils.isEmpty(orderId)) {
            throw new ArgumentMissingException("参数orderId为空");
        }
        ConsumerOrder order = consumerOrderRepository.findById(orderId).orElse(null);
        if (null == order) {
            throw new ObjectNotFoundException("未找到id为：" + orderId + " 的订单");
        }
        String staffKeyLocation = order.getStaffKeyLocation();
        if (StringUtils.isEmpty(staffKeyLocation)) {
            throw new ObjectNotFoundException("未查询到订单中的柜子信息，订单数据有误，请联系管理员或稍后重试");
        }
        return staffKeyLocation;
    }
}
