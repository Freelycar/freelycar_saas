package com.freelycar.saas.project.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.basic.wrapper.*;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.exception.DataIsExistException;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.project.entity.*;
import com.freelycar.saas.project.repository.*;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
public class ProjectService {
    private final static String PREFERENTIAL_KEYWORD = "***新用户专享***";
    private Logger logger = LoggerFactory.getLogger(ProjectService.class);
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectTypeRepository projectTypeRepository;

    @Autowired
    private LocalContainerEntityManagerFactoryBean entityManagerFactory;

    @Autowired
    private StaffService staffService;

    @Autowired
    private RSPProjectRepository rspProjectRepository;

    @Autowired
    private RealServiceProviderRepository realServiceProviderRepository;

    /**
     * 新增/修改项目对象
     *
     * @param project
     * @return
     */
    public Project modify(Project project) throws DataIsExistException, ObjectNotFoundException {
        //验重
        if (this.checkRepeatName(project)) {
            throw new DataIsExistException("已包含名称为：“" + project.getName() + "”的数据，不能重复添加。");
        }

        //是否有ID，判断时新增还是修改
        String id = project.getId();
        if (StringUtils.isEmpty(id)) {
            project.setDelStatus(Constants.DelStatus.NORMAL.isValue());
            project.setCreateTime(new Timestamp(System.currentTimeMillis()));
            project.setSort(generateSort(project.getStoreId()));
        } else {
            Optional<Project> optional = projectRepository.findById(id);
            //判断数据库中是否有该对象
            if (!optional.isPresent()) {
                throw new ObjectNotFoundException("修改失败，原因：" + Project.class + "中不存在id为 " + id + " 的对象");
            }
            Project source = optional.get();
            //将目标对象（project）中的null值，用源对象中的值替换(因会员价可为空)
//            UpdateTool.copyNullProperties(source, project);
            project.setDelStatus(source.getDelStatus());
            project.setCreateTime(source.getCreateTime());
            project.setPricePerUnit(source.getPricePerUnit());
            project.setReferWorkTime(source.getReferWorkTime());
            project.setUseTimes(source.getUseTimes());
            project.setSaleStatus(source.getSaleStatus());
            project.setStoreId(source.getStoreId());
            project.setBookOnline(source.getBookOnline());
            project.setSort(source.getSort());
        }
        //执行保存or修改
        return projectRepository.saveAndFlush(project);
    }

    /**
     * 自动生成排序
     *
     * @return
     */
    private synchronized BigInteger generateSort(String storeId) {
        Project project = projectRepository.findTopByStoreIdAndDelStatusAndSortIsNotNullOrderBySortDesc(storeId, Constants.DelStatus.NORMAL.isValue());
        if (null == project) {
            return new BigInteger("10");
        }
        return project.getSort().add(new BigInteger("10"));
    }

    public boolean switchLocation(Map<String, BigInteger> map) {
        Set<String> projectIds = map.keySet();
        /*for (String projectId : projectIds) {
            System.out.println(projectId + ":" + map.get(projectId));

        }*/
        List<Project> projects = projectRepository.findByDelStatusAndIdIn(Constants.DelStatus.NORMAL.isValue(), projectIds);
        if (projectIds.size() != projectIds.size()) {
            return false;
        } else {
            for (Project project : projects) {
                project.setSort(map.get(project.getId()));
                projectRepository.saveAndFlush(project);
            }
            return true;
        }
    }

    /**
     * 验证项目是否重复
     * true：重复；false：不重复
     *
     * @param project
     * @return
     */
    private boolean checkRepeatName(Project project) {
        List<Project> projectList;
        if (null != project.getId()) {
            projectList = projectRepository.checkRepeatName(project.getId(), project.getName(), project.getStoreId());
        } else {
            logger.info(project.getName());
            projectList = projectRepository.checkRepeatName(project.getName(), project.getStoreId());
        }
        return projectList.size() != 0;
    }

    /**
     * 为项目添加项目类型
     *
     * @param project
     * @return
     */
    private Project addProjectType(Project project) {
        if (project != null && project.getProjectTypeId() != null) {
            Optional<ProjectType> typeOptional = projectTypeRepository.findById(project.getProjectTypeId());
            typeOptional.ifPresent(projectType -> project.setProjectTypeName(projectType.getName()));
        }
        return project;
    }

