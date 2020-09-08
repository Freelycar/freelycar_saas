package com.freelycar.saas.project.service;

import com.freelycar.saas.BootApplication;
import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.DataIsExistException;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.project.entity.Project;
import com.freelycar.saas.project.entity.ProjectType;
import com.freelycar.saas.project.repository.ProjectRepository;
import com.freelycar.saas.project.repository.ProjectTypeRepository;
import com.freelycar.saas.project.repository.StoreRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.freelycar.saas.basic.wrapper.ResultCode.RESULT_DATA_NONE;


/**
 * @Author: pyt
 * @Date: 2020/8/10 14:14
 * @Description:
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = BootApplication.class)
//@EnableAutoConfiguration
public class ProjectServiceTest {
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private ProjectTypeService projectTypeService;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private ProjectTypeRepository projectTypeRepository;

    @Test
    public void modify() {
        /**
         * 2.为所有门店添加项目
         */
        String[] projectName = {"基础保养（更换机油机滤垫片）-汉兰达", "基础保养（更换机油机滤垫片）-八凯", "基础保养（更换机油机滤垫片）-小车型"};
        Float[] prices = {1040f, 650f, 470f};
        /*Set<String> storeIds = storeRepository.findIdByDelStatus(false);
        for (String storeId : storeIds) {
            Optional<ProjectType> optional = projectTypeRepository.findByStoreIdAndDelStatusAndName(storeId, false, "4S店");
            if (optional.isPresent()) {
                ProjectType type = optional.get();
                for (int i = 0; i < projectName.length; i++) {
                    Project project = new Project();
                    project.setComment("广汽丰田");
                    project.setProjectTypeId(type.getId());
                    project.setStoreId(storeId);
                    project.setName(projectName[i]);
                    project.setPrice(prices[i]);
                    try {
                        projectService.modify(project);
                    } catch (DataIsExistException e) {
                        e.printStackTrace();
                    } catch (ObjectNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }*/
    }

   /* @Test
    public void upperArk() {
        *//**
         * 5.项目开通智能柜功能
         *//*
        List<Project> projectList = projectRepository.findByDelStatusAndComment(false, "广汽丰田");
        for (Project project : projectList) {
            try {
                int result = projectRepository.uppArkById(project.getId());
                if (result != 1) {
                    System.out.println("上架失败，数据未找到");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("上架失败，上架操作出现异常");
            }
        }
    }*/

    /*@Test
    public void addProjectType() {
        *//**
     * 1.为所有门店添加4S店项目类别
     *//*
        Set<String> storeIds = storeRepository.findIdByDelStatus(false);
        for (String storeId : storeIds) {
            System.out.println(storeId);
            ProjectType projectType = new ProjectType();
            projectType.setStoreId(storeId);
            projectType.setName("4S店");
            ResultJsonObject result = projectTypeService.modify(projectType);
            System.out.println(result);
        }
    }*/


   /* @Test
    public void updateProjectSort() {
        Set<String> storeIds = projectRepository.findDistinctStoreIdByDelStatus();
        for (String storeId :
                storeIds) {
            List<Project> projects = projectRepository.findByStoreIdAndDelStatusOrderByCreateTimeDesc(storeId, Constants.DelStatus.NORMAL.isValue());
            long count = 10l;
            for (Project project :
                    projects) {
                project.setSort(count);
                count += 10l;
            }
            projectRepository.saveAll(projects);
        }
    }*/

}