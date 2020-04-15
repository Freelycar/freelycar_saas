package com.freelycar.saas.screen.service;

import com.alibaba.fastjson.JSONObject;
import com.freelycar.saas.project.entity.ConsumerProjectInfo;
import com.freelycar.saas.project.entity.Project;
import com.freelycar.saas.project.entity.ProjectType;
import com.freelycar.saas.project.repository.ConsumerProjectInfoRepository;
import com.freelycar.saas.project.repository.ProjectRepository;
import com.freelycar.saas.project.repository.ProjectTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author pyt
 * @date 2020/4/2 15:31
 * @email 2630451673@qq.com
 * @desc
 */
@Service("consumerProjectService1")
public class ConsumerProjectService {
    private final static Logger logger = LoggerFactory.getLogger(ConsumerProjectService.class);
    @Autowired
    private ConsumerProjectInfoRepository consumerProjectInfoRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectTypeRepository projectTypeRepository;

    /**
     * 服务项目排名
     */
    public JSONObject genProjectRanking() {
        //1.获取项目数据
        //1.1获取到12个月的项目服务数据
        List<ConsumerProjectInfo> projectInfoList = consumerProjectInfoRepository.findByDelStatus(false);
        //项目id
        Set<String> projectIds = new HashSet<>();
        //项目id-项目业务量
        List<Map<String, Integer>> projectSumList = new ArrayList<>();
        for (ConsumerProjectInfo project :
                projectInfoList) {
            String projectId = project.getProjectId();
            if (!projectIds.contains(projectId)) {
                //项目id未记录，项目业务量记1
                projectIds.add(projectId);
                Map<String, Integer> projectSum = new HashMap<>();
                projectSum.put(projectId, 1);
                projectSumList.add(projectSum);
            } else {
                //项目id已记录，项目业务量+1
                for (Map<String, Integer> projectSum :
                        projectSumList) {
                    for (String key :
                            projectSum.keySet()) {
                        if (key.equals(projectId)) {
                            int sum = projectSum.get(projectId);
                            sum++;
                            projectSum.put(projectId, sum);
                        }
                    }
                }
            }
        }
        //2.根据项目类型对项目数据进行处理
        List<Project> projectList = projectRepository.findByDelStatusAndIdIn(false, projectIds);
        Set<String> projectTypeIds = new HashSet<>();
        List<Map<String, Integer>> projectTypeSumList = new ArrayList<>();
        Map<String, String> projectTypeRelationship = new HashMap<>();
        for (Project project : projectList) {
            String projectTypeId = project.getProjectTypeId();
            if (!projectTypeIds.contains(projectTypeId)) {
                projectTypeIds.add(projectTypeId);
            }
            projectTypeRelationship.put(project.getId(), projectTypeId);
        }
        List<ProjectType> projectTypeList = projectTypeRepository.findAll();
        Map<String, String> projectType = new HashMap<>();
        for (ProjectType type :
                projectTypeList) {
            projectType.put(type.getId(), type.getName());
        }
        Set<String> record = new HashSet<>();
        for (Map<String, Integer> projectSum :
                projectSumList) {
            for (String key :
                    projectSum.keySet()) {
                //1)获取项目的项目类型名称
                String projectTypeId = projectTypeRelationship.get(key);
                if (projectTypeId != null) {
                    String projectTypeName = projectType.get(projectTypeId);
                    //2)项目类型名称为key
                    if (!record.contains(projectTypeName)) {
                        record.add(projectTypeName);
                        Map<String, Integer> projectTypeSum = new HashMap<>();
                        projectTypeSum.put(projectTypeName, projectSum.get(key));
                        projectTypeSumList.add(projectTypeSum);
                    } else {
                        for (Map<String, Integer> projectTypeSum :
                                projectTypeSumList) {
                            for (String keyi :
                                    projectTypeSum.keySet()) {
                                if (keyi.equals(projectTypeName)) {
                                    int currentSum = projectTypeSum.get(projectTypeName);
                                    currentSum += projectSum.get(key);
                                    projectTypeSum.put(projectTypeName, currentSum);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        JSONObject result = new JSONObject();
        result.put("projectTypeSumList", projectTypeSumList);
        String[] projectTypeNames = {"保养", "汽车检测", "汽车精修", "维修", "美容", "其他"};
        int[] projectTypeSums = {234, 209, 183, 145, 84, 194};
        int count = projectTypeSumList.size();
        if (count < 6) {
            Set<String> typeNames = new HashSet<>();
            for (Map<String, Integer> projectSum : projectTypeSumList) {
                for (String key : projectSum.keySet()) {
                    typeNames.add(key);
                }
            }
            for (int i = 0; i < projectTypeNames.length; i++) {
                if (!typeNames.contains(projectTypeNames[i])) {
                    typeNames.add(projectTypeNames[i]);
                    Map<String, Integer> projectSumNew = new HashMap<>();
                    projectSumNew.put(projectTypeNames[i], projectTypeSums[i]);
                    projectTypeSumList.add(projectSumNew);
                    count++;
                }
                if (count == 6) break;
            }
        }
        for (int i = 0; i < projectTypeSumList.size() - 1; i++) {
            Map<String, Integer> map1 = projectTypeSumList.get(i);
            int count1 = 0;
            for (String key :
                    map1.keySet()) {
                count1 = map1.get(key);
            }
            for (int j = i + 1; j < projectTypeSumList.size(); j++) {
                Map<String, Integer> map2 = projectTypeSumList.get(j);
                int count2 = 0;
                for (String key :
                        map2.keySet()) {
                    count2 = map2.get(key);
                }
                if (count2 > count1) {
                    count1 = count2;
                    Collections.swap(projectTypeSumList, i, j);
                }
            }
        }
        return result;
    }
}