    /**
     * 获取项目详情
     *
     * @param id
     * @return
     */
    public ResultJsonObject getDetail(String id) {
        Project project = projectRepository.findById(id).orElse(null);
        project = this.addProjectType(project);
        return ResultJsonObject.getDefaultResult(project);
    }

    /**
     * 查询项目列表
     *
     * @param storeId
     * @param currentPage
     * @param pageSize
     * @return
     */
    public PaginationRJO list(String storeId, Integer currentPage, Integer pageSize, String name, String projectTypeId) {
        Pageable pageable = PageableTools.basicPage(currentPage, pageSize);
        Page<Project> projectPage;
        if (StringUtils.isEmpty(projectTypeId)) {
            projectPage = projectRepository.findAllByDelStatusAndStoreIdAndNameContainingOrderBySortAsc(Constants.DelStatus.NORMAL.isValue(), storeId, name, pageable);
        } else {
            projectPage = projectRepository.findAllByDelStatusAndStoreIdAndNameContainingAndProjectTypeIdOrderBySortAsc(Constants.DelStatus.NORMAL.isValue(), storeId, name, projectTypeId, pageable);
        }
        return addProjectTypeForPage(projectPage);
    }

    private PaginationRJO addProjectTypeForPage(Page<Project> projectPage) {
        PaginationRJO rjo = PaginationRJO.of(projectPage);
        List<Project> content = projectPage.getContent();
        List<Project> result = new ArrayList<>();
        for (Project project :
                content) {
            result.add(addProjectType(project));
        }
        rjo.setData(result);
        return rjo;
    }

    /**
     * 删除操作（软删除）
     *
     * @param id
     * @return
     */
    @Transactional
    public ResultJsonObject delete(String id) {
        try {
            int result = projectRepository.delById(id);
            if (result != 1) {
                return ResultJsonObject.getErrorResult(id, "删除失败," + RESULT_DATA_NONE);
            }
        } catch (Exception e) {
            return ResultJsonObject.getErrorResult(id, "删除失败，删除操作出现异常");
        }
        return ResultJsonObject.getDefaultResult(id, "删除成功");
    }


    /**
     * 服务上架智能柜
     *
     * @param id
     * @return
     */
    @Transactional
    public ResultJsonObject upperArk(String id) {
        try {
            int result = projectRepository.uppArkById(id);
            if (result != 1) {
                return ResultJsonObject.getErrorResult(id, "上架失败," + RESULT_DATA_NONE);
            }
        } catch (Exception e) {
            return ResultJsonObject.getErrorResult(id, "上架失败，上架操作出现异常");
        }
        return ResultJsonObject.getDefaultResult(id, "上架成功");
    }

    /**
     * 服务下架智能柜
     *
     * @param id
     * @return
     */
    @Transactional
    public ResultJsonObject lowerArk(String id) {
        try {
            int result = projectRepository.lowArkById(id);
            if (result != 1) {
                return ResultJsonObject.getErrorResult(id, "下架失败," + RESULT_DATA_NONE);
            }
        } catch (Exception e) {
            return ResultJsonObject.getErrorResult(id, "下架失败，下架操作出现异常");
        }
        return ResultJsonObject.getDefaultResult(id, "下架成功");
    }

    /**
     * 上架（在微信端显示）
     *
     * @param id
     * @return
     */
    @Transactional
    public ResultJsonObject upperShelf(String id) {
        try {
            int result = projectRepository.uppById(id);
            if (result != 1) {
                return ResultJsonObject.getErrorResult(id, "上架失败," + RESULT_DATA_NONE);
            }
        } catch (Exception e) {
            return ResultJsonObject.getErrorResult(id, "上架失败，上架操作出现异常");
        }
        return ResultJsonObject.getDefaultResult(id, "上架成功");
    }

