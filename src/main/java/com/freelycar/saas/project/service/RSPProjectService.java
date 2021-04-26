package com.freelycar.saas.project.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.basic.wrapper.PageableTools;
import com.freelycar.saas.basic.wrapper.PaginationRJO;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.exception.BatchDeleteException;
import com.freelycar.saas.exception.DataIsExistException;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.project.entity.*;
import com.freelycar.saas.project.model.CustomerOrderListObject;
import com.freelycar.saas.project.model.RealServiceProviderModel;
import com.freelycar.saas.project.model.RspProjectModel;
import com.freelycar.saas.project.repository.*;
import com.freelycar.saas.util.MySQLPageTool;
import com.freelycar.saas.util.UpdateTool;
import com.freelycar.saas.wechat.model.BaseOrderInfo;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;

import static com.freelycar.saas.basic.wrapper.ResultCode.RESULT_DATA_NONE;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Ting
 * Date: 2020-09-10
 * Time: 15:49
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class RSPProjectService {
    private StoreRepository storeRepository;

    private RSPProjectRepository rspProjectRepository;

    private RealServiceProviderRepository realServiceProviderRepository;

    private RSPStoreSortRepository rspStoreSortRepository;

    private RspStaffStoreRepository rspStaffStoreRepository;

    private StaffService staffService;

    private RspProjectStoreRepository rspProjectStoreRepository;

    private RealServiceProviderService realServiceProviderService;

    @Autowired
    public void setStoreRepository(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Autowired
    public void setRealServiceProviderService(RealServiceProviderService realServiceProviderService) {
        this.realServiceProviderService = realServiceProviderService;
    }

    @Autowired
    public void setRspProjectStoreRepository(RspProjectStoreRepository rspProjectStoreRepository) {
        this.rspProjectStoreRepository = rspProjectStoreRepository;
    }

    @Autowired
    public void setRspStaffStoreRepository(RspStaffStoreRepository rspStaffStoreRepository) {
        this.rspStaffStoreRepository = rspStaffStoreRepository;
    }

    @Autowired
    public void setRspStoreSortRepository(RSPStoreSortRepository rspStoreSortRepository) {
        this.rspStoreSortRepository = rspStoreSortRepository;
    }

    @Autowired
    public void setStaffService(StaffService staffService) {
        this.staffService = staffService;
    }

    @Autowired
    public void setRealServiceProviderRepository(RealServiceProviderRepository realServiceProviderRepository) {
        this.realServiceProviderRepository = realServiceProviderRepository;
    }

    @Autowired
    public void setRspProjectRepository(RSPProjectRepository rspProjectRepository) {
        this.rspProjectRepository = rspProjectRepository;
    }

    @Autowired
    private LocalContainerEntityManagerFactoryBean entityManagerFactory;

    /**
     * 排序
     *
     * @return
     */
    private synchronized BigDecimal generateSort(String rspId) {
        RSPProject rspProject = rspProjectRepository.findTopByRspIdAndDelStatusAndSortIsNotNullOrderBySortDesc(rspId, Constants.DelStatus.NORMAL.isValue());
        if (null == rspProject) {
            return new BigDecimal("10");
        }
        return rspProject.getSort().add(new BigDecimal("10"));
    }

    /**
     * 项目列表位置交换
     *
     * @param map
     * @return
     */
    public boolean switchLocation(Map<String, BigDecimal> map) {
        Set<String> ids = map.keySet();
        List<RSPProject> rspProjectList = rspProjectRepository.findByIdInAndDelStatus(ids, Constants.DelStatus.NORMAL.isValue());
        if (rspProjectList.size() != ids.size()) {
            return false;
        } else {
            for (RSPProject project :
                    rspProjectList) {
                project.setSort(map.get(project.getId()));
                rspProjectRepository.saveAndFlush(project);
            }
            return true;
        }
    }

    /**
     * 服务商项目新增/修改
     * modify: 服务商开通网点功能情况下，新增项目即上架网点
     */
    public RSPProject add(RSPProject project) throws DataIsExistException, ArgumentMissingException, ObjectNotFoundException {
        if (project != null && !StringUtils.isEmpty(project.getId())) {
            String projectId = project.getId();
            //修改
            Optional<RSPProject> rspProjectOptional = rspProjectRepository.findByIdAndDelStatus(projectId, Constants.DelStatus.NORMAL.isValue());
            if (rspProjectOptional.isPresent()) {
                RSPProject source = rspProjectOptional.get();
                UpdateTool.copyNullProperties(source, project);
                rspProjectRepository.saveAndFlush(project);
                return project;
            } else throw new ObjectNotFoundException("操作失败，未找到id为：" + projectId + " 的RSPProject对象");
        } else if (project != null
                && !StringUtils.isEmpty(project.getName())
                && !StringUtils.isEmpty(project.getRspId())) {
            //新增
            String name = project.getName();
            List<RSPProject> rspProjectList = rspProjectRepository.findByNameAndRspIdAndDelStatus(name, project.getRspId(), Constants.DelStatus.NORMAL.isValue());
            if (rspProjectList.size() > 0) {
                throw new DataIsExistException("已包含名称为：“" + name + "”的数据，不能重复添加。");
            } else {
                //1.新增项目
                project.setCreateTime(new Timestamp(System.currentTimeMillis()));
                project.setDelStatus(Constants.DelStatus.NORMAL.isValue());
                project.setSort(generateSort(project.getRspId()));
                project = rspProjectRepository.save(project);
                //2.上架
                //2.1 查找服务商关联网点
                Set<String> storeIdSet = rspStoreSortRepository.findStoreIdByRspId(project.getRspId());
                List<Store> storeList = storeRepository.findByDelStatusAndIdIn(Constants.DelStatus.NORMAL.isValue(), new ArrayList<>(storeIdSet));
                List<String> projectIds = new ArrayList<>();
                projectIds.add(project.getId());
                //2.2 上架项目
                for (Store store :
                        storeList) {
                    storeBookOnlineProject(store.getId(), projectIds);
                }
                return project;
            }
        } else {
            throw new ArgumentMissingException("参数不完整");
        }
    }

    /**
     * 删除项目（单个、批量）
     */
    @Transactional(rollbackFor = BatchDeleteException.class)
    public ResultJsonObject delete(String[] ids) throws BatchDeleteException {
        Set<String> idSet = new HashSet<>(Arrays.asList(ids));
        int result = rspProjectRepository.delById(idSet);
        if (result == 0) {
            return ResultJsonObject.getErrorResult(ids, "删除失败," + RESULT_DATA_NONE);
        }
        if (result != ids.length) {
            throw new BatchDeleteException("部分id不存在");
        }
        return ResultJsonObject.getDefaultResult(ids, "删除成功");
    }

    /**
     * 服务商下项目列表
     */
    public Page<RSPProject> list(String name, Integer currentPage, Integer pageSize, String rspId) {
        return rspProjectRepository.findByDelStatusAndNameContainingAndRspIdOrderBySortAsc(Constants.DelStatus.NORMAL.isValue(), name, rspId, PageableTools.basicPage(currentPage, pageSize));
    }

    /**
     * 服务商下全部项目id
     */
    public List<String> list(String rspId) {
        return rspProjectRepository.findIdByDelStatusAndRspId(Constants.DelStatus.NORMAL.isValue(), rspId);
    }

    public List<RSPProject> listByRspId(String rspId) {
        return rspProjectRepository.findIdByRspId(rspId);
    }

    /**
     * 网点下项目列表
     */
    public Page<RspProjectModel> listByStore(String name, String rspName, Integer currentPage, Integer pageSize, String storeId) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT \n" +
                "p.id,p.name,p.price,p.`comment`,p.createTime,rsp.name AS rspName\n" +
                "FROM rspproject p LEFT JOIN realserviceprovider rsp ON p.rspId = rsp.id\n" +
                "WHERE rspId IN (SELECT id from realserviceprovider WHERE id in (SELECT DISTINCT(rspId) FROM `rspstoreSort` WHERE storeId='")
                .append(storeId)
                .append("')) ");
        //项目名模糊查询
        if (StringUtils.hasText(name)) {
            sql.append(" AND p.name like '%").append(name).append("%' ");
        }
        if (StringUtils.hasText(rspName)) {
            sql.append(" AND rsp.name like '%").append(rspName).append("%' ");
        }
        sql.append("AND p.delStatus = FALSE \n" +
                "AND rsp.delStatus = FALSE\n" +
                "ORDER BY id DESC");
        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());
        nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(RspProjectModel.class));
        Pageable pageable = PageableTools.basicPage(currentPage, pageSize);
        int total = nativeQuery.getResultList().size();
        @SuppressWarnings({"unused", "unchecked"})
        List<RspProjectModel> rspProjectList = nativeQuery.setFirstResult(MySQLPageTool.getStartPosition(currentPage, pageSize)).setMaxResults(pageSize).getResultList();
        Set<String> projectIds = rspProjectStoreRepository.findRspProjectIdByStoreId(storeId);
        //关闭em
        em.close();
        //项目在网点的上架情况
        if (projectIds.size() > 0) {
            for (RspProjectModel model :
                    rspProjectList) {
                String projectId = model.getId();
                if (projectIds.contains(projectId)) {
                    model.setBookOnline(true);
                }
            }
        }
        @SuppressWarnings("unchecked")
        Page<RspProjectModel> page = new PageImpl(rspProjectList, pageable, total);
        return page;
    }

    /**
     * 获取门店下全部已上架项目
     *
     * @param storeId
     * @return
     * @throws ArgumentMissingException
     */
    private List<RSPProject> getRspProjects(String storeId) throws ArgumentMissingException {
        if (StringUtils.isEmpty(storeId)) {
            throw new ArgumentMissingException("参数storeId值为空");
        }
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM rspproject p WHERE \n" +
                "p.rspId IN (SELECT DISTINCT(rspId) FROM rspstoresort WHERE storeId = '")
                .append(storeId).append("') \n" +
                "AND p.id in (SELECT DISTINCT(rspProjectId) FROM rspprojectstore WHERE storeId = '")
                .append(storeId).append("')\n" +
                "AND delStatus = FALSE");
        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());
        nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(RSPProject.class));
        @SuppressWarnings({"unused", "unchecked"})
        List<RSPProject> rspProjectList = nativeQuery.getResultList();
        //关闭em
        em.close();
        return rspProjectList;
    }

    /**
     * 微信端-项目列表展示：
     * 服务商项目列表
     *
     * @param storeId
     * @param preferential
     * @return 数据格式：
     * rspId，name
     * rspprojectId，name，price，comment
     * @throws ArgumentMissingException
     */
    private final static String PREFERENTIAL_KEYWORD = "***新用户专享***";

    public List<JSONObject> getRspProjects(String storeId, boolean preferential) throws ArgumentMissingException {
        List<JSONObject> res = new ArrayList<>();
        List<RealServiceProviderModel> rspList = realServiceProviderService.listByStore(storeId);
        List<RSPProject> rspProjectList = getRspProjects(storeId);
        //按服务商返回数据
        //服务商id数据
        Set<String> rspIdSet = new HashSet<>();
        for (RSPProject project :
                rspProjectList) {
            String rspId = project.getRspId();
            if (rspIdSet.contains(rspId)) {
                continue;
            } else {
                rspIdSet.add(rspId);
            }
        }
        for (RealServiceProviderModel model :
                rspList) {
            String rspId = model.getId();
            JSONObject rspData = new JSONObject();
            rspData.put("rspId", rspId);
            rspData.put("name", model.getName());
            rspData.put("phone", model.getPhone());
            rspData.put("address", model.getAddress());
            //项目的可接单状态判断
            boolean staffReady = staffReadyHandler(rspId, storeId);
            rspData.put("staffReady", staffReady);
            JSONArray actProjects = new JSONArray();
            JSONArray array = new JSONArray();
            //将活动的项目置顶
            for (RSPProject project :
                    rspProjectList) {
                if (!rspId.equals(project.getRspId())) {
                    continue;
                } else {
                    String comment = project.getComment();
                    JSONObject projectData = new JSONObject();
                    projectData.put("id", project.getId());
                    projectData.put("name", project.getName());
                    projectData.put("price", project.getPrice());
                    projectData.put("comment", project.getComment());
                    if (PREFERENTIAL_KEYWORD.equals(comment)) {
                        actProjects.add(projectData);
                    } else {
                        array.add(projectData);
                    }
                }
            }
            actProjects.addAll(array);
            if (preferential) {
                rspData.put("projects", actProjects);
            } else {
                rspData.put("projects", array);
            }
            if (actProjects.size() > 0 || array.size() > 0) {
                res.add(rspData);
            }
        }
        return res;
    }

    /**
     * 服务商下是否有可服务于该门店的技师
     * 服务商下：未删除、开通智能柜功能、选择服务该网点、接单中的技师
     *
     * @param rspId
     * @param storeId
     * @return
     */
    private boolean staffReadyHandler(String rspId, String storeId) {
        List<Staff> staffList = staffService.getAllArkStaffInRsp(rspId);
        if (staffList == null || staffList.isEmpty() || staffList.size() < 1) {
            return false;
        } else {
            boolean flag = false;
            for (Staff staff :
                    staffList) {
                Set<String> storeIds = rspStaffStoreRepository.findByStaffId(staff.getId());
                if (!storeIds.contains(storeId)) {
                    continue;
                } else {
                    String phone = staff.getPhone();
                    Employee employee = staffService.getEmployeeByPhone(phone);
                    boolean notification = (null == employee.getNotification()) ? false : employee.getNotification();
                    if (notification) {
                        flag = true;
                        break;
                    }
                }
            }
            return flag;
        }
    }


    /**
     * 在门店下上架服务商项目
     *
     * @param storeId
     * @param projectIds
     */
    public void storeBookOnlineProject(String storeId, List<String> projectIds) {
        for (String projectId :
                projectIds) {
            List<RspProjectStore> list = rspProjectStoreRepository.findByStoreIdAndRspProjectId(storeId, projectId);
            if (list == null || list.size() == 0) {
                RspProjectStore rspProjectStore = new RspProjectStore();
                rspProjectStore.setRspProjectId(projectId);
                rspProjectStore.setStoreId(storeId);
                rspProjectStoreRepository.save(rspProjectStore);
            }
        }
    }

    /**
     * 在门店下下架服务商项目
     *
     * @param storeId
     * @param projectIds
     */
    public void storeBookOfflineProject(String storeId, List<String> projectIds) {
        rspProjectStoreRepository.deleteByStoreIdAndRspProjectIdIn(storeId, projectIds);
    }
}
