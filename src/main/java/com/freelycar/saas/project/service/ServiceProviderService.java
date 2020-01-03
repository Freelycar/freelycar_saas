package com.freelycar.saas.project.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.basic.wrapper.PageableTools;
import com.freelycar.saas.basic.wrapper.ResultCode;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.exception.DataIsExistException;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.project.entity.Project;
import com.freelycar.saas.project.entity.ServiceProvider;
import com.freelycar.saas.project.repository.ServiceProviderRepository;
import com.freelycar.saas.util.AddressUtil;
import com.freelycar.saas.util.UpdateTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;

import static com.freelycar.saas.basic.wrapper.ResultCode.RESULT_DATA_NONE;

/**
 * @author puyuting
 * @date 2019/12/23
 * @email 2630451673@qq.com
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ServiceProviderService {
    private Logger logger = LoggerFactory.getLogger(ServiceProviderService.class);

    @Autowired
    private ServiceProviderRepository serviceProviderRepository;
    @Autowired
    private AddressUtil addressUtil;

    public ServiceProvider findById(String serviceProviderId){
        return serviceProviderRepository.findById(serviceProviderId).orElse(null);
    }

    /**
     * 新增/修改服务商
     *
     * @param serviceProvider
     * @return
     */
    public ServiceProvider modify(ServiceProvider serviceProvider) throws DataIsExistException, ObjectNotFoundException {
        //验重
        if (checkName(serviceProvider)) {
            throw new DataIsExistException("已包含名称为：“" + serviceProvider.getName() + "”的数据，不能重复添加。");
        }
        //是否有ID，判断时新增还是修改
        String id = serviceProvider.getId();
        if (StringUtils.isEmpty(id)) {
            serviceProvider.setCreateTime(new Timestamp(System.currentTimeMillis()));
            serviceProvider.setDelStatus(Constants.DelStatus.NORMAL.isValue());
        } else {
            Optional<ServiceProvider> optional = serviceProviderRepository.findById(id);
            //判断数据库中是否有该对象
            if (!optional.isPresent()) {
                throw new ObjectNotFoundException("修改失败，原因：" + Project.class + "中不存在id为 " + id + " 的对象");
            }
            ServiceProvider source = optional.get();
            //将目标对象（project）中的null值，用源对象中的值替换(因会员价可为空)
            UpdateTool.copyNullProperties(source, serviceProvider);
        }
        //执行保存or修改
        return serviceProviderRepository.saveAndFlush(serviceProvider);
    }

    /**
     * 删除服务商项目（软删除）
     *
     * @param id
     * @return
     */
    @Transactional
    public ResultJsonObject delete(String id) {
        try {
            int result = serviceProviderRepository.delById(id);
            if (result != 1) {
                return ResultJsonObject.getErrorResult(id, "删除失败," + RESULT_DATA_NONE);
            }
        } catch (Exception e) {
            return ResultJsonObject.getErrorResult(id, "删除失败，删除操作出现异常");
        }
        return ResultJsonObject.getDefaultResult(id, "删除成功");
    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    public ResultJsonObject delByIds(String ids) {
        if (StringUtils.isEmpty(ids)) {
            return ResultJsonObject.getErrorResult(null, "删除失败：ids" + ResultCode.PARAM_NOT_COMPLETE.message());
        }
        String[] idsList = ids.split(",");
        for (String id : idsList) {
            serviceProviderRepository.delById(id);
        }
        return ResultJsonObject.getDefaultResult(null);
    }

    /**
     * 分页查询（包含“门店名称”的模糊查询）
     *
     * @param name
     * @param currentPage
     * @param pageSize
     * @return
     */
    public Page<ServiceProvider> list(String name, Integer currentPage, Integer pageSize) {
        return serviceProviderRepository.findByDelStatusAndNameContainingOrderByIdAsc(Constants.DelStatus.NORMAL.isValue(), name, PageableTools.basicPage(currentPage, pageSize));
    }

    /**
     * 查询服务商详情
     *
     * @param id
     * @return
     * @throws ArgumentMissingException
     * @throws ObjectNotFoundException
     */
    public ServiceProvider detail(String id) throws ArgumentMissingException, ObjectNotFoundException {
        if (StringUtils.isEmpty(id)) {
            throw new ArgumentMissingException("参数id为空，无法查询门店信息");
        }
        return serviceProviderRepository.findById(id).orElseThrow(ObjectNotFoundException::new);
    }

    public JSONObject getLatAndLonAndDetail(String address){
        Map<String, Double> map =  addressUtil.getLngAndLat(address);
        double lng = map.get("lng");
        double lat = map.get("lat");
        JSONObject result = new JSONObject();
        result.put("location",map);
        String location = addressUtil.getAddress(lng,lat);
        result.put("formatted_address",location);
        return result;
    }

    /**
     * 检查服务商名称
     * true:存在；false:不存在
     *
     * @param serviceProvider
     * @return
     */
    private boolean checkName(ServiceProvider serviceProvider) {
        return serviceProviderRepository.findByNameAndDelStatus(serviceProvider.getName(), false).size() > 0;
    }
}
