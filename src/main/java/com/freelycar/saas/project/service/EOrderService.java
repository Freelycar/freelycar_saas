package com.freelycar.saas.project.service;

import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.project.entity.*;
import com.freelycar.saas.project.repository.EOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author puyuting
 * @date 2020/1/3
 * @email 2630451673@qq.com
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class EOrderService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${edaijia.channel}")
    private Integer channel;
    @Value("${edaijia.customerId}")
    private String customerId;

    @Autowired
    private ClientService clientService;
    @Autowired
    private CarService carService;
    @Autowired
    private ArkService arkService;
    @Autowired
    private ServiceProviderService serviceProviderService;

    @Autowired
    private EOrderRepository eOrderRepository;

    public EOrder create(String arkSn,String carId,String clientId,String serviceProviderId) throws ObjectNotFoundException {
        //固定参数：渠道、商户id、订单类型、订单成单类型
        EOrder order = new EOrder(channel,customerId,1,0);

        //获取车辆信息
        Car carInfo = carService.findById(carId);
        if (null == carInfo) {
            logger.error("未找到对应的车辆信息 " + carId);
            throw new ObjectNotFoundException("未找到对应的车辆信息");
        }
        order.setCarNo(carInfo.getLicensePlate());  //车牌号

        //获取客户信息
        Client clientInfo = clientService.findById(clientId);
        if (null == clientInfo) {
            logger.error("未找到对应的车主信息 " + clientId);
            throw new ObjectNotFoundException("未找到对应的车主信息");
        }
        order.setCreateMobile(clientInfo.getPhone());   //下单人手机号
        order.setMobile(clientInfo.getPhone());         //车主手机号
        order.setPickupContactPhone(clientInfo.getPhone()); //取车地址联系人手机号

        order.setUsername(clientInfo.getName());        //车主姓名
        order.setPickupContactName(clientInfo.getName());   //取车地址联系人姓名

        Ark arkInfo = arkService.findByArkSn(arkSn);
        if (null == arkInfo){
            logger.error("未找到对应的智能柜信息 " + clientId);
            throw new ObjectNotFoundException("未找到对应的智能柜信息");
        }
        order.setPickupAddress(arkInfo.getLocation());  //取车地址
        order.setPickupAddressLng(arkInfo.getAddresslng()); //取车地址经度
        order.setPickupAddressLat(arkInfo.getAddresslat()); //取车地址纬度

        ServiceProvider serviceProvider = serviceProviderService.findById(serviceProviderId);
        if (null == serviceProvider){
            logger.error("未找到对应的服务商信息 " + clientId);
            throw new ObjectNotFoundException("未找到对应的服务商信息");
        }
        order.setReturnAddress(serviceProvider.getAddress());       //还车地址
        order.setReturnAddressLng(serviceProvider.getAddressLng()); //还车地址经度
        order.setReturnAddressLat(serviceProvider.getAddressLat()); //还车地址纬度
        order.setReturnContactName(serviceProvider.getName());      //还车地址联系人姓名
        order.setReturnContactPhone(serviceProvider.getPhone());    //还车地址联系人手机号

        return eOrderRepository.saveAndFlush(order);
    }
}
