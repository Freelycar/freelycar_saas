package com.freelycar.saas.util;

import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.exception.NormalException;
import com.freelycar.saas.exception.NumberOutOfRangeException;
import com.freelycar.saas.project.entity.OrderSn;
import com.freelycar.saas.project.entity.Store;
import com.freelycar.saas.project.repository.OrderSnRepository;
import com.freelycar.saas.project.repository.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单ID生成器
 * 订单号生成规则：订单类型编号（1位）+ 门店（3位）+ 日期（6位）+ 每日递增（4位）
 *
 * @author tangwei - Toby
 * @date 2019-01-28
 * @email toby911115@gmail.com
 */
@Component
public class OrderIDGenerator implements ApplicationRunner {
    private static final SimpleDateFormat sdfDate = new SimpleDateFormat("yyMMdd");

    private final static String INIT_SN = "0000";
    private final static String ORIGIN_SN = "0001";
    private final static String HALF_SN = "5000";
    //缓存
    private static Map<String, String> orderSnCacheVariable = new ConcurrentHashMap<>();
    private static Map<String, String> dateNumberCacheVariable = new ConcurrentHashMap<>();
    private static Map<String, String> storeSnCacheVariable = new ConcurrentHashMap<>();
    private static Map<String, String> resOrderSnCacheVariable = new ConcurrentHashMap<>();

    private final String[] orderTypeSn = new String[]{"S", "A", "C", "R"};
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private OrderSnRepository orderSnRepository;

    @Autowired
    private StoreRepository storeRepository;

    private String generateOrderSnWithoutOrderType(String storeId) throws ArgumentMissingException, NumberOutOfRangeException, NormalException {
        if (StringUtils.isEmpty(storeId)) {
            throw new ArgumentMissingException("门店ID为空值，无法生成单据ID");
        }
        String currentDateNumber = sdfDate.format(new Date());
        logger.debug("当前时间日期6位数：" + currentDateNumber);

        synchronized (orderSnCacheVariable) {
            synchronized (resOrderSnCacheVariable) {
                String orderSn = orderSnCacheVariable.get(storeId);
                String dateNumber = dateNumberCacheVariable.get(storeId);
                String storeSn = storeSnCacheVariable.get(storeId);

                if (StringUtils.hasText(dateNumber) && StringUtils.hasText(orderSn) && StringUtils.hasText(storeSn)) {
                    if (!dateNumber.equalsIgnoreCase(currentDateNumber)) {
                        dateNumberCacheVariable.put(storeId, currentDateNumber);
                        dateNumber = currentDateNumber;
                    }
                    String newOrderSn = Number2StringFormatter.format4Number2String(Integer.parseInt(orderSn + 1));
                    orderSnCacheVariable.put(storeId, newOrderSn);


                    String resOrderSn = storeSn + dateNumber + newOrderSn;
                    String lastRes = resOrderSnCacheVariable.get(storeId);
                    resOrderSnCacheVariable.put(storeId, resOrderSn);

                    //递归调用，直到获取到不同的订单编号
                    if (resOrderSn.equalsIgnoreCase(lastRes)) {
                        this.generateOrderSnWithoutOrderType(storeId);
                    }
                    return resOrderSn;
                }
            }
            throw new NormalException("获取该门店订单ID失败");
        }
    }

    public String getOrderSn(String storeId, int orderType) throws ArgumentMissingException, NumberOutOfRangeException, NormalException {
        if (orderType < 1 || orderType > orderTypeSn.length + 1) {
            throw new ArgumentMissingException("参数orderType超过了规则下标，无法生成单据ID");
        }
        String resOrderSn = this.generateOrderSnWithoutOrderType(storeId);

        return orderTypeSn[orderType - 1] + resOrderSn;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("--------开始读取数据库中每个店的最新编号--------");

        String currentDateNumber = sdfDate.format(new Date());
        logger.debug("当前时间日期6位数：" + currentDateNumber);

        List<Store> stores = storeRepository.findAllByDelStatusOrderBySortAsc(Constants.DelStatus.NORMAL.isValue());

        if (null == stores || stores.isEmpty()) {
            logger.info("-----没有读取到有效的门店信息，初始化门店订单编号失败-----");
            return;
        }
        for (Store store : stores) {
            String storeId = store.getId();
            OrderSn orderSn = orderSnRepository.findTopByStoreIdOrderByIdDesc(storeId);
            if (null == orderSn) {
                logger.error("没找到门店ID为:" + storeId + " 的单据规则数据，无法生成单据ID");
            } else {
                String dateNumber = orderSn.getDateNumber();
                String storeSn = orderSn.getStoreSn();
                String orderNumber = orderSn.getOrderNumber();
                if (!currentDateNumber.equalsIgnoreCase(dateNumber)) {
                    dateNumber = currentDateNumber;
                    orderNumber = INIT_SN;
                }
                storeSnCacheVariable.put(storeId, storeSn);
                dateNumberCacheVariable.put(storeId, dateNumber);
                orderSnCacheVariable.put(storeId, orderNumber);
            }
        }
    }

//    synchronized public String generate(String storeId, int orderType) throws Exception {
//        String res;
//        synchronized (this) {
//            if (StringUtils.isEmpty(storeId)) {
//                throw new Exception("门店ID为空值，无法生成单据ID");
//            }
//            if (orderType < 1 || orderType > orderTypeSn.length + 1) {
//                throw new Exception("参数orderType超过了规则下标");
//            }
//            String currentDateNumber = sdfDate.format(new Date());
//            logger.debug("当前时间日期6位数：" + currentDateNumber);
//
//
//            StringBuilder orderIdSn = new StringBuilder().append(orderTypeSn[orderType - 1]);
//
//            OrderSn orderSn = orderSnRepository.findTopByStoreIdAndAndDateNumberOrderByCreateTimeDesc(storeId, currentDateNumber);
//            if (null != orderSn) {
//                String storeSn = orderSn.getStoreSn();
//                orderIdSn.append(storeSn);
//
//                orderIdSn.append(currentDateNumber);
//
//                int currentOrderNumber = Integer.parseInt(orderSn.getOrderNumber());
//                String nextOrderNumber = Number2StringFormatter.format4Number2String(currentOrderNumber + 1);
//                orderIdSn.append(nextOrderNumber);
//
//                orderSn.setOrderNumber(nextOrderNumber);
//                orderSnRepository.save(orderSn);
//            } else {
//                orderSn = orderSnRepository.findTopByStoreIdOrderByCreateTimeDesc(storeId);
//                if (null == orderSn) {
//                    throw new Exception("没找到门店ID为:" + storeId + " 的单据规则数据，无法生成单据ID");
//                }
//
//                OrderSn todayOrderSnObject = new OrderSn();
//                todayOrderSnObject.setStoreId(storeId);
//                todayOrderSnObject.setStoreSn(orderSn.getStoreSn());
//                todayOrderSnObject.setDateNumber(currentDateNumber);
//                todayOrderSnObject.setCreateTime(new Timestamp(System.currentTimeMillis()));
//                todayOrderSnObject.setOrderNumber("0001");
//                OrderSn todayOrderSnObjectRes = orderSnRepository.save(todayOrderSnObject);
//
//                if (null == todayOrderSnObjectRes) {
//                    throw new Exception("生成当日单据规则失败，无法生成单据ID");
//                }
//
//                orderIdSn.append(todayOrderSnObjectRes.getStoreSn())
//                        .append(todayOrderSnObjectRes.getDateNumber())
//                        .append(todayOrderSnObjectRes.getOrderNumber());
//
//            }
//            res = orderIdSn.toString();
//        }
//        return res;
//    }
}
