package com.freelycar.saas.project.service;

import com.freelycar.saas.basic.wrapper.*;
import com.freelycar.saas.exception.CarNumberValidationException;
import com.freelycar.saas.project.entity.Car;
import com.freelycar.saas.project.entity.Card;
import com.freelycar.saas.project.entity.Client;
import com.freelycar.saas.project.entity.Coupon;
import com.freelycar.saas.project.model.CustomerInfo;
import com.freelycar.saas.project.model.CustomerList;
import com.freelycar.saas.project.model.NewClientInfo;
import com.freelycar.saas.project.repository.CarRepository;
import com.freelycar.saas.project.repository.CardRepository;
import com.freelycar.saas.project.repository.ClientRepository;
import com.freelycar.saas.project.repository.CouponRepository;
import com.freelycar.saas.util.MySQLPageTool;
import com.freelycar.saas.util.TimestampUtil;
import com.freelycar.saas.util.UpdateTool;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.*;

/**
 * @author tangwei - Toby
 * @date 2018-12-25
 * @email toby911115@gmail.com
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ClientService {
    private Logger logger = LoggerFactory.getLogger(ClientService.class);
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private CarRepository carRepository;
    @Autowired
    private CardRepository cardRepository;
    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CarService carService;

    @Autowired
    private LocalContainerEntityManagerFactoryBean entityManagerFactory;

    public Client findById(String id) {

        return clientRepository.findById(id).orElse(null);
    }

    /**
     * 同时保存客户信息和车辆信息
     *
     * @param client
     * @param car
     * @return
     */
    public ResultJsonObject addClientAndCar(Client client, Car car) throws CarNumberValidationException {
        //非空验证
        if (null == client || null == car) {
            return ResultJsonObject.getErrorResult("保存失败！客户信息或车辆信息为空！");
        }

        //保存用户信息
        Client clientRes = this.saveOrUpdate(client);
        if (null == clientRes) {
            return ResultJsonObject.getErrorResult("保存失败！保存客户信息失败！");
        }
        String clientId = clientRes.getId();
        car.setClientId(clientId);

        //保存车辆信息
        Car carRes = carService.saveOrUpdate(car);

        if (null == carRes) {
            return ResultJsonObject.getErrorResult("保存失败！保存车辆信息失败！");
        }

        //组装成对应model返回到前台
        NewClientInfo newClientInfo = new NewClientInfo();
        newClientInfo.setClient(clientRes);
        newClientInfo.setCar(carRes);

        return ResultJsonObject.getDefaultResult(newClientInfo);
    }

    /**
     * 保存/修改客户信息
     *
     * @param client
     * @return
     */
    public Client saveOrUpdate(Client client) {
        if (null == client) {
            return null;
        }

        String id = client.getId();
        if (StringUtils.isEmpty(id)) {
            client.setCreateTime(new Timestamp(System.currentTimeMillis()));
            client.setDelStatus(Constants.DelStatus.NORMAL.isValue());
            client.setMember(false);
            client.setPoints(0);
            client.setState(0);
        } else {
            Optional<Client> optionalClient = clientRepository.findById(id);
            if (!optionalClient.isPresent()) {
                return null;
            }
            Client source = optionalClient.get();
            UpdateTool.copyNullProperties(source, client);
        }
        return clientRepository.saveAndFlush(client);
    }

    /**
     * 获取客户详情（不包括会员卡、车辆）
     *
     * @param id
     * @return
     */
    public ResultJsonObject getDetail(String id) {
        return ResultJsonObject.getDefaultResult(clientRepository.findById(id).get());
    }

    /**
     * 获取客户详情（包括会员卡、车辆）
     *
     * @param id
     * @return
     */
    public ResultJsonObject getCustomerInfo(String id) {


        Optional<Client> optionalClient = clientRepository.findById(id);
        if (!optionalClient.isPresent()) {
            return ResultJsonObject.getErrorResult(null, "不存在id为" + id + "的用户。");
        }
        Client client = optionalClient.get();

        List<Car> cars = carRepository.findByClientIdAndDelStatus(id, Constants.DelStatus.NORMAL.isValue());

        List<Card> cards = cardRepository.findByClientIdAndDelStatus(id, Constants.DelStatus.NORMAL.isValue());

        List<Coupon> coupons = couponRepository.findByClientIdAndDelStatus(id, Constants.DelStatus.NORMAL.isValue());

        CustomerInfo customerInfo = new CustomerInfo();
        customerInfo.setClient(client);
        customerInfo.setCar(cars);
        customerInfo.setCard(cards);
        customerInfo.setCoupon(coupons);


        return ResultJsonObject.getDefaultResult(customerInfo);

    }

    /**
     * 获取客户列表
     *
     * @param storeId
     * @param currentPage
     * @param pageSize
     * @param params
     * @return
     */
    public ResultJsonObject list(String storeId, Integer currentPage, Integer pageSize, Map params) {
        String name = null;
        String phone = null;
        String licensePlate = null;
        if (null != params) {
            name = (String) params.get("name");
            phone = (String) params.get("phone");
            licensePlate = (String) params.get("licensePlate");
        }
        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        StringBuilder sql = new StringBuilder();
        Query nativeQuery = null;
        if (StringUtils.hasText(licensePlate) || StringUtils.hasText(name) || StringUtils.hasText(phone)) {
            sql.append("SELECT COUNT(1) \n" +
                    "FROM client c WHERE \n" +
                    "c.storeId = '").append(storeId).append("' \n" +
                    "AND c.delStatus = FALSE ");
            StringBuilder sql3 = new StringBuilder();
            if (StringUtils.hasText(licensePlate)) {
                sql3.append("AND c.id in (SELECT DISTINCT(clientId) FROM car WHERE licensePlate like '%").append(licensePlate).append("%') ");
            }
            if (StringUtils.hasText(name)) {
                sql3.append("AND c.name LIKE '%").append(name).append("%' ");
            }
            if (StringUtils.hasText(phone)) {
                sql3.append("AND c.phone LIKE '%").append(phone).append("%'  ");
            }
            sql.append(sql3);
        } else {
            sql.append("SELECT COUNT(1) \n" +
                    "FROM client c WHERE \n" +
                    "c.storeId = '").append(storeId).append("' \n" +
                    "AND c.delStatus = FALSE");
        }

        nativeQuery = em.createNativeQuery(sql.toString());
        Object result = nativeQuery.getSingleResult();
        StringBuilder sql1 = new StringBuilder();
        sql1.append("SELECT \n" +
                "c.id,\n" +
                "c.name,\n" +
                "c.phone,\n" +
                "(SELECT GROUP_CONCAT(car.carBrand) FROM car WHERE car.clientId = c.id AND delStatus = FALSE) as brands,\n" +
                "(SELECT GROUP_CONCAT(car.licensePlate) FROM car WHERE car.clientId = c.id AND delStatus = FALSE) as plates,\n" +
                "(SELECT createTime FROM consumerorder co WHERE co.clientId = c.id AND co.delStatus = FALSE ORDER BY createTime ASC LIMIT 0,1) as lastVisit,\n" +
                "(SELECT count(1) FROM consumerorder co WHERE co.clientId = c.id AND co.delStatus = FALSE) as totalCount,\n" +
                "IF(c.isMember=0,'否','是') as isMember\n" +
                "FROM client c \n" +
                "WHERE c.storeId = '").append(storeId).append("' \n" +
                "AND c.delStatus = FALSE\n" +
                "ORDER BY c.createTime asc");
        StringBuilder sql2 = new StringBuilder();

        if (StringUtils.hasText(licensePlate) || StringUtils.hasText(name) || StringUtils.hasText(phone)) {
            sql2.append("SELECT * FROM (").append(sql1).append(") as r \n" +
                    "WHERE ");
            StringBuilder sql3 = new StringBuilder();
            if (StringUtils.hasText(licensePlate)) {
                sql3.append("AND r.plates LIKE '%").append(licensePlate).append("%' ");
            }
            if (StringUtils.hasText(name)) {
                sql3.append("AND r.name LIKE '%").append(name).append("%' ");
            }
            if (StringUtils.hasText(phone)) {
                sql3.append("AND r.phone LIKE '%").append(phone).append("%'  ");
            }
            sql2.append(sql3.substring(3));
            nativeQuery = em.createNativeQuery(sql2.toString());
        } else {
            nativeQuery = em.createNativeQuery(sql1.toString());
        }

        nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(CustomerList.class));

        Pageable pageable = PageableTools.basicPage(currentPage, pageSize);
        int total = Integer.valueOf(result.toString());
        @SuppressWarnings({"unused", "unchecked"})
        List<CustomerList> customerInfos = nativeQuery.setFirstResult(MySQLPageTool.getStartPosition(currentPage, pageSize)).setMaxResults(pageSize).getResultList();

        //关闭em
        em.close();

        @SuppressWarnings("unchecked")
        Page<CustomerList> page = new PageImpl(customerInfos, pageable, total);

        return ResultJsonObject.getDefaultResult(PaginationRJO.of(page));
    }

    /**
     * 获取客户列表(待优化)
     *
     * @param storeId
     * @param currentPage
     * @param pageSize
     * @param params
     * @return
     */
    public ResultJsonObject exportList(String storeId, Integer currentPage, Integer pageSize, Map params, boolean export) {
        String name = null;
        String phone = null;
        String licensePlate = null;
        if (null != params) {
            name = (String) params.get("name");
            phone = (String) params.get("phone");
            licensePlate = (String) params.get("licensePlate");
        }
        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        StringBuilder sql1 = new StringBuilder();
        sql1.append("SELECT \n" +
                "c.id,\n" +
                "c.name,\n" +
                "c.phone,\n" +
                "(SELECT GROUP_CONCAT(car.carBrand) FROM car WHERE car.clientId = c.id AND delStatus = FALSE) as brands,\n" +
                "(SELECT GROUP_CONCAT(car.licensePlate) FROM car WHERE car.clientId = c.id AND delStatus = FALSE) as plates,\n" +
                "(SELECT createTime FROM consumerorder co WHERE co.clientId = c.id AND co.delStatus = FALSE ORDER BY createTime ASC LIMIT 0,1) as lastVisit,\n" +
                "(SELECT count(1) FROM consumerorder co WHERE co.clientId = c.id AND co.delStatus = FALSE) as totalCount,\n" +
                "IF(c.isMember=0,'否','是') as isMember\n" +
                "FROM client c \n" +
                "WHERE c.storeId = '").append(storeId).append("' \n" +
                "AND c.delStatus = FALSE\n" +
                "ORDER BY c.createTime asc");
        StringBuilder sql2 = new StringBuilder();
        Query nativeQuery = null;
        if (StringUtils.hasText(licensePlate) || StringUtils.hasText(name) || StringUtils.hasText(phone)) {
            sql2.append("SELECT * FROM (").append(sql1).append(") as r \n" +
                    "WHERE ");
            StringBuilder sql3 = new StringBuilder();
            if (StringUtils.hasText(licensePlate)) {
                sql3.append("AND r.plates LIKE '%").append(licensePlate).append("%' ");
            }
            if (StringUtils.hasText(name)) {
                sql3.append("AND r.name LIKE '%").append(name).append("%' ");
            }
            if (StringUtils.hasText(phone)) {
                sql3.append("AND r.phone LIKE '%").append(phone).append("%'  ");
            }
            sql2.append(sql3.substring(3));
            nativeQuery = em.createNativeQuery(sql2.toString());
        } else {
            nativeQuery = em.createNativeQuery(sql1.toString());
        }

        nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(CustomerList.class));

        List<CustomerList> customerInfos = nativeQuery.getResultList();

        //关闭em
        em.close();

        return ResultJsonObject.getDefaultResult(customerInfos);
    }


    /**
     * 获取门店会员总个数
     *
     * @param storeId
     * @return
     */
    public int memberCount(String storeId) {
        return clientRepository.countByDelStatusAndStoreIdAndIsMember(Constants.DelStatus.NORMAL.isValue(), storeId, true);
    }

    /**
     * 统计当月的会员新增
     *
     * @param storeId
     * @return
     */
    public int memberCountForMonth(String storeId) {
        Calendar calendar = Calendar.getInstance();
        String startTime = TimestampUtil.getFisrtDayOfMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1) + " 00:00:00";
        String endTime = TimestampUtil.getFisrtDayOfMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 2) + " 00:00:00";
        return clientRepository.countByDelStatusAndStoreIdAndIsMemberAndMemberDateBetween(Constants.DelStatus.NORMAL.isValue(), storeId, true, Timestamp.valueOf(startTime), Timestamp.valueOf(endTime));
    }

    /**
     * 统计当天的会员新增
     *
     * @param storeId
     * @return
     */
    public int memberCountForToday(String storeId) {
        String startTime = TimestampUtil.getCurrentDate() + " 00:00:00";
        String endTime = TimestampUtil.getCurrentDate() + " 23:59:59";
        return clientRepository.countByDelStatusAndStoreIdAndIsMemberAndMemberDateBetween(Constants.DelStatus.NORMAL.isValue(), storeId, true, Timestamp.valueOf(startTime), Timestamp.valueOf(endTime));
    }

    /**
     * 门店会员统计
     *
     * @param storeId
     * @return key:value
     */
    public Map<String, Integer> memberStatistics(String storeId) {
        Map<String, Integer> map = new HashMap<>();
        map.put("total", this.memberCount(storeId));
        map.put("month_new", this.memberCountForMonth(storeId));
        map.put("today_new", this.memberCountForToday(storeId));
        return map;
    }

    /**
     * 更新client的累积消费金额
     *
     * @param clientId
     * @param amount
     * @return
     */
    public Client updateClientAcount(String clientId, double amount) {
        if (StringUtils.isEmpty(clientId)) {
            return null;
        }
        Client client = clientRepository.getOne(clientId);
        double consumeAmount = client.getConsumeAmount() == null ? 0 : client.getConsumeAmount();
        client.setConsumeAmount(consumeAmount + amount);
        return clientRepository.saveAndFlush(client);
    }

    /**
     * 根据ID删除客户信息
     *
     * @param id
     * @return
     */
    public ResultJsonObject delete(String id) {
        if (StringUtils.isEmpty(id)) {
            return ResultJsonObject.getErrorResult(null, "删除失败：id" + ResultCode.PARAM_NOT_COMPLETE.message());
        }
        int res = clientRepository.delById(id);
        if (res == 1) {
            return ResultJsonObject.getDefaultResult(id);
        }
        return ResultJsonObject.getErrorResult(null, ResultCode.RESULT_DATA_NONE.message());
    }

    /**
     * 为其他门店自动生成一个client对象
     *
     * @param source
     * @param storeId
     * @return
     */
    public Client copyNewObjectForOtherStore(Client source, String storeId) {
        if (null == source || StringUtils.isEmpty(storeId)) {
            return null;
        }
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());

        Client newClient = new Client();
        //初始化的数据项
        newClient.setCreateTime(currentTime);
        newClient.setLastVisit(currentTime);
        newClient.setMember(false);
        newClient.setConsumeAmount((double) 0);
        newClient.setDelStatus(Constants.DelStatus.NORMAL.isValue());
        newClient.setConsumeTimes(0);
        newClient.setPoints(0);

        //所属门店
        newClient.setStoreId(storeId);

        //其他数据copy原对象
        newClient.setPhone(source.getPhone());
        newClient.setName(source.getName());
        newClient.setNickName(source.getNickName());
        newClient.setTrueName(source.getTrueName());
        newClient.setBirthday(source.getBirthday());
        newClient.setGender(source.getGender());
        newClient.setAge(source.getAge());
        newClient.setRecommendName(source.getRecommendName());
        newClient.setIdNumber(source.getIdNumber());
        newClient.setDriverLicense(source.getDriverLicense());
        newClient.setState(source.getState());

        //返回新对象
        return newClient;
    }
}