    /**
     * 下架（不在微信端显示）
     *
     * @param id
     * @return
     */
    @Transactional
    public ResultJsonObject lowerShelf(String id) {
        try {
            int result = projectRepository.lowById(id);
            if (result != 1) {
                return ResultJsonObject.getErrorResult(id, "下架失败," + RESULT_DATA_NONE);
            }
        } catch (Exception e) {
            return ResultJsonObject.getErrorResult(id, "下架失败，下架操作出现异常");
        }
        return ResultJsonObject.getDefaultResult(id, "下架成功");
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
            projectRepository.delById(id);
        }
        return ResultJsonObject.getDefaultResult(null);
    }

    private List<Project> getProjects(String storeId) throws ArgumentMissingException {
        if (StringUtils.isEmpty(storeId)) {
            throw new ArgumentMissingException("参数storeId值为空");
        }
        return projectRepository.findAllByStoreIdAndDelStatusAndSaleStatusOrderBySortAsc(storeId, Constants.DelStatus.NORMAL.isValue(), true);
    }


    /**
     * 门店下 全部 未删除 已上架 项目 按照创建时间排序
     * 对新用户显示：新用户专享项目
     * 对非新用户排除：新用户专享项目
     *
     * @param storeId
     * @param preferential
     * @return
     * @throws ArgumentMissingException
     */
    public List<Project> getProjects(String storeId, boolean preferential) throws ArgumentMissingException {

        List<Project> projects = getProjects(storeId);
        List<Project> actProjects = new ArrayList<>();
        List<Project> res = new ArrayList<>();

        projects = staffReadyHandler(projects, storeId);

        //将活动的项目置顶
        if (preferential) {
            for (Project project : projects) {
                String comment = project.getComment();
                if (PREFERENTIAL_KEYWORD.equals(comment)) {
                    actProjects.add(project);
                } else {
                    res.add(addProjectType(project));
                }
            }
            actProjects.addAll(res);
            return actProjects;
        }

        //将活动项目剔除
        for (Project project : projects) {
            String comment = project.getComment();
            if (!PREFERENTIAL_KEYWORD.equals(comment)) {
                res.add(addProjectType(project));
            }
        }

        return res;
    }


    private List<Project> staffReadyHandler(List<Project> projects, String storeId) throws ArgumentMissingException {
        if (null == projects) {
            throw new ArgumentMissingException();
        }

        List<List<Project>> allStaffProjects = new ArrayList<>();

        // 查找门店所有在服务的技师
        logger.info("查找门店所有在服务的技师");
        List<Staff> staffs = staffService.getAllArkStaffInStore(storeId);
        if (null != staffs && !staffs.isEmpty()) {
            for (Staff staff : staffs) {
                String phone = staff.getPhone();
                Employee employee = staffService.getEmployeeByPhone(phone);
                boolean notification = (null == employee.getNotification()) ? false : employee.getNotification();
                if (notification) {
                    // 获取staff对应的Project集合，并且对比当前项目列表中是否有包含
                    List<Project> staffProjects = staff.getProjects();
                    allStaffProjects.add(staffProjects);
                }
            }
            logger.info("staffProjects.size():" + allStaffProjects.size());
        }

        return servicingProjectsFilter(projects, allStaffProjects);
    }

    private List<Project> servicingProjectsFilter(List<Project> storeProjects, List<List<Project>> allStaffProjects) {
        List<Project> servicingProjects = new ArrayList<>();
        for (Project project : storeProjects) {
            for (List<Project> staffProjects : allStaffProjects) {
                if (staffProjects.contains(project)) {
                    //如果有任意一个技师的服务列表中有该服务项目，则认为其在服务列表中有效
                    project.setStaffReady(true);
                    break;
                }
            }
            servicingProjects.add(project);
        }
        return servicingProjects;
    }

    /**
     * 查询门店想展示给车主的服务项目
     *
     * @param storeId
     * @return
     */
    public List<Project> getShowProjects(String storeId) {
        StringBuilder sql = new StringBuilder();
        sql.append(" select p.*,pt.`name` as projectTypeName from project p LEFT JOIN projectType pt on p.projectTypeId=pt.id where p.bookOnline=1 and p.delStatus=0 ")
                .append("  and p.storeId= '").append(storeId).append("' ORDER BY p.createTime asc ");

        EntityManager em = entityManagerFactory.getNativeEntityManagerFactory().createEntityManager();
        Query nativeQuery = em.createNativeQuery(sql.toString());
        nativeQuery.unwrap(NativeQuery.class).setResultTransformer(Transformers.aliasToBean(Project.class));
        @SuppressWarnings({"unused", "unchecked"})
        List<Project> projects = nativeQuery.getResultList();

        //关闭em
        em.close();

        return projects;
    }


}
