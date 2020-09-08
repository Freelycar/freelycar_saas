package com.freelycar.saas.project.service;

import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.exception.DataIsExistException;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.project.entity.Project;
import com.freelycar.saas.project.entity.RealServiceProvider;
import com.freelycar.saas.project.entity.ServiceProvider;
import com.freelycar.saas.project.repository.RealServiceProviderRepository;
import com.freelycar.saas.util.UpdateTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.Optional;

@Service
@Transactional(rollbackFor = Exception.class)
public class RealServiceProviderService {
    private Logger logger = LoggerFactory.getLogger(RealServiceProviderService.class);

    private RealServiceProviderRepository realServiceProviderRepository;

    @Autowired
    public void setRealServiceProviderRepository(RealServiceProviderRepository realServiceProviderRepository) {
        this.realServiceProviderRepository = realServiceProviderRepository;
    }

    /**
     * 新增或修改
     *
     * @param serviceProvider
     */
    public RealServiceProvider modify(RealServiceProvider serviceProvider) throws DataIsExistException, ObjectNotFoundException {
        String id = serviceProvider.getId();
        Optional<RealServiceProvider> optional = realServiceProviderRepository.findByName(serviceProvider.getName());
        //1.判断新增/修改
        if (StringUtils.isEmpty(id)) {//新增
            if (optional.isPresent()) {
                throw new DataIsExistException("已包含名称为：“" + serviceProvider.getName() + "”的数据，不能重复添加。");
            }
            serviceProvider.setCreateTime(new Timestamp(System.currentTimeMillis()));
            serviceProvider.setDelStatus(Constants.DelStatus.NORMAL.isValue());
            serviceProvider.setServiceStatus(Constants.ServiceStatus.OUT_OF_SERVICE.isValue());
        } else {
            Optional<RealServiceProvider> realServiceProvider = realServiceProviderRepository.findById(id);
            //判断数据库中是否有该对象
            if (!realServiceProvider.isPresent()) {
                throw new ObjectNotFoundException("修改失败，原因：" + RealServiceProvider.class + "中不存在id为 " + id + " 的对象");
            }
            if (optional.isPresent() && optional.get().getName() != realServiceProvider.get().getName()) {
                throw new DataIsExistException("已包含名称为：“" + serviceProvider.getName() + "”的数据，不能修改。");
            }
            RealServiceProvider source = realServiceProvider.get();
            //将目标对象（project）中的null值，用源对象中的值替换(因会员价可为空)
            UpdateTool.copyNullProperties(source, serviceProvider);
        }
        //执行保存or修改
        return realServiceProviderRepository.saveAndFlush(serviceProvider);
    }
}
