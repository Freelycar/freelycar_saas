package com.freelycar.saas.project.service;

import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.basic.wrapper.PageableTools;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.BatchDeleteException;
import com.freelycar.saas.exception.DataIsExistException;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.project.entity.RSPStoreSort;
import com.freelycar.saas.project.entity.RealServiceProvider;
import com.freelycar.saas.project.model.RealServiceProviderModel;
import com.freelycar.saas.project.repository.RSPStoreSortRepository;
import com.freelycar.saas.project.repository.RealServiceProviderRepository;
import com.freelycar.saas.util.MySQLPageTool;
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
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;

import static com.freelycar.saas.basic.wrapper.ResultCode.RESULT_DATA_NONE;

@Service
@Transactional(rollbackFor = Exception.class)
public class RealServiceProviderService {
    private Logger logger = LoggerFactory.getLogger(RealServiceProviderService.class);

    private RealServiceProviderRepository realServiceProviderRepository;

    private RSPStoreSortRepository rspStoreSortRepository;

    @Autowired
    public void setRspStoreSortRepository(RSPStoreSortRepository rspStoreSortRepository) {
        this.rspStoreSortRepository = rspStoreSortRepository;
    }

    @Autowired
    private LocalContainerEntityManagerFactoryBean entityManagerFactory;

    @Autowired
    public void setRealServiceProviderRepository(RealServiceProviderRepository realServiceProviderRepository) {
        this.realServiceProviderRepository = realServiceProviderRepository;
    }

    /**
     * 排序
     *
     * @return
     */
    private synchronized BigInteger generateSort() {
        RealServiceProvider realServiceProvider = realServiceProviderRepository.findTopByDelStatusAndSortIsNotNullOrderBySortDesc(Constants.DelStatus.NORMAL.isValue());
        if (null == realServiceProvider) {
            return new BigInteger("10");
        }
        return realServiceProvider.getSort().add(new BigInteger("10"));
    }

    /**
     * 修改网点下服务商位置
     *
     * @param map
     * @return
     */
    public boolean switchLocation(String storeId, Map<String, BigInteger> map) {
        Set<String> ids = map.keySet();//服务商id
        List<RealServiceProvider> realServiceProviderList = realServiceProviderRepository.findByIdInAndDelStatus(ids, Constants.DelStatus.NORMAL.isValue());
        if (realServiceProviderList.size() != ids.size()) {
            return false;
        } else {
            for (String rspId :
                    ids) {
                List<RSPStoreSort> sortList = rspStoreSortRepository.findByStoreIdAndRspId(storeId, rspId);
                for (RSPStoreSort sort :
                        sortList) {
                    sort.setSort(map.get(rspId));
                    rspStoreSortRepository.saveAndFlush(sort);
                }
            }
            return true;
        }
    }

    /**
     * 新增或修改
     *
     * @param serviceProvider
     */
    public RealServiceProvider modify(RealServiceProvider serviceProvider) throws DataIsExistException, ObjectNotFoundException {
        String id = serviceProvider.getId();
        Optional<RealServiceProvider> optional = realServiceProviderRepository.findByNameAndDelStatus(serviceProvider.getName(), Constants.DelStatus.NORMAL.isValue());
        //1.判断新增/修改
        if (StringUtils.isEmpty(id)) {//新增
            if (optional.isPresent()) {
                throw new DataIsExistException("已包含名称为：“" + serviceProvider.getName() + "”的数据，不能重复添加。");
            }
            serviceProvider.setCreateTime(new Timestamp(System.currentTimeMillis()));
            serviceProvider.setDelStatus(Constants.DelStatus.NORMAL.isValue());
            serviceProvider.setServiceStatus(Constants.ServiceStatus.IN_SERVICE.isValue());
            serviceProvider.setSort(generateSort());
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

    /**
     * 删除服务商项目（软删除）
     *
     * @param ids
     * @return
     */
    @Transactional(rollbackFor = BatchDeleteException.class)
    public ResultJsonObject delete(String[] ids) throws BatchDeleteException {
        Set<String> idSet = new HashSet<>(Arrays.asList(ids));
        int result = realServiceProviderRepository.delById(idSet);
        if (result == 0) {
            return ResultJsonObject.getErrorResult(ids, "删除失败," + RESULT_DATA_NONE);
        }
        if (result != ids.length) {
            throw new BatchDeleteException("部分id不存在");
        }
        return ResultJsonObject.getDefaultResult(ids, "删除成功");
    }

    /**
     * 分页查询（包含“门店名称”的模糊查询）
     *
     * @param name
     * @param currentPage
     * @param pageSize
     * @return
     */
    public Page<RealServiceProvider> list(String name, Integer currentPage, Integer pageSize) {
        return realServiceProviderRepository.findByDelStatusAndNameContainingOrderBySortAsc(Constants.DelStatus.NORMAL.isValue(), name, PageableTools.basicPage(currentPage, pageSize));
    }

    /**
     * 切换服务商状态
     *
     * @param id
     * @return
     */
    public ResultJsonObject changeServiceStatus(String id) {
        Optional<RealServiceProvider> optional = realServiceProviderRepository.findByIdAndDelStatus(id, Constants.DelStatus.NORMAL.isValue());
        if (optional.isPresent()) {
            RealServiceProvider provider = optional.get();
            boolean oldStatus = provider.getServiceStatus();
            provider.setServiceStatus(!oldStatus);
            realServiceProviderRepository.saveAndFlush(provider);
            return ResultJsonObject.getDefaultResult(provider);
        } else return ResultJsonObject.getErrorResult(null, "id:" + id + "不存在！");
    }

    /**
     * 获取网点下服务商排序
     *
     * @param storeId
     */
    public Page<RealServiceProviderModel> listByStore(String storeId, Integer currentPage, Integer pageSize) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT \n" +
                "rsp.id,rsp.`name`,rsp.address,(SELECT sort FROM rspstoresort WHERE rspId = rsp.id AND storeId = '")
                .append(storeId).append("') AS sort \n" +
                "FROM realserviceprovider rsp WHERE \n" +
                "delStatus = FALSE AND id IN (SELECT DISTINCT(rspId) FROM rspstoresort WHERE storeId = '").append(storeId).append("')\n" +
                "ORDER BY sort ASC");
        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());
        nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(RealServiceProviderModel.class));
        Pageable pageable = PageableTools.basicPage(currentPage, pageSize);
        int total = nativeQuery.getResultList().size();
        @SuppressWarnings({"unused", "unchecked"})
        List<RealServiceProviderModel> rspProjectList = nativeQuery.setFirstResult(MySQLPageTool.getStartPosition(currentPage, pageSize)).setMaxResults(pageSize).getResultList();
        //关闭em
        em.close();
        @SuppressWarnings("unchecked")
        Page<RealServiceProviderModel> page = new PageImpl(rspProjectList, pageable, total);
        return page;
    }

    public List<RealServiceProviderModel> listByStore(String storeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT \n" +
                "rsp.id,rsp.`name`,rsp.phone,rsp.address,(SELECT sort FROM rspstoresort WHERE rspId = rsp.id AND storeId = '")
                .append(storeId).append("') AS sort \n" +
                "FROM realserviceprovider rsp WHERE rsp.service_status = true and \n" +
                "id IN (SELECT DISTINCT(rspId) FROM rspstoresort WHERE storeId = '").append(storeId).append("')\n" +
                "ORDER BY sort ASC");
        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());
        nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(RealServiceProviderModel.class));
        @SuppressWarnings({"unused", "unchecked"})
        List<RealServiceProviderModel> rspProjectList = nativeQuery.getResultList();
        //关闭em
        em.close();
        return rspProjectList;
    }
}
